package com.railswad.deliveryservice.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    public static ZonedDateTime parseToIstZonedDateTime(String dateString, boolean isStartDate) {
        if (!StringUtils.hasText(dateString)) {
            return null;
        }
        try {
            LocalDate localDate;
            if (dateString.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?")) {
                localDate = LocalDate.parse(dateString.split("T")[0], DATE_FORMATTER);
            } else {
                localDate = LocalDate.parse(dateString, DATE_FORMATTER);
            }
            if (isStartDate) {
                return localDate.atStartOfDay(IST_ZONE);
            } else {
                return localDate.atTime(23, 59, 59).atZone(IST_ZONE);
            }
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse date string {} to IST ZonedDateTime: {}", dateString, e.getMessage());
            throw new IllegalArgumentException("Invalid date format: " + dateString, e);
        }
    }
}