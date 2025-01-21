package com.foodygo.service;

import com.foodygo.dto.request.CustomerCreateRequest;
import com.foodygo.dto.request.CustomerUpdateRequest;
import com.foodygo.entity.*;
import com.foodygo.exception.ElementNotFoundException;
import com.foodygo.repository.CustomerRepository;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CustomerServiceImp extends BaseServiceImp<Customer, Integer> implements CustomerService {

    private final CustomerRepository customerRepository;
    private final BuildingService buildingService;
    private final UserService userService;

    // Firebase

    @Value("${firebase.bucket.name}")
    private String bucketName;

    @Value("${firebase.content.type}")
    private String contentType;

    @Value("${firebase.get.stream}")
    private String fileConfigFirebase;

    @Value("${firebase.get.url}")
    private String urlFirebase;

    @Value("${firebase,get.folder}")
    private String folderContainImage;

    @Value("${firebase.file.format}")
    private String fileFormat;

    // BufferImage

    @Value("${buffer-image.type}")
    private String bufferImageType;

    @Value("${buffer-image.fill-rect.width}")
    private int bufferImageWidth;

    @Value("${buffer-image.fill-rect.height}")
    private int bufferImageHeight;

    @Value("${buffer-image.fill-rect.color.background}")
    private String bufferImageColorBackground;

    @Value("${buffer-image.fill-rect.color.text}")
    private String bufferImageColorText;

    @Value("${buffer-image.fill-rect.font.text}")
    private String bufferImageFontText;

    @Value("${buffer-image.fill-rect.size.text}")
    private int bufferImageSizeText;

    @Value("${buffer-image.fill-rect.x}")
    private int bufferImageX;

    @Value("${buffer-image.fill-rect.y}")
    private int bufferImageY;

    @Value("${buffer-image.devide}")
    private int bufferImageDevide;

    public CustomerServiceImp(CustomerRepository customerRepository, BuildingService buildingService, UserService userService) {
        super(customerRepository);
        this.customerRepository = customerRepository;
        this.buildingService = buildingService;
        this.userService = userService;
    }

    @Override
    public List<Customer> getAllCustomersActive() {
        List<Customer> customers = customerRepository.findAll();
        List<Customer> activeCustomers = new ArrayList<Customer>();
        for (Customer customer : customers) {
            if(!customer.isDeleted()) {
                activeCustomers.add(customer);
            }
        }
        return activeCustomers;
    }

    @Override
    public Customer undeleteCustomer(Integer customerID) {
        Customer customer = customerRepository.findCustomerById((customerID));
        if (customer != null) {
            if (customer.isDeleted()) {
                customer.setDeleted(false);
                return customerRepository.save(customer);
            }
        }
        return null;
    }

    @Override
    public User getUserByCustomerID(Integer customerID) {
        Customer customer = customerRepository.findCustomerById(customerID);
        if (customer != null) {
            return customer.getUser();
        }
        return null;
    }

    private Customer getCustomer(Integer customerID) {
        Customer customer = customerRepository.findCustomerById(customerID);
        if (customer == null) {
            throw new ElementNotFoundException("Customer not found");
        }
        return customer;
    }

    private Building getBuilding(int buildingID) {
        Building building = buildingService.findById(buildingID);
        if (building == null) {
            throw new ElementNotFoundException("Building is not found");
        }
        return building;
    }

    private User getUser(int userID) {
        User user = userService.findById(userID);
        if (user == null) {
            throw new ElementNotFoundException("User is not found");
        }
        return user;
    }

    @Override
    public Customer createCustomer(CustomerCreateRequest customerCreateRequest) {
        Building building = getBuilding(customerCreateRequest.getBuildingID());
        User user = getUser(customerCreateRequest.getUserID());
        try {
            String url = null;
            if(customerCreateRequest.getImage() != null) {
                String dataUrl = generateImageWithInitial(user.getEmail());
                url = uploadFileBase64(dataUrl);
            }
            Customer customer = Customer.builder()
                    .image(url)
                    .building(building)
                    .user(user)
                    .build();
            return customerRepository.save(customer);
        } catch (Exception e) {
            log.error("Customer creation failed", e);
            return null;
        }
    }

    @Override
    public Customer updateCustomer(CustomerUpdateRequest customerUpdateRequest, int customerID) {
        Customer customer = getCustomer(customerID);
        try {
            if(customerUpdateRequest.getImage() != null) {
                String oldAvatar = customer.getImage();
                String url = upload(customerUpdateRequest.getImage());
                customer.setImage(url);
                if (oldAvatar != null) {
                    deleteImageOnFireBase(oldAvatar);
                }
            }
            if (customerUpdateRequest.getBuildingID() > 0) {
                Building building = getBuilding(customerUpdateRequest.getBuildingID());
                customer.setBuilding(building);
            }
            if (customerUpdateRequest.getUserID() > 0) {
                User user = getUser(customerUpdateRequest.getUserID());
                customer.setUser(user);
            }
            return customerRepository.save(customer);
        } catch (Exception e) {
            log.error("Customer update failed", e);
            return null;
        }
    }

    @Override
    public List<Order> getOrdersByCustomerID(Integer customerID) {
        // chua có order service
        return List.of();
    }

    @Override
    public Customer getCustomerByOrderID(Integer orderID) {
        // chua có order service
        return null;
    }

    @Override
    public Building getBuildingByCustomerID(int customerID) {
        Customer customer = getCustomer(customerID);
        if (customer != null) {
            return customer.getBuilding();
        }
        return null;
    }

    @Override
    public User getUserByCustomerID(int customerID) {
        Customer customer = getCustomer(customerID);
        if (customer != null) {
            return customer.getUser();
        }
        return null;
    }

    @Override
    public Wallet getWalletByCustomerID(Integer customerID) {
        Customer customer = getCustomer(customerID);
        if (customer != null) {
            return customer.getWallet();
        }
        return null;
    }

    @Override
    public Customer getCustomerByWalletID(Integer walletID) {
        // chưa có wallet service
        return null;
    }

//    @Override
//    public List<Deposit> getDepositByCustomerID(Integer customerID) {
//        Customer customer = getCustomer(customerID);
//        if (customer != null) {
//            return customer.getDeposit();
//        }
//        return null;
//    }
//
//    @Override
//    public Customer getCustomerByDepositID(Integer depositID) {
//        // chưa có wallet service
//        return null;
//    }




    //  Xử lí hình ảnh vs firebase

    private String uploadFile(File file, String fileName) throws IOException {  // file vs fileName is equal
        String folder = folderContainImage + "/" + fileName;  // 1 is folder and fileName is "randomString + "extension""
        BlobId blobId = BlobId.of(bucketName, folder); // blodId is a path to file in firebase
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();  // blodInfo contains blodID and more
        InputStream inputStream = CustomerServiceImp.class.getClassLoader().getResourceAsStream(fileConfigFirebase); // change the file name with your one
        Credentials credentials = GoogleCredentials.fromStream(inputStream);
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        storage.create(blobInfo, Files.readAllBytes(file.toPath()));
        // saved image on firebase
        String DOWNLOAD_URL = urlFirebase;
        return String.format(DOWNLOAD_URL, URLEncoder.encode(folder, StandardCharsets.UTF_8));
    }

    private boolean deleteImageOnFireBase(String urlImage) throws IOException {
        String folder = folderContainImage + "/" + urlImage;
        BlobId blobId = BlobId.of(bucketName, folder); // Replace with your bucker name
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
        InputStream inputStream = CustomerServiceImp.class.getClassLoader().getResourceAsStream(fileConfigFirebase); // change the file name with your one
        Credentials credentials = GoogleCredentials.fromStream(inputStream);
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        return storage.delete(blobId);
    }

    private String uploadFileBase64(String base64Image) throws IOException {

        String fileName = UUID.randomUUID().toString() + fileFormat;  // Generate a random file name
        String folder = folderContainImage + "/" + fileName;
        BlobId blobId = BlobId.of(bucketName, folder); // Replace with your bucket name
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();    // type media does not see the picture on firebase

        InputStream inputStream = CustomerServiceImp.class.getClassLoader().getResourceAsStream(fileConfigFirebase); // change the file name with your one
        Credentials credentials = GoogleCredentials.fromStream(inputStream);
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();

        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        storage.create(blobInfo, imageBytes);

        String DOWNLOAD_URL = urlFirebase;
        return String.format(DOWNLOAD_URL, URLEncoder.encode(folder, StandardCharsets.UTF_8));
    }

    private File convertToFile(MultipartFile multipartFile, String fileName) throws IOException {
        File tempFile = new File(fileName);          // create newFile ưith String of fileName (random String + "extension") and save to Current Working Directory or Java Virtual Machine (JVM)
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
            fos.close();
        }
        return tempFile;
    }

    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private String upload(MultipartFile multipartFile) {
        try {
            String fileName = multipartFile.getOriginalFilename();                        // to get file name.jpg, .png, ...
            fileName = UUID.randomUUID().toString().concat(this.getExtension(fileName));  // to generated random string values for file name and plus + "extension".
            File file1 = this.convertToFile(multipartFile, fileName);                      // to convert multipartFile to File
            String URL = this.uploadFile(file1, fileName);                                   // to get uploaded file link
            file1.delete();
            return URL;
        } catch (Exception e) {
            e.printStackTrace();
            return "Image couldn't upload, Something went wrong";
        }
    }

    private String upload(MultipartFile[] multipartFile) {
        try {

            for (MultipartFile file : multipartFile) {
                String fileName = file.getOriginalFilename();                        // to get original file name
                fileName = UUID.randomUUID().toString().concat(this.getExtension(fileName));  // to generated random string values for file name.

                File file1 = this.convertToFile(file, fileName);                      // to convert multipartFile to File
                String URL = this.uploadFile(file1, fileName);                                   // to get uploaded file link
                file1.delete();
            }
//            String fileName = multipartFile.getOriginalFilename();                        // to get original file name
//            fileName = UUID.randomUUID().toString().concat(this.getExtension(fileName));  // to generated random string values for file name.
//
//            File file = this.convertToFile(multipartFile, fileName);                      // to convert multipartFile to File
//            String URL = this.uploadFile(file, fileName);                                   // to get uploaded file link
//            file.delete();
//            return URL;
        } catch (Exception e) {
            e.printStackTrace();
            return "Image couldn't upload, Something went wrong";
        }
        return null;
    }

    private String generateImageWithInitial(String userName) throws IOException {

        char initial = Character.toUpperCase(userName.trim().charAt(0));

        // create width and height of image
        int width = bufferImageWidth;
        int height = bufferImageHeight;

        // BufferedImage to process image in memory, it can be drawing, edit, insert things into image
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // insert character into image
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.decode(bufferImageColorBackground));
        graphics.fillRect(bufferImageX, bufferImageY, width, height);  // x, y is the conner on the top left of rectangle
        graphics.setFont(new Font(bufferImageFontText, Font.BOLD, bufferImageSizeText));
        graphics.setColor(Color.decode(bufferImageColorText));
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int x = (width - fontMetrics.charWidth(initial)) / bufferImageDevide;
        int y = ((height - fontMetrics.getHeight()) / bufferImageDevide) + fontMetrics.getAscent();
        graphics.drawString(String.valueOf(initial), x, y);
        graphics.dispose();

        // change image to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, bufferImageType, baos);

        byte[] imageBytes = baos.toByteArray();

        // Encode the byte array to a Base64 string
        String base64String = Base64.getEncoder().encodeToString(imageBytes);

        return base64String;
    }

}
