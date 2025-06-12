package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.MenuCategoryDTO;
import com.railswad.deliveryservice.dto.MenuItemDTO;
import com.railswad.deliveryservice.entity.MenuCategory;
import com.railswad.deliveryservice.entity.MenuItem;
import com.railswad.deliveryservice.entity.Vendor;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.MenuCategoryRepository;
import com.railswad.deliveryservice.repository.MenuItemRepository;
import com.railswad.deliveryservice.repository.VendorRepository;
import com.railswad.deliveryservice.service.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

    @Autowired
    private MenuService menuService;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @PostMapping("/categories")
//    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<MenuCategoryDTO> createMenuCategory(@RequestBody MenuCategoryDTO categoryDTO) {
        logger.info("Received request to create menu category for vendor ID: {}", categoryDTO.getVendorId());
        return ResponseEntity.ok(menuService.createMenuCategory(categoryDTO));
    }

    @PutMapping("/categories/{categoryId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<MenuCategoryDTO> updateMenuCategory(@PathVariable Long categoryId, @RequestBody MenuCategoryDTO categoryDTO) {
        logger.info("Received request to update menu category ID: {}", categoryId);
        return ResponseEntity.ok(menuService.updateMenuCategory(categoryId, categoryDTO));
    }

    @GetMapping("/categories/{categoryId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<MenuCategoryDTO> getMenuCategoryById(@PathVariable Long categoryId, @RequestBody MenuCategoryDTO categoryDTO) {
        logger.info("Received request to update menu category ID: {}", categoryId);
        return ResponseEntity.ok(menuService.getMenuCategoriesById(categoryId));
    }

    @DeleteMapping("/categories/{categoryId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<Void> deleteMenuCategory(@PathVariable Long categoryId) {
        logger.info("Received request to delete menu category ID: {}", categoryId);
        menuService.deleteMenuCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
//    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<MenuItemDTO> createMenuItem(@RequestBody MenuItemDTO itemDTO) {
        logger.info("Received request to create menu item for category ID: {}", itemDTO.getCategoryId());
        return ResponseEntity.ok(menuService.createMenuItem(itemDTO));
    }

    @PutMapping("/items/{itemId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<MenuItemDTO> updateMenuItem(@PathVariable Long itemId, @RequestBody MenuItemDTO itemDTO) {
        logger.info("Received request to update menu item ID: {}", itemId);
        return ResponseEntity.ok(menuService.updateMenuItem(itemId, itemDTO));
    }

    @DeleteMapping("/items/{itemId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long itemId) {
        logger.info("Received request to delete menu item ID: {}", itemId);
        menuService.deleteMenuItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<MenuItemDTO> getMenuItemById(@PathVariable Long itemId) {
        logger.info("Received request to fetch menu item ID: {}", itemId);
        return ResponseEntity.ok(menuService.getMenuItemById(itemId));
    }

    @GetMapping("/vendors/{vendorId}/items")
    public ResponseEntity<List<MenuItemDTO>> getAvailableMenuItemsByVendor(@PathVariable Long vendorId) {
        logger.info("Received request to fetch available menu items for vendor ID: {}", vendorId);
        return ResponseEntity.ok(menuService.getAvailableMenuItemsByVendor(vendorId));
    }

    @GetMapping("/vendors/{vendorId}/categories")
    public ResponseEntity<Page<MenuCategoryDTO>> getMenuCategoriesByVendor(@PathVariable Long vendorId, Pageable pageable) {
        logger.info("Received request to fetch menu categories for vendor ID: {}", vendorId);
        return ResponseEntity.ok(menuService.getMenuCategoriesByVendor(vendorId, pageable));
    }

    @PostMapping("/upload")
//    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<String> uploadExcelFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("vendorId") Long vendorId,
            @RequestParam(value = "clearExisting", defaultValue = "false") boolean clearExisting) {
        logger.info("Received request to upload menu for vendor ID: {}, clearExisting: {}", vendorId, clearExisting);
        try {
            String message = menuService.uploadMenuItems(file, vendorId, clearExisting);
            logger.info("Menu upload successful for vendor ID: {}", vendorId);
            return ResponseEntity.status(HttpStatus.OK).body(message);
        } catch (Exception e) {
            logger.error("Menu upload failed for vendor ID: {}: {}", vendorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not upload the file: " + e.getMessage());
        }
    }

    @GetMapping("/{vendorId}/menu")
    public ResponseEntity<?> getMenuByVendor(@PathVariable Long vendorId) {
        logger.info("Received request to fetch full menu for vendor ID: {}", vendorId);
        try {
            Map<String, List<MenuItemDTO>> menu = menuService.getMenuByVendor(vendorId);
            return ResponseEntity.ok(menu);
        } catch (ResourceNotFoundException e) {
            logger.error("Vendor ID {} not found", vendorId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to fetch menu for vendor ID: {}: {}", vendorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch menu: " + e.getMessage());
        }
    }
}