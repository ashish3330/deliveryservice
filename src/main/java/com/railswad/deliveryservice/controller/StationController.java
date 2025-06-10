package com.railswad.deliveryservice.controller;

import com.railswad.deliveryservice.dto.StationDTO;
import com.railswad.deliveryservice.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    @Autowired
    private StationService stationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationDTO> createStation(@RequestBody StationDTO stationDTO) {
        return ResponseEntity.ok(stationService.createStation(stationDTO));
    }

    @PutMapping("/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StationDTO> updateStation(@PathVariable Integer stationId, @RequestBody StationDTO stationDTO) {
        return ResponseEntity.ok(stationService.updateStation(stationId, stationDTO));
    }

    @DeleteMapping("/{stationId}")
    @PreAuthorize("hasRole('ADMIN')")
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
    public ResponseEntity<Page<StationDTO>> getStations(
            @RequestParam(required = false) String stationName,
            @RequestParam(required = false) String stationCode,
            @RequestParam(required = false) String state,
            @PageableDefault(size = 10, sort = "stationName") Pageable pageable) {
        Page<StationDTO> stations = stationService.findStationsByFilters(stationName, stationCode, state, pageable);
        return ResponseEntity.ok(stations);
    }
}