package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.MenuCategoryDTO;
import com.railswad.deliveryservice.dto.MenuItemDTO;
import com.railswad.deliveryservice.entity.MenuCategory;
import com.railswad.deliveryservice.entity.MenuItem;
import com.railswad.deliveryservice.entity.User;
import com.railswad.deliveryservice.entity.Vendor;
import com.railswad.deliveryservice.exception.InvalidInputException;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.exception.ServiceException;
import com.railswad.deliveryservice.exception.UnauthorizedException;
import com.railswad.deliveryservice.exception.FileProcessingException;
import com.railswad.deliveryservice.repository.MenuCategoryRepository;
import com.railswad.deliveryservice.repository.MenuItemRepository;
import com.railswad.deliveryservice.repository.UserRepository;
import com.railswad.deliveryservice.repository.VendorRepository;
import com.railswad.deliveryservice.util.ExcelHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuService.class);

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuCategoryRepository menuCategoryRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExcelHelper excelHelper;

    private void checkAuthorization(Long vendorId) {
        logger.debug("Checking authorization for vendor ID: {}", vendorId);
        if (vendorId == null) {
            logger.warn("Invalid authorization check: vendorId is null");
            throw new InvalidInputException("Vendor ID is required");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            logger.warn("No authenticated user found for authorization check");
            throw new UnauthorizedException("User must be authenticated");
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", auth.getName());
                    return new ResourceNotFoundException("User not found: " + auth.getName());
                });

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            Vendor vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> {
                        logger.warn("Vendor not found with id: {}", vendorId);
                        return new ResourceNotFoundException("Vendor not found with id: " + vendorId);
                    });
            if (!vendor.getUser().getUserId().equals(user.getUserId())) {
                logger.warn("User {} not authorized to modify vendor ID: {}", auth.getName(), vendorId);
                throw new UnauthorizedException("User not authorized to modify this vendor's menu");
            }
        }
        logger.debug("Authorization check passed for vendor ID: {}", vendorId);
    }

    @Transactional
    @CacheEvict(value = {"vendorMenu", "availableMenuItems"}, key = "#categoryDTO.vendorId", allEntries = false)
    public MenuCategoryDTO createMenuCategory(MenuCategoryDTO categoryDTO) {
        logger.info("Creating menu category for vendor ID: {}", categoryDTO.getVendorId());
        validateMenuCategoryDTO(categoryDTO);

        checkAuthorization(categoryDTO.getVendorId());

        Vendor vendor = vendorRepository.findById(categoryDTO.getVendorId())
                .orElseThrow(() -> {
                    logger.warn("Vendor not found with id: {}", categoryDTO.getVendorId());
                    return new ResourceNotFoundException("Vendor not found with id: " + categoryDTO.getVendorId());
                });

        MenuCategory category = new MenuCategory();
        category.setVendor(vendor);
        category.setCategoryName(categoryDTO.getCategoryName());
        category.setDisplayOrder(categoryDTO.getDisplayOrder() != null ? categoryDTO.getDisplayOrder() : 0);

        try {
            MenuCategory savedCategory = menuCategoryRepository.save(category);
            categoryDTO.setCategoryId(savedCategory.getCategoryId());
            logger.info("Menu category created with ID: {}", savedCategory.getCategoryId());
            return categoryDTO;
        } catch (Exception e) {
            logger.error("Failed to create menu category for vendor ID: {} due to: {}", categoryDTO.getVendorId(), e.getMessage(), e);
            throw new ServiceException("MENU_CATEGORY_CREATION_FAILED", "Failed to create menu category");
        }
    }

    @Transactional
    @CacheEvict(value = {"vendorMenu", "availableMenuItems"}, key = "#categoryDTO.vendorId", allEntries = false)
    public MenuCategoryDTO updateMenuCategory(Long categoryId, MenuCategoryDTO categoryDTO) {
        logger.info("Updating menu category ID: {}", categoryId);
        if (categoryId == null) {
            logger.warn("Invalid update request: categoryId is null");
            throw new InvalidInputException("Category ID is required");
        }
        validateMenuCategoryDTO(categoryDTO);

        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    logger.warn("Menu category not found with id: {}", categoryId);
                    return new ResourceNotFoundException("Menu category not found with id: " + categoryId);
                });

        checkAuthorization(category.getVendor().getVendorId());

        category.setCategoryName(categoryDTO.getCategoryName());
        category.setDisplayOrder(categoryDTO.getDisplayOrder() != null ? categoryDTO.getDisplayOrder() : 0);

        try {
            MenuCategory updatedCategory = menuCategoryRepository.save(category);
            categoryDTO.setCategoryId(updatedCategory.getCategoryId());
            categoryDTO.setVendorId(updatedCategory.getVendor().getVendorId());
            logger.info("Menu category updated with ID: {}", categoryId);
            return categoryDTO;
        } catch (Exception e) {
            logger.error("Failed to update menu category ID: {} due to: {}", categoryId, e.getMessage(), e);
            throw new ServiceException("MENU_CATEGORY_UPDATE_FAILED", "Failed to update menu category");
        }
    }

    @CacheEvict(value = {"vendorMenu", "availableMenuItems"}, allEntries = true) // Can't resolve vendorId here
    @Transactional
    public void deleteMenuCategory(Long categoryId) {
        logger.info("Deleting menu category ID: {}", categoryId);
        if (categoryId == null) {
            logger.warn("Invalid delete request: categoryId is null");
            throw new InvalidInputException("Category ID is required");
        }

        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    logger.warn("Menu category not found with id: {}", categoryId);
                    return new ResourceNotFoundException("Menu category not found with id: " + categoryId);
                });

        checkAuthorization(category.getVendor().getVendorId());

        try {
            menuCategoryRepository.delete(category);
            logger.info("Menu category deleted with ID: {}", categoryId);
        } catch (Exception e) {
            logger.error("Failed to delete menu category ID: {} due to: {}", categoryId, e.getMessage(), e);
            throw new ServiceException("MENU_CATEGORY_DELETION_FAILED", "Failed to delete menu category");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "menuItems", allEntries = true),
            @CacheEvict(value = "vendorMenu", key = "#itemDTO.vendorId", condition = "#itemDTO.vendorId != null"),
            @CacheEvict(value = "availableMenuItems", key = "#itemDTO.vendorId", condition = "#itemDTO.vendorId != null")
    })
    public MenuItemDTO createMenuItem(MenuItemDTO itemDTO) {
        logger.info("Creating menu item for category ID: {}", itemDTO.getCategoryId());
        validateMenuItemDTO(itemDTO);

        if (itemDTO.getCategoryId() == null) {
            logger.warn("Invalid menu item: categoryId is null");
            throw new InvalidInputException("Category ID is required");
        }

        MenuCategory category = menuCategoryRepository.findById(itemDTO.getCategoryId())
                .orElseThrow(() -> {
                    logger.warn("Menu category not found with id: {}", itemDTO.getCategoryId());
                    return new ResourceNotFoundException("Menu category not found with id: " + itemDTO.getCategoryId());
                });

        checkAuthorization(category.getVendor().getVendorId());

        if (menuItemRepository.findByCategoryAndItemName(category, itemDTO.getItemName()).isPresent()) {
            logger.warn("Duplicate menu item '{}' in category ID: {}", itemDTO.getItemName(), itemDTO.getCategoryId());
            throw new InvalidInputException("Menu item '" + itemDTO.getItemName() + "' already exists in category ID: " + itemDTO.getCategoryId());
        }

        MenuItem item = new MenuItem();
        item.setCategory(category);
        item.setItemName(itemDTO.getItemName());
        item.setDescription(itemDTO.getDescription());
        item.setBasePrice(itemDTO.getBasePrice());
        item.setVendorPrice(itemDTO.getVendorPrice());
        item.setVegetarian(itemDTO.isVegetarian());
        item.setAvailable(itemDTO.isAvailable());
        item.setPreparationTimeMin(itemDTO.getPreparationTimeMin());
        item.setImageUrl(itemDTO.getImageUrl());
        item.setDisplayOrder(itemDTO.getDisplayOrder() != null ? itemDTO.getDisplayOrder() : 0);
        item.setAvailableStartTime(itemDTO.getAvailableStartTime());
        item.setAvailableEndTime(itemDTO.getAvailableEndTime());
        item.setItemCategory(itemDTO.getItemCategory());

        try {
            MenuItem savedItem = menuItemRepository.save(item);
            itemDTO.setItemId(savedItem.getItemId());
            logger.info("Menu item created with ID: {}", savedItem.getItemId());
            return itemDTO;
        } catch (Exception e) {
            logger.error("Failed to create menu item for category ID: {} due to: {}", itemDTO.getCategoryId(), e.getMessage(), e);
            throw new ServiceException("MENU_ITEM_CREATION_FAILED", "Failed to create menu item");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "menuItems", key = "#itemId"),
            @CacheEvict(value = "vendorMenu", key = "#category.getVendor().vendorId", condition = "#category != null"),
            @CacheEvict(value = "availableMenuItems", key = "#category.getVendor().vendorId", condition = "#category != null")
    })
    public MenuItemDTO updateMenuItem(Long itemId, MenuItemDTO itemDTO) {
        logger.info("Updating menu item ID: {}", itemId);
        if (itemId == null) {
            logger.warn("Invalid update request: itemId is null");
            throw new InvalidInputException("Item ID is required");
        }
        validateMenuItemDTO(itemDTO);

        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> {
                    logger.warn("Menu item not found with id: {}", itemId);
                    return new ResourceNotFoundException("Menu item not found with id: " + itemId);
                });

        MenuCategory category = menuCategoryRepository.findById(itemDTO.getCategoryId())
                .orElseThrow(() -> {
                    logger.warn("Menu category not found with id: {}", itemDTO.getCategoryId());
                    return new ResourceNotFoundException("Menu category not found with id: " + itemDTO.getCategoryId());
                });

        checkAuthorization(category.getVendor().getVendorId());

        if (!item.getItemName().equals(itemDTO.getItemName()) &&
                menuItemRepository.findByCategoryAndItemName(category, itemDTO.getItemName()).isPresent()) {
            logger.warn("Duplicate menu item '{}' in category ID: {}", itemDTO.getItemName(), itemDTO.getCategoryId());
            throw new InvalidInputException("Menu item '" + itemDTO.getItemName() + "' already exists in category ID: " + itemDTO.getCategoryId());
        }

        item.setCategory(category);
        item.setItemName(itemDTO.getItemName());
        item.setDescription(itemDTO.getDescription());
        item.setBasePrice(itemDTO.getBasePrice());
        item.setVendorPrice(itemDTO.getVendorPrice());
        item.setVegetarian(itemDTO.isVegetarian());
        item.setAvailable(itemDTO.isAvailable());
        item.setPreparationTimeMin(itemDTO.getPreparationTimeMin());
        item.setImageUrl(itemDTO.getImageUrl());
        item.setDisplayOrder(itemDTO.getDisplayOrder() != null ? itemDTO.getDisplayOrder() : 0);
        item.setAvailableStartTime(itemDTO.getAvailableStartTime());
        item.setAvailableEndTime(itemDTO.getAvailableEndTime());
        item.setItemCategory(itemDTO.getItemCategory());

        try {
            MenuItem updatedItem = menuItemRepository.save(item);
            itemDTO.setItemId(updatedItem.getItemId());
            logger.info("Menu item updated with ID: {}", itemId);
            return itemDTO;
        } catch (Exception e) {
            logger.error("Failed to update menu item ID: {} due to: {}", itemId, e.getMessage(), e);
            throw new ServiceException("MENU_ITEM_UPDATE_FAILED", "Failed to update menu item");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "menuItems", key = "#itemId"),
            @CacheEvict(value = "vendorMenu", key = "#vendor.vendorId", condition = "#vendor != null"),
            @CacheEvict(value = "availableMenuItems", key = "#vendor.vendorId", condition = "#vendor != null")
    })
    public void deleteMenuItem(Long itemId) {
        logger.info("Deleting menu item ID: {}", itemId);
        if (itemId == null) {
            logger.warn("Invalid delete request: itemId is null");
            throw new InvalidInputException("Item ID is required");
        }

        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> {
                    logger.warn("Menu item not found with id: {}", itemId);
                    return new ResourceNotFoundException("Menu item not found with id: " + itemId);
                });

        Vendor vendor = vendorRepository.findById(item.getCategory().getVendor().getVendorId())
                .orElseThrow(() -> {
                    logger.warn("Vendor not found for category ID: {}", item.getCategory().getCategoryId());
                    return new ResourceNotFoundException("Vendor not found for category ID: " + item.getCategory().getCategoryId());
                });

        checkAuthorization(vendor.getVendorId());

        try {
            menuItemRepository.delete(item);
            logger.info("Menu item deleted with ID: {}", itemId);
        } catch (Exception e) {
            logger.error("Failed to delete menu item ID: {} due to: {}", itemId, e.getMessage(), e);
            throw new ServiceException("MENU_ITEM_DELETION_FAILED", "Failed to delete menu item");
        }
    }


    @Cacheable(value = "menuItems", key = "#itemId")
    public MenuItemDTO getMenuItemById(Long itemId) {
        logger.info("Fetching menu item with ID: {}", itemId);
        if (itemId == null) {
            logger.warn("Invalid fetch request: itemId is null");
            throw new InvalidInputException("Item ID is required");
        }

        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> {
                    logger.warn("Menu item not found with id: {}", itemId);
                    return new ResourceNotFoundException("Menu item not found with id: " + itemId);
                });

        MenuItemDTO itemDTO = new MenuItemDTO();
        itemDTO.setItemId(item.getItemId());
        itemDTO.setCategoryId(item.getCategory().getCategoryId());
        itemDTO.setItemName(item.getItemName());
        itemDTO.setDescription(item.getDescription());
        itemDTO.setBasePrice(item.getBasePrice());
        itemDTO.setVendorPrice(item.getVendorPrice());
        itemDTO.setVegetarian(item.isVegetarian());
        itemDTO.setAvailable(item.isAvailable());
        itemDTO.setPreparationTimeMin(item.getPreparationTimeMin());
        itemDTO.setImageUrl(item.getImageUrl());
        itemDTO.setDisplayOrder(item.getDisplayOrder());
        itemDTO.setAvailableStartTime(item.getAvailableStartTime());
        itemDTO.setAvailableEndTime(item.getAvailableEndTime());
        itemDTO.setItemCategory(item.getItemCategory());
        logger.debug("Fetched menu item ID: {}", itemId);
        return itemDTO;
    }

    @Cacheable(value = "availableMenuItems", key = "#vendorId")

    public List<MenuItemDTO> getAvailableMenuItemsByVendor(Long vendorId) {
        logger.info("Fetching available menu items for vendor ID: {}", vendorId);
        if (vendorId == null) {
            logger.warn("Invalid fetch request: vendorId is null");
            throw new InvalidInputException("Vendor ID is required");
        }

        LocalTime currentTime = LocalTime.now();
        List<MenuItem> items;
        try {
            items = menuItemRepository.findAvailableItemsByVendor(vendorId, currentTime);
            logger.debug("Found {} available menu items for vendor ID: {}", items.size(), vendorId);
        } catch (Exception e) {
            logger.error("Failed to fetch available menu items for vendor ID: {} due to: {}", vendorId, e.getMessage(), e);
            throw new ServiceException("MENU_ITEM_FETCH_FAILED", "Failed to fetch available menu items");
        }

        return items.stream().map(item -> {
            MenuItemDTO itemDTO = new MenuItemDTO();
            itemDTO.setItemId(item.getItemId());
            itemDTO.setCategoryId(item.getCategory().getCategoryId());
            itemDTO.setItemName(item.getItemName());
            itemDTO.setDescription(item.getDescription());
            itemDTO.setBasePrice(item.getBasePrice());
            itemDTO.setVendorPrice(item.getVendorPrice());
            itemDTO.setVegetarian(item.isVegetarian());
            itemDTO.setAvailable(item.isAvailable());
            itemDTO.setPreparationTimeMin(item.getPreparationTimeMin());
            itemDTO.setImageUrl(item.getImageUrl());
            itemDTO.setDisplayOrder(item.getDisplayOrder());
            itemDTO.setAvailableStartTime(item.getAvailableStartTime());
            itemDTO.setAvailableEndTime(item.getAvailableEndTime());
            itemDTO.setItemCategory(item.getItemCategory());
            return itemDTO;
        }).collect(Collectors.toList());
    }

    public Page<MenuCategoryDTO> getMenuCategoriesByVendor(Long vendorId, Pageable pageable) {
        logger.info("Fetching menu categories for vendor ID: {}", vendorId);
        if (vendorId == null) {
            logger.warn("Invalid fetch request: vendorId is null");
            throw new InvalidInputException("Vendor ID is required");
        }
        if (pageable == null) {
            logger.warn("Invalid fetch request: pageable is null");
            throw new InvalidInputException("Pageable is required");
        }

        try {
            Page<MenuCategory> categories = menuCategoryRepository.findByVendorVendorId(vendorId, pageable);
            logger.debug("Found {} menu categories for vendor ID: {}", categories.getTotalElements(), vendorId);
            return categories.map(category -> {
                MenuCategoryDTO categoryDTO = new MenuCategoryDTO();
                categoryDTO.setCategoryId(category.getCategoryId());
                categoryDTO.setVendorId(category.getVendor().getVendorId());
                categoryDTO.setCategoryName(category.getCategoryName());
                categoryDTO.setDisplayOrder(category.getDisplayOrder());
                return categoryDTO;
            });
        } catch (Exception e) {
            logger.error("Failed to fetch menu categories for vendor ID: {} due to: {}", vendorId, e.getMessage(), e);
            throw new ServiceException("MENU_CATEGORY_FETCH_FAILED", "Failed to fetch menu categories");
        }
    }

    @Cacheable(value = "vendorMenu", key = "#vendorId")
    public Map<String, List<MenuItemDTO>> getMenuByVendor(Long vendorId) {
        logger.info("Fetching full menu for vendor ID: {}", vendorId);
        if (vendorId == null) {
            logger.warn("Invalid fetch request: vendorId is null");
            throw new InvalidInputException("Vendor ID is required");
        }

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> {
                    logger.error("Vendor ID {} not found", vendorId);
                    return new ResourceNotFoundException("Vendor ID " + vendorId + " not found");
                });

        List<MenuCategory> categories = menuCategoryRepository.findByVendorVendorId(vendorId);
        Map<String, List<MenuItemDTO>> menu = new HashMap<>();

        try {
            for (MenuCategory category : categories) {
                List<MenuItem> items = menuItemRepository.findByCategory(category);
                List<MenuItemDTO> itemDTOs = items.stream().map(item -> {
                    MenuItemDTO dto = new MenuItemDTO();
                    dto.setItemId(item.getItemId());
                    dto.setCategoryId(item.getCategory().getCategoryId());
                    dto.setItemName(item.getItemName());
                    dto.setDescription(item.getDescription());
                    dto.setBasePrice(item.getBasePrice());
                    dto.setVendorPrice(item.getVendorPrice());
                    dto.setVegetarian(item.isVegetarian());
                    dto.setAvailable(item.isAvailable());
                    dto.setPreparationTimeMin(item.getPreparationTimeMin());
                    dto.setImageUrl(item.getImageUrl());
                    dto.setDisplayOrder(item.getDisplayOrder());
                    dto.setAvailableStartTime(item.getAvailableStartTime());
                    dto.setAvailableEndTime(item.getAvailableEndTime());
                    return dto;
                }).collect(Collectors.toList());
                menu.put(category.getCategoryName(), itemDTOs);
            }
            logger.info("Fetched menu with {} categories for vendor ID: {}", menu.size(), vendorId);
            return menu;
        } catch (Exception e) {
            logger.error("Failed to fetch menu for vendor ID: {} due to: {}", vendorId, e.getMessage(), e);
            throw new ServiceException("MENU_FETCH_FAILED", "Failed to fetch menu for vendor");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "vendorMenu", key = "#vendorId"),
            @CacheEvict(value = "availableMenuItems", key = "#vendorId"),
            @CacheEvict(value = "menuItems", allEntries = true)
    })
    public String uploadMenuItems(MultipartFile file, Long vendorId, boolean clearExisting) {
        logger.info("Starting menu upload for vendor ID: {}, clearExisting: {}", vendorId, clearExisting);
        if (file == null || file.isEmpty()) {
            logger.warn("Invalid file upload: File is null or empty");
            throw new InvalidInputException("Excel file is required");
        }
        if (vendorId == null) {
            logger.warn("Invalid file upload: vendorId is null");
            throw new InvalidInputException("Vendor ID is required");
        }

        if (!excelHelper.hasExcelFormat(file)) {
            logger.warn("Invalid file format: {}", file.getContentType());
            throw new InvalidInputException("Please upload an Excel file: .xlsx or .xls extension required");
        }

        checkAuthorization(vendorId);

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> {
                    logger.warn("Vendor not found with id: {}", vendorId);
                    return new ResourceNotFoundException("Vendor not found with id: " + vendorId);
                });

        if (clearExisting) {
            logger.info("Clearing existing menu for vendor ID: {}", vendorId);
            try {
                List<MenuCategory> existingCategories = menuCategoryRepository.findByVendorVendorId(vendorId);
                menuCategoryRepository.deleteAll(existingCategories);
                logger.info("Cleared {} categories for vendor ID: {}", existingCategories.size(), vendorId);
            } catch (Exception e) {
                logger.error("Failed to clear existing menu for vendor ID: {} due to: {}", vendorId, e.getMessage(), e);
                throw new ServiceException("MENU_CLEAR_FAILED", "Failed to clear existing menu");
            }
        }

        List<MenuItemDTO> dtos;
        try {
            dtos = excelHelper.excelToMenuItemDTOs(file.getInputStream());
            logger.info("Parsed {} menu items from Excel file", dtos.size());
        } catch (IOException e) {
            logger.error("Failed to parse Excel file: {}", e.getMessage(), e);
            throw new FileProcessingException("Failed to parse Excel file: " + e.getMessage());
        }

        if (dtos.isEmpty()) {
            logger.warn("No menu items found in Excel file");
            throw new InvalidInputException("Excel file contains no menu items");
        }

        List<MenuItem> menuItems = new ArrayList<>();
        for (MenuItemDTO dto : dtos) {
            validateExcelMenuItemDTO(dto, dtos.indexOf(dto) + 2);

            MenuCategory category = menuCategoryRepository.findByVendorAndCategoryName(vendor, dto.getCategoryName())
                    .orElseGet(() -> {
                        logger.info("Creating new category '{}' for vendor ID: {}", dto.getCategoryName(), vendorId);
                        MenuCategory newCategory = new MenuCategory();
                        newCategory.setVendor(vendor);
                        newCategory.setCategoryName(dto.getCategoryName());
                        newCategory.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
                        return menuCategoryRepository.save(newCategory);
                    });

            if (menuItemRepository.findByCategoryAndItemName(category, dto.getItemName()).isPresent()) {
                logger.warn("Duplicate item '{}' in category '{}' at row {}", dto.getItemName(), dto.getCategoryName(), dtos.indexOf(dto) + 2);
                throw new InvalidInputException("Item '" + dto.getItemName() + "' already exists in category '" + dto.getCategoryName() + "' at row " + (dtos.indexOf(dto) + 2));
            }

            MenuItem item = new MenuItem();
            item.setCategory(category);
            item.setItemName(dto.getItemName());
            item.setDescription(dto.getDescription());
            item.setBasePrice(dto.getBasePrice());
            item.setVendorPrice(dto.getVendorPrice());
            item.setVegetarian(dto.isVegetarian());
            item.setAvailable(dto.isAvailable());
            item.setPreparationTimeMin(dto.getPreparationTimeMin());
            item.setImageUrl(dto.getImageUrl());
            item.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
            item.setAvailableStartTime(dto.getAvailableStartTime());
            item.setAvailableEndTime(dto.getAvailableEndTime());
            item.setItemCategory(dto.getItemCategory());

            menuItems.add(item);
            logger.debug("Prepared menu item: {} in category: {}", dto.getItemName(), dto.getCategoryName());
        }

        try {
            menuItemRepository.saveAll(menuItems);
            logger.info("Successfully saved {} menu items for vendor ID: {}", menuItems.size(), vendorId);
            return "Uploaded the file successfully for vendor ID " + vendorId + ": " + file.getOriginalFilename();
        } catch (Exception e) {
            logger.error("Failed to save menu items for vendor ID: {} due to: {}", vendorId, e.getMessage(), e);
            throw new ServiceException("MENU_ITEM_UPLOAD_FAILED", "Failed to save menu items from Excel file");
        }
    }

    private void validateMenuCategoryDTO(MenuCategoryDTO categoryDTO) {
        if (categoryDTO == null) {
            logger.warn("Invalid menu category: DTO is null");
            throw new InvalidInputException("Menu category data is required");
        }
        if (categoryDTO.getVendorId() == null) {
            logger.warn("Invalid menu category: vendorId is null");
            throw new InvalidInputException("Vendor ID is required");
        }
        if (!StringUtils.hasText(categoryDTO.getCategoryName())) {
            logger.warn("Invalid menu category: categoryName is empty");
            throw new InvalidInputException("Category name is required");
        }
    }

    private void validateMenuItemDTO(MenuItemDTO itemDTO) {
        if (itemDTO == null) {
            logger.warn("Invalid menu item: DTO is null");
            throw new InvalidInputException("Menu item data is required");
        }
        if (!StringUtils.hasText(itemDTO.getItemName())) {
            logger.warn("Invalid menu item: itemName is empty");
            throw new InvalidInputException("Item name is required");
        }
        if (itemDTO.getBasePrice() == null || itemDTO.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Invalid menu item: basePrice is null or negative");
            throw new InvalidInputException("Valid base price is required");
        }
        if (itemDTO.getVendorPrice() != null) {
            if (itemDTO.getVendorPrice().compareTo(BigDecimal.ZERO) < 0) {
                logger.warn("Invalid menu item: vendorPrice is negative");
                throw new InvalidInputException("Vendor price cannot be negative");
            }
            if (itemDTO.getBasePrice().compareTo(itemDTO.getVendorPrice()) <= 0) {
                logger.warn("Invalid menu item: basePrice must be greater than vendorPrice");
                throw new InvalidInputException("Base price must be greater than vendor price");
            }
        }
    }

    private void validateExcelMenuItemDTO(MenuItemDTO dto, int rowNumber) {
        if (!StringUtils.hasText(dto.getCategoryName())) {
            logger.warn("Invalid menu item at row {}: categoryName is empty", rowNumber);
            throw new InvalidInputException("Category name is required at row " + rowNumber);
        }
        if (!StringUtils.hasText(dto.getItemName())) {
            logger.warn("Invalid menu item at row {}: itemName is empty", rowNumber);
            throw new InvalidInputException("Item name is required at row " + rowNumber);
        }
        if (dto.getBasePrice() == null || dto.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Invalid menu item at row {}: basePrice is null or negative", rowNumber);
            throw new InvalidInputException("Valid base price is required at row " + rowNumber);
        }
        if (dto.getVendorPrice() != null) {
            if (dto.getVendorPrice().compareTo(BigDecimal.ZERO) < 0) {
                logger.warn("Invalid menu item at row {}: vendorPrice is negative", rowNumber);
                throw new InvalidInputException("Vendor price cannot be negative at row " + rowNumber);
            }
            if (dto.getBasePrice().compareTo(dto.getVendorPrice()) <= 0) {
                logger.warn("Invalid menu item at row {}: basePrice must be greater than vendorPrice", rowNumber);
                throw new InvalidInputException("Base price must be greater than vendor price at row " + rowNumber);
            }
        }
    }
}