package com.railswad.deliveryservice.util;

import com.railswad.deliveryservice.dto.MenuItemDTO;
import com.railswad.deliveryservice.dto.OrderExcelDTO;
import com.railswad.deliveryservice.dto.StationDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ExcelHelper {

    private static final Logger logger = LoggerFactory.getLogger(ExcelHelper.class);
    public static final String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


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
                    String vendorPriceStr = getValue(formatter, row, headerMap, "vendorPrice");
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

    public List<StationDTO> excelToStationDTOs(InputStream is) throws IOException {
        logger.info("Starting Excel file parsing for StationDTOs");
        List<StationDTO> stationDTOs = new ArrayList<>();
        Workbook workbook = null;

        try {
            workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                logger.warn("Excel sheet is empty");
                return stationDTOs;
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
                StationDTO dto = new StationDTO();

                try {
                    dto.setStationCode(getValue(formatter, row, headerMap, "stationcode"));
                    dto.setStationName(getValue(formatter, row, headerMap, "stationname"));
                    dto.setCity(getValue(formatter, row, headerMap, "city"));
                    dto.setState(getValue(formatter, row, headerMap, "state"));
                    dto.setPincode(getValue(formatter, row, headerMap, "pincode"));

                    String latitudeStr = getValue(formatter, row, headerMap, "latitude");
                    dto.setLatitude(latitudeStr.isEmpty() ? null : Double.parseDouble(latitudeStr));

                    String longitudeStr = getValue(formatter, row, headerMap, "longitude");
                    dto.setLongitude(longitudeStr.isEmpty() ? null : Double.parseDouble(longitudeStr));

                    // Basic validation
                    if (dto.getStationCode() == null || dto.getStationName() == null || dto.getCity() == null) {
                        throw new IllegalArgumentException("Missing required fields (stationcode, stationname, city)");
                    }

                    stationDTOs.add(dto);
                    logger.debug("Parsed row {}: {}", row.getRowNum() + 1, dto);
                } catch (Exception e) {
                    logger.error("Error parsing row {}: {}", row.getRowNum() + 1, e.getMessage());
                    throw new RuntimeException("Error processing row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }

            logger.info("Successfully parsed {} stations from Excel", stationDTOs.size());
            return stationDTOs;
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


    public Workbook generateOrdersExcel(List<OrderExcelDTO> orders) {
        logger.info("Generating Excel for {} orders", orders.size());
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Orders");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Order ID", "Station Name", "Vendor Name", "Number of Items", "Total Amount", "Final Amount", "Tax Amount", "GST Number", "Order Date", "Order Payment Type"};
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("₹#,##0.00"));

        int rowNum = 1;
        for (OrderExcelDTO order : orders) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(order.getOrderId());
            row.createCell(1).setCellValue(order.getStationName() != null ? order.getStationName() : "");
            row.createCell(2).setCellValue(order.getVendorName() != null ? order.getVendorName() : "");
            row.createCell(3).setCellValue(order.getNumberOfItems() != null ? order.getNumberOfItems() : 0);

            Cell totalAmountCell = row.createCell(4);
            totalAmountCell.setCellValue(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);
            totalAmountCell.setCellStyle(currencyStyle);

            Cell finalAmountCell = row.createCell(5);
            finalAmountCell.setCellValue(order.getFinalAmount() != null ? order.getFinalAmount() : 0.0);
            finalAmountCell.setCellStyle(currencyStyle);

            Cell taxAmountCell = row.createCell(6);
            taxAmountCell.setCellValue(order.getTaxAmount() != null ? order.getTaxAmount() : 0.0);
            taxAmountCell.setCellStyle(currencyStyle);

            row.createCell(7).setCellValue(order.getGstNumber() != null ? order.getGstNumber() : "");

            // Format Order Date as string in IST
            Cell orderDateCell = row.createCell(8);
            orderDateCell.setCellValue(order.getOrderDate() != null ? order.getOrderDate().format(DATE_TIME_FORMATTER) : "");

            row.createCell(9).setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod() : "");
        }

        // Set fixed column widths
        int[] columnWidths = {5000, 8000, 8000, 4000, 4000, 4000, 4000, 6000, 6000, 5000}; // Widths in 1/256th of a character
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, columnWidths[i]);
        }

        logger.info("Excel generation completed with {} rows", rowNum - 1);
        return workbook;
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
