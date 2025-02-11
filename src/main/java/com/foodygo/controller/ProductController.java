package com.foodygo.controller;

import com.foodygo.dto.CategoryDTO;
import com.foodygo.dto.ProductDTO;
import com.foodygo.dto.response.ObjectResponse;
import com.foodygo.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@Tag(name = "Product")
public class ProductController {

    private final ProductService productService;

    @Value("${application.default-page-size}")
    private int defaultPageSize;

    @GetMapping()
    @Operation(summary = "Get all products",
            description = "Retrieves all products, with optional pagination and sorting.")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'SELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ObjectResponse> getAllProducts(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending
    ) {
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize != null ? pageSize : defaultPageSize, sort);
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(OK.toString())
                                .message("Get all products")
                                .data(productService.getAllProductDTOs(pageable))
                                .build()
                );
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product by ID",
            description = "Retrieves a product by its ID.")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'SELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ObjectResponse> getProductById(@PathVariable Integer productId) {
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(OK.toString())
                                .message("Get product with ID " + productId)
                                .data(productService.getProductDTOById(productId))
                                .build()
                );
    }

    @GetMapping("/search-by-restaurant")
    @Operation(summary = "Get products by restaurant ID",
            description = "Retrieves products by restaurant ID, with optional pagination and sorting.")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'SELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ObjectResponse> getProductsByRestaurantId(
            @RequestParam Integer restaurantId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending
    ) {
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize != null ? pageSize : defaultPageSize, sort);
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(OK.toString())
                                .message("Get product with restaurant ID " + restaurantId)
                                .data(productService.getAllProductDTOsByRestaurantId(restaurantId, pageable))
                                .build()
                );
    }

    @GetMapping("/search-by-category")
    @Operation(summary = "Get products by category ID",
            description = "Retrieves products by category ID, with optional pagination and sorting.")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'SELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ObjectResponse> getProductsByCategoryId(
            @RequestParam Integer categoryId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "true") boolean ascending
    ) {
        Sort sort = ascending ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize != null ? pageSize : defaultPageSize, sort);
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(OK.toString())
                                .message("Get product with category ID " + categoryId)
                                .data(productService.getAllProductDTOsByCategoryId(categoryId, pageable))
                                .build()
                );
    }

    @Operation(summary = "Create a new product",
            description = "Creates a new product with the provided details.")
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ObjectResponse> createCategory(
            @RequestBody ProductDTO request
    ) {
        productService.createProduct(request);
        return ResponseEntity
                .status(CREATED)
                .body(
                        ObjectResponse.builder()
                                .status(CREATED.toString())
                                .message("Creat product successfully")
                                .build()
                );
    }

    @Operation(summary = "Update an existing category",
            description = "Updates the details of an existing category.")
    @PutMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ObjectResponse> updateCategory(
            @RequestBody ProductDTO request
    ) {
        productService.updateProductInfo(request);
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(OK.toString())
                                .message("Update product successfully")
                                .build()
                );
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Switch product availability",
            description = "Switch product availability to open/close")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ObjectResponse> switchAvailability(@PathVariable Integer productId) {
        boolean availability = productService.switchProductAvailability(productId);
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(OK.toString())
                                .message("Switch product's availability to " + availability)
                                .build()
                );
    }

    @Operation(summary = "Delete a category",
            description = "Deletes a category by its ID.")
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER')")
    public ResponseEntity<ObjectResponse> deleteCategory(
            @PathVariable Integer productId
    ) {
        productService.deleteProduct(productId);
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(CREATED.toString())
                                .message("Delete product successfully")
                                .build()
                );
    }
}

