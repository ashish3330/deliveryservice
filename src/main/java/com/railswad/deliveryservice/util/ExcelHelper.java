package com.railswad.deliveryservice.util;

import com.railswad.deliveryservice.dto.MenuItemDTO;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

@Component
public class ExcelHelper {

    private static final Logger logger = LoggerFactory.getLogger(ExcelHelper.class);
    public static final String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public boolean hasExcelFormat(MultipartFile file) {
        boolean isExcel = TYPE.equals(file.getContentType());
        logger.info("Checking file format: {}. Is Excel: {}", file.getContentType(), isExcel);
        return isExcel;
    }

    public List<MenuItemDTO> excelToMenuItemDTOs(InputStream is) throws IOException {
        logger.info("Starting Excel file parsing");
        List<MenuItemDTO> menuItemDTOs = new ArrayList<>();
        Workbook workbook = null;

        try {
            workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                logger.warn("Excel sheet is empty");
                return menuItemDTOs;
            }

            // Step 1: Read header row
            Row headerRow = rowIterator.next();
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell).trim().toLowerCase();
                headerMap.put(header, cell.getColumnIndex());
            }

            logger.info("Header columns found: {}", headerMap.keySet());

            // Step 2: Read data rows
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                MenuItemDTO dto = new MenuItemDTO();

                try {
                    dto.setCategoryName(getValue(formatter, row, headerMap, "categoryname"));
                    dto.setItemName(getValue(formatter, row, headerMap, "itemname"));
                    dto.setDescription(getValue(formatter, row, headerMap, "description"));

                    String basePriceStr = getValue(formatter, row, headerMap, "basePrice");
                    dto.setBasePrice(basePriceStr.isEmpty() ? null : new BigDecimal(basePriceStr));

                    String vendorPriceStr = getValue(formatter, row, headerMap, "basePrice");
                    dto.setVendorPrice(basePriceStr.isEmpty() ? null : new BigDecimal(vendorPriceStr));

                    String vegetarianStr = getValue(formatter, row, headerMap, "vegetarian");
                    dto.setVegetarian(vegetarianStr.equalsIgnoreCase("true"));

                    String availableStr = getValue(formatter, row, headerMap, "available");
                    dto.setAvailable(availableStr.equalsIgnoreCase("true"));

                    String prepTime = getValue(formatter, row, headerMap, "preparationtimemin");
                    dto.setPreparationTimeMin(prepTime.isEmpty() ? null : Integer.parseInt(prepTime));

                    dto.setImageUrl(getValue(formatter, row, headerMap, "imageurl"));

                    String orderStr = getValue(formatter, row, headerMap, "displayorder");
                    dto.setDisplayOrder(orderStr.isEmpty() ? 0 : Integer.parseInt(orderStr));

                    String startTime = getValue(formatter, row, headerMap, "availablestarttime");
                    dto.setAvailableStartTime(startTime.isEmpty() ? null : LocalTime.parse(startTime));

                    String endTime = getValue(formatter, row, headerMap, "availableendtime");
                    dto.setAvailableEndTime(endTime.isEmpty() ? null : LocalTime.parse(endTime));

                    menuItemDTOs.add(dto);
                    logger.debug("Parsed row {}: {}", row.getRowNum() + 1, dto);
                } catch (Exception e) {
                    logger.error("Error parsing row {}: {}", row.getRowNum() + 1, e.getMessage());
                    throw new RuntimeException("Error processing row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }

            logger.info("Successfully parsed {} menu items from Excel", menuItemDTOs.size());
            return menuItemDTOs;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                    logger.debug("Closed Excel workbook");
                } catch (IOException e) {
                    logger.error("Error closing workbook: {}", e.getMessage());
                }
            }
        }
    }

    private String getValue(DataFormatter formatter, Row row, Map<String, Integer> headerMap, String key) {
        Integer colIndex = headerMap.get(key.toLowerCase());
        if (colIndex == null) {
            logger.warn("Column '{}' not found in header", key);
            return "";
        }
        Cell cell = row.getCell(colIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }
}
