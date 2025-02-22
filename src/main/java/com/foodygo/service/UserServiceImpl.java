package com.foodygo.service;

import com.foodygo.configuration.CustomUserDetail;
import com.foodygo.configuration.JWTAuthenticationFilter;
import com.foodygo.configuration.JWTToken;
import com.foodygo.dto.CustomerDTO;
import com.foodygo.dto.UserDTO;
import com.foodygo.dto.request.UserCreateRequest;
import com.foodygo.dto.request.UserRegisterRequest;
import com.foodygo.dto.request.UserUpdateRequest;
import com.foodygo.dto.request.UserUpdateRoleRequest;
import com.foodygo.dto.response.PagingResponse;
import com.foodygo.dto.response.TokenResponse;
import com.foodygo.entity.*;
import com.foodygo.enums.EnumRoleNameType;
import com.foodygo.enums.EnumTokenType;
import com.foodygo.exception.AuthenticationException;
import com.foodygo.exception.ElementExistException;
import com.foodygo.exception.ElementNotFoundException;
import com.foodygo.exception.UnchangedStateException;
import com.foodygo.mapper.CustomerMapper;
import com.foodygo.mapper.UserMapper;
import com.foodygo.repository.CustomerRepository;
import com.foodygo.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
public class UserServiceImpl extends BaseServiceImpl<User, Integer> implements UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final CustomerRepository customerRepository;
    private final JWTToken jwtToken;
    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationManager authenticationManager;

    public UserServiceImpl(UserRepository userRepository, RoleService roleService, BCryptPasswordEncoder bCryptPasswordEncoder,
                           UserMapper userMapper, CustomerMapper customerMapper, CustomerRepository customerRepository,
                           JWTToken jwtToken, JWTAuthenticationFilter jwtAuthenticationFilter, AuthenticationManager authenticationManager) {
        super(userRepository);
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userMapper = userMapper;
        this.customerMapper = customerMapper;
        this.customerRepository = customerRepository;
        this.jwtToken = jwtToken;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public PagingResponse findAllUsers(Integer currentPage, Integer pageSize) {
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize);

        var pageData = userRepository.findAll(pageable);

        return !pageData.getContent().isEmpty() ? PagingResponse.builder()
                .code("Success")
                .message("Get all users paging successfully")
                .currentPage(currentPage)
                .pageSizes(pageSize)
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .data(pageData.getContent().stream()
                        .map(userMapper::userToUserDTO)
                        .toList())
                .build() :
                PagingResponse.builder()
                        .code("Failed")
                        .message("Get all users paging failed")
                        .currentPage(currentPage)
                        .pageSizes(pageSize)
                        .totalElements(pageData.getTotalElements())
                        .totalPages(pageData.getTotalPages())
                        .data(pageData.getContent().stream()
                                .map(userMapper::userToUserDTO)
                                .toList())
                        .build();
    }

    @Override
    public PagingResponse getAllUsersActive(Integer currentPage, Integer pageSize) {

        Pageable pageable = PageRequest.of(currentPage - 1, pageSize);

        var pageData = userRepository.findAllByDeletedFalse(pageable);

        return !pageData.getContent().isEmpty() ? PagingResponse.builder()
                .code("Success")
                .message("Get all users active paging successfully")
                .currentPage(currentPage)
                .pageSizes(pageSize)
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .data(pageData.getContent().stream()
                        .map(userMapper::userToUserDTO)
                        .toList())
                .build() :
                PagingResponse.builder()
                        .code("Failed")
                        .message("Get all users active paging failed")
                        .currentPage(currentPage)
                        .pageSizes(pageSize)
                        .totalElements(pageData.getTotalElements())
                        .totalPages(pageData.getTotalPages())
                        .data(pageData.getContent().stream()
                                .map(userMapper::userToUserDTO)
                                .toList())
                        .build();
    }

    private String getRoleByRoleID(Integer roleID) {
        if (roleID == null) {
            throw new ElementNotFoundException("Role ID is null");
        }
        return switch (roleID) {
            case 1 -> "ROLE_ADMIN";
            case 2 -> "ROLE_STAFF";
            case 3 -> "ROLE_USER";
            case 4 -> "ROLE_MANAGER";
            case 5 -> "ROLE_SELLER";
            default -> throw new ElementNotFoundException("Role ID is not valid");
        };
    }

    @Override
    public List<User> getUsersByRole(Integer roleID) {

        String role = getRoleByRoleID(roleID);

        List<User> listsByRole = userRepository.findAll();
        Role role_admin = roleService.getRoleByRoleName(EnumRoleNameType.ROLE_ADMIN);
        Role role_manager = roleService.getRoleByRoleName(EnumRoleNameType.ROLE_MANAGER);

        if (role.equals(EnumRoleNameType.ROLE_STAFF.name())) {
            for (User user : userRepository.findAll()) {
                if (user.getRole().equals(role_admin) || user.getRole().equals(role_manager)) {
                    listsByRole.remove(user);
                }
            }
        } else if (role.equals(EnumRoleNameType.ROLE_ADMIN.name())) {
            return userRepository.findAll();
        }
        return listsByRole;
    }

    @Override
    public boolean lockedUser(int id) {
        User user = userRepository.getUserByUserID(id);
        if (user != null && user.isNonLocked()) {
            userRepository.locked(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean unLockedUser(int id) {
        User user = userRepository.getUserByUserID(id);
        if (user != null && !user.isNonLocked()) {
            userRepository.unLocked(id);
            return true;
        }
        return false;
    }


    @Override
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            throw new ElementNotFoundException("User with email " + email + " not found");
        }
        return userMapper.userToUserDTO(user);
    }

    @Override
    public boolean getUserByPhone(String phone) {
        User user = userRepository.getUserByPhone(phone);
        return user != null && user.isNonLocked() && user.isEnabled();
    }

    @Override
    public void lockedUserByEmail(String email) {
        User user = userRepository.getUserByEmail(email);
        if (user != null && user.isNonLocked()) {
            userRepository.lockedByEmail(email);
        }
    }

    @Override
    public boolean checkEmailOrPhone(String s) {
        User user = null;
        boolean check = false;
        char c = s.toLowerCase().charAt(0);
        if (c >= 'a' && c <= 'z') {
            user = userRepository.getUserByEmail(s);
            check = true;
        } else if (c >= '0' && c <= '9') {
            user = userRepository.getUserByPhone(s);
        }
        return check;
    }

    @Override
    public UserDTO registerUser(UserRegisterRequest userRegisterRequest) {
        User checkExistingUser = userRepository.getUserByEmail(userRegisterRequest.getEmail());
        if (checkExistingUser != null) {
            throw new ElementExistException("User already exists");
        }
        Role role = roleService.getRoleByRoleName(EnumRoleNameType.ROLE_USER);
        User user = User.builder()
                .email(userRegisterRequest.getEmail())
                .password(bCryptPasswordEncoder.encode(userRegisterRequest.getPassword()))
                .accessToken(null)
                .refreshToken(null)
                .enabled(true)
                .nonLocked(true)
                .role(role)
                .build();
        return userMapper.userToUserDTO(userRepository.save(user));
    }

    @Override
    public UserDTO updateUser(UserUpdateRequest userUpdateRequest, int userID) {

        CustomUserDetail customUserDetail = (CustomUserDetail) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (customUserDetail.getUserID() != userID) {
            throw new AuthenticationException("You are not allowed to update other user");
        }

        User user = userRepository.getUserByUserID(userID);
        if (user != null) {
            if (userUpdateRequest.getPassword() != null) {
                user.setPassword(bCryptPasswordEncoder.encode(userUpdateRequest.getPassword()));
            }
            if (userUpdateRequest.getPhone() != null) {
                if (!user.getPhone().equals(userUpdateRequest.getPhone())) {
                    User checkExistingUser = userRepository.getUserByPhone(userUpdateRequest.getPhone());
                    if (checkExistingUser != null) {
                        throw new ElementExistException("Phone already exists");
                    }
                }
                user.setPhone(userUpdateRequest.getPhone());
            }
            if (userUpdateRequest.getFullName() != null) {
                user.setFullName(userUpdateRequest.getFullName());
            }
            return userMapper.userToUserDTO(userRepository.save(user));
        } else {
            throw new ElementNotFoundException("User not found");
        }
    }

    @Override
    public UserDTO updateUserRole(UserUpdateRoleRequest userUpdateRoleRequest, int userID) {
        User user = userRepository.getUserByUserID(userID);

        if (user == null) {
            throw new ElementNotFoundException("User not found");
        }
        if (userUpdateRoleRequest.getPassword() != null) {
            user.setPassword(bCryptPasswordEncoder.encode(userUpdateRoleRequest.getPassword()));
        }
        if (userUpdateRoleRequest.getPhone() != null) {
            if (!user.getPhone().equals(userUpdateRoleRequest.getPhone())) {
                User checkExistingUser = userRepository.getUserByPhone(userUpdateRoleRequest.getPhone());
                if (checkExistingUser != null) {
                    throw new ElementExistException("Phone already exists");
                }
            }
            user.setPhone(userUpdateRoleRequest.getPhone());
        }
        if (userUpdateRoleRequest.getFullName() != null) {
            user.setFullName(userUpdateRoleRequest.getFullName());
        }
        if (userUpdateRoleRequest.getRoleID() > 0) {
            Role role = roleService.getRoleByRoleId(userUpdateRoleRequest.getRoleID());
            if (role == null) {
                throw new ElementNotFoundException("Role not found");
            }
            user.setRole(role);
        }
        return userMapper.userToUserDTO(userRepository.save(user));
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        TokenResponse tokenResponse = new TokenResponse("Failed", "Refresh token failed", null, null, null, null);
        String email = jwtToken.getEmailFromJwt(refreshToken, EnumTokenType.REFRESH_TOKEN);
        User user = userRepository.getUserByEmail(email);
        if (user != null) {
            if (StringUtils.hasText(refreshToken) && user.getRefreshToken().equals(refreshToken)) {
                if (jwtToken.validate(refreshToken, EnumTokenType.REFRESH_TOKEN)) {
                    CustomUserDetail customUserDetail = CustomUserDetail.mapUserToUserDetail(user);
                    if (customUserDetail != null) {
                        String newToken = jwtToken.generatedToken(customUserDetail);
                        user.setAccessToken(newToken);
                        userRepository.save(user);
                        tokenResponse = new TokenResponse("Success", "Refresh token successfully", newToken, refreshToken, user.getFullName(), user.getEmail());
                    }
                }
            }
        }
        return tokenResponse;
    }

    @Override
    public TokenResponse login(String email, String password) {
        TokenResponse tokenResponse = new TokenResponse("Failed", "Login failed", null, null, null, null);

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtToken.generatedToken(userDetails);
        String refreshToken = jwtToken.generatedRefreshToken(userDetails);
        User user = userRepository.getUserByEmail(userDetails.getEmail());
        if (user != null) {
            user.setRefreshToken(refreshToken);
            user.setAccessToken(token);
            userRepository.save(user);
            tokenResponse = TokenResponse.builder()
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .token(token)
                    .refreshToken(refreshToken)
                    .code("Success")
                    .message("Login successfully")
                    .build();
        }
        return tokenResponse;
    }

    @Override
    public boolean logout(HttpServletRequest request, HttpServletResponse response) {
        String token = jwtAuthenticationFilter.getToken(request);
        String email = jwtToken.getEmailFromJwt(token, EnumTokenType.TOKEN);
        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            throw new ElementNotFoundException("User not found");
        }
        user.setAccessToken(null);
        user.setRefreshToken(null);
        User checkUser = userRepository.save(user);
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return checkUser.getAccessToken() == null;
    }

    @Override
    public TokenResponse getTokenLoginFromOauth2() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            return new TokenResponse("Failed", "Login Failed", null, null, null, null);
        }

        OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
        String email = oauth2User.getAttribute("email");
        User user = userRepository.getUserByEmail(email);

        if (user == null) {
            Role role = roleService.getRoleByRoleName(EnumRoleNameType.ROLE_USER);
            user = User.builder()
                    .email(email)
                    .enabled(true)
                    .nonLocked(true)
                    .role(role)
                    .build();
            userRepository.save(user);
        }

        CustomUserDetail userDetail = CustomUserDetail.mapUserToUserDetail(user);
        String token = jwtToken.generatedToken(userDetail);
        String refreshToken = jwtToken.generatedRefreshToken(userDetail);

        user.setAccessToken(token);
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return new TokenResponse("Success", "Login successfully", token, refreshToken, user.getFullName(), user.getEmail());
    }

    @Override
    public UserDTO createUserWithRole(UserCreateRequest userCreateRequest) {
        User checkExistingUser = userRepository.getUserByEmail(userCreateRequest.getEmail());
        if (checkExistingUser != null) {
            throw new ElementExistException("User already exists");
        }
        Role role = roleService.getRoleByRoleId(userCreateRequest.getRoleID());
        User user = User.builder()
                .email(userCreateRequest.getEmail())
                .password(bCryptPasswordEncoder.encode(userCreateRequest.getPassword()))
                .accessToken(null)
                .refreshToken(null)
                .enabled(true)
                .nonLocked(true)
                .role(role)
                .build();
        return userMapper.userToUserDTO(userRepository.save(user));
    }

    @Override
    public UserDTO undeletedUser(int userID) {
        User user = userRepository.getUserByUserID(userID);
        if (user == null) {
            throw new ElementNotFoundException("User not found");
        }
        if (user.isNonLocked() && user.isEnabled() && !user.isDeleted()) {
            throw new UnchangedStateException("User already deleted");
        }
        user.setNonLocked(true);
        user.setDeleted(false);
        user.setEnabled(true);
        return userMapper.userToUserDTO(userRepository.save(user));
    }

    @Override
    public CustomerDTO getCustomerByUserID(int userID) {
        User user = userRepository.getUserByUserID(userID);
        if (user != null) {
            return customerMapper.customerToCustomerDTO(user.getCustomer());
        }
        return null;
    }

    @Override
    public List<OrderActivity> getOrderActivitiesByUserID(int userID) {
        // chua co order activity
        return List.of();
    }

    @Override
    public UserDTO getUserByOrderActivityID(int orderActivityID) {
        // chua co order activity
        return null;
    }

    @Override
    public List<Order> getOrdersByEmployeeID(int userID) {
        User user = userRepository.getUserByUserID(userID);
        if (user == null) {
            throw new ElementNotFoundException("User not found");
        }
        return user.getEmployeeOrders();
    }

    @Override
    public UserDTO getEmployeeByOrderID(int orderID) {
        // chua co order service
        return null;
    }

    @Override
    public UserDTO deleteUser(int userID) {
        User user = userRepository.getUserByUserID(userID);
        if (user == null) {
            throw new ElementNotFoundException("User not found");
        }
        user.setDeleted(true);
        user.setEnabled(false);
        user.setNonLocked(false);
        Customer customer = user.getCustomer();
        if (customer != null) {
            customer.setDeleted(true);
            customerRepository.save(customer);
        }
        return userMapper.userToUserDTO(userRepository.save(user));
    }

    @Override
    public int countNumberOfRegisterToday() {
        return userRepository.countNumberOfRegisterToday();
    }

}
