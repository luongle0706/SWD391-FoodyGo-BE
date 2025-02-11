package com.foodygo.controller;

import com.foodygo.dto.AddonSectionDTO;
import com.foodygo.dto.response.ObjectResponse;
import com.foodygo.service.AddonSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/addon-sections")
@RequiredArgsConstructor
@Tag(name = "Addon Section")
public class AddonSectionController {

    private final AddonSectionService addonSectionService;

    @Value("${application.default-page-size}")
    private int defaultPageSize;

    @GetMapping("/search-by-product/{productId}")
    @Operation(summary = "Get Addon Sections by Product ID", description = "Retrieve a paginated list of addon sections by the specified product ID. Supports sorting and pagination.")
    public ResponseEntity<ObjectResponse> getAddonSectionsByProductId(
            @PathVariable Integer productId,
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
                                .message("Get all addon sections by product Id")
                                .data(addonSectionService.getAddonSectionsByProductId(productId, pageable))
                                .build()
                );
    }

    @GetMapping("/{addonSectionId}")
    @Operation(summary = "Get Addon Section by ID", description = "Retrieve details of an addon section by its unique ID.")
    public ResponseEntity<ObjectResponse> getAddonSectionById(@PathVariable Integer addonSectionId) {
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(OK.toString())
                                .message("Get addon section by id")
                                .data(addonSectionService.getAddonSectionDTOById(addonSectionId))
                                .build()
                );
    }

    @PostMapping
    @Operation(summary = "Create Addon Section", description = "Create a new addon section using the provided data.")
    public ResponseEntity<ObjectResponse> createAddonSection(
            @RequestBody AddonSectionDTO.CreateRequest request
    ) {
        return ResponseEntity
                .status(CREATED)
                .body(
                        ObjectResponse.builder()
                                .status(CREATED.toString())
                                .message("Create addon section")
                                .data(addonSectionService.createAddonSectionDTO(request))
                                .build()
                );
    }

    @PutMapping
    @Operation(summary = "Update Addon Section", description = "Update an existing addon section with the provided data.")
    public ResponseEntity<ObjectResponse> updateAddonSection(
            @RequestBody AddonSectionDTO.UpdateRequest request
    ) {
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(OK.toString())
                                .message("Updated addon section")
                                .data(addonSectionService.updateAddonSectionDTO(request))
                                .build()
                );
    }

    @DeleteMapping("/{addonSectionId}")
    @Operation(summary = "Delete Addon Section", description = "Delete an addon section by its unique ID.")
    public ResponseEntity<ObjectResponse> deleteAddonSection(
            @PathVariable Integer addonSectionId
    ) {
        addonSectionService.deleteAddonSection(addonSectionId);
        return ResponseEntity
                .status(OK)
                .body(
                        ObjectResponse.builder()
                                .status(OK.toString())
                                .message("Deleted addon section")
                                .build()
                );
    }
}
