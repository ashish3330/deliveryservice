package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.StationDTO;
import com.railswad.deliveryservice.service.StationService;
import com.railswad.deliveryservice.util.ExcelHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {
    private static final Logger logger = LoggerFactory.getLogger(StationController.class);

    @Autowired
    private StationService stationService;

    @Autowired
    private ExcelHelper excelHelper;

    @PostMapping
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<StationDTO> createStation(@RequestBody StationDTO stationDTO) {
        return ResponseEntity.ok(stationService.createStation(stationDTO));
    }

    @PutMapping("/{stationId}")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<StationDTO> updateStation(@PathVariable Integer stationId, @RequestBody StationDTO stationDTO) {
        return ResponseEntity.ok(stationService.updateStation(stationId, stationDTO));
    }

    @DeleteMapping("/{stationId}")
//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteStation(@PathVariable Integer stationId) {
        stationService.deleteStation(stationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{stationId}")
    public ResponseEntity<StationDTO> getStationById(@PathVariable Integer stationId) {
        return ResponseEntity.ok(stationService.getStationById(stationId));
    }

//    @GetMapping
//    public ResponseEntity<Page<StationDTO>> getAllStations(Pageable pageable) {
//        return ResponseEntity.ok(stationService.getAllStations(pageable));
//    }


    @GetMapping
    public ResponseEntity<Page<StationDTO>> getStationsPaged(
            @RequestParam(required = false) String stationName,
            @RequestParam(required = false) String stationCode,
            @RequestParam(required = false) String state,
            @PageableDefault(size = 10, sort = "stationName") Pageable pageable) {
        Page<StationDTO> stations = stationService.findStationsByFilters(stationName, stationCode, state, pageable);
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/all")
    public List<StationDTO> getStations(
            @RequestParam(required = false) String stationName,
            @RequestParam(required = false) String stationCode,
            @RequestParam(required = false) String state) {
        return stationService.getStations(stationName, stationCode, state);
    }


//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/bulk-upload")
    public ResponseEntity<?> bulkUploadStations(@RequestParam("file") MultipartFile file) {
        logger.info("Received bulk upload request for file: {}", file.getOriginalFilename());

        if (!excelHelper.hasExcelFormat(file)) {
            logger.warn("Invalid file format for upload: {}", file.getContentType());
            return ResponseEntity.badRequest().body("Only .xlsx files are supported");
        }

        try {
            List<StationDTO> stationDTOs = excelHelper.excelToStationDTOs(file.getInputStream());
            List<StationDTO> savedStations = stationService.bulkCreateStations(stationDTOs);
            logger.info("Successfully uploaded {} stations", savedStations.size());
            return ResponseEntity.ok(savedStations);
        } catch (IOException e) {
            logger.error("Error processing Excel file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Bulk upload failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Bulk upload failed: " + e.getMessage());
        }
    }

}