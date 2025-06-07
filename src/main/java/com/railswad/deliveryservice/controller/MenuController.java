package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.MenuCategoryDTO;
import com.railswad.deliveryservice.dto.MenuItemDTO;
import com.railswad.deliveryservice.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<MenuCategoryDTO> createMenuCategory(@RequestBody MenuCategoryDTO categoryDTO) {
        return ResponseEntity.ok(menuService.createMenuCategory(categoryDTO));
    }

    @PutMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<MenuCategoryDTO> updateMenuCategory(@PathVariable Long categoryId, @RequestBody MenuCategoryDTO categoryDTO) {
        return ResponseEntity.ok(menuService.updateMenuCategory(categoryId, categoryDTO));
    }

    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<Void> deleteMenuCategory(@PathVariable Long categoryId) {
        menuService.deleteMenuCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<MenuItemDTO> createMenuItem(@RequestBody MenuItemDTO itemDTO) {
        return ResponseEntity.ok(menuService.createMenuItem(itemDTO));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<MenuItemDTO> updateMenuItem(@PathVariable Long itemId, @RequestBody MenuItemDTO itemDTO) {
        return ResponseEntity.ok(menuService.updateMenuItem(itemId, itemDTO));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long itemId) {
        menuService.deleteMenuItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<MenuItemDTO> getMenuItemById(@PathVariable Long itemId) {
        return ResponseEntity.ok(menuService.getMenuItemById(itemId));
    }

    @GetMapping("/vendors/{vendorId}/items")
    public ResponseEntity<List<MenuItemDTO>> getAvailableMenuItemsByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(menuService.getAvailableMenuItemsByVendor(vendorId));
    }

    @GetMapping("/vendors/{vendorId}/categories")
    public ResponseEntity<Page<MenuCategoryDTO>> getMenuCategoriesByVendor(@PathVariable Long vendorId, Pageable pageable) {
        return ResponseEntity.ok(menuService.getMenuCategoriesByVendor(vendorId, pageable));
    }
}
