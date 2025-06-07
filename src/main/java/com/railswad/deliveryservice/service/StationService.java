package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.StationDTO;
import com.railswad.deliveryservice.entity.Station;
import com.railswad.deliveryservice.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class StationService {

    @Autowired
    private StationRepository stationRepository;

    public StationDTO createStation(StationDTO stationDTO) {
        Station station = new Station();
        station.setStationCode(stationDTO.getStationCode());
        station.setStationName(stationDTO.getStationName());
        station.setCity(stationDTO.getCity());
        station.setState(stationDTO.getState());
        station.setPincode(stationDTO.getPincode());
        station.setLatitude(stationDTO.getLatitude());
        station.setLongitude(stationDTO.getLongitude());

        Station savedStation = stationRepository.save(station);
        stationDTO.setStationId(savedStation.getStationId());
        return stationDTO;
    }

    public StationDTO updateStation(Integer stationId, StationDTO stationDTO) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));
        station.setStationCode(stationDTO.getStationCode());
        station.setStationName(stationDTO.getStationName());
        station.setCity(stationDTO.getCity());
        station.setState(stationDTO.getState());
        station.setPincode(stationDTO.getPincode());
        station.setLatitude(stationDTO.getLatitude());
        station.setLongitude(stationDTO.getLongitude());

        Station updatedStation = stationRepository.save(station);
        stationDTO.setStationId(updatedStation.getStationId());
        return stationDTO;
    }

    public void deleteStation(Integer stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));
        stationRepository.delete(station);
    }

    public StationDTO getStationById(Integer stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));
        StationDTO stationDTO = new StationDTO();
        stationDTO.setStationId(station.getStationId());
        stationDTO.setStationCode(station.getStationCode());
        stationDTO.setStationName(station.getStationName());
        stationDTO.setCity(station.getCity());
        stationDTO.setState(station.getState());
        stationDTO.setPincode(station.getPincode());
        stationDTO.setLatitude(station.getLatitude());
        stationDTO.setLongitude(station.getLongitude());
        return stationDTO;
    }

    public Page<StationDTO> getAllStations(Pageable pageable) {
        return stationRepository.findAll(pageable).map(station -> {
            StationDTO stationDTO = new StationDTO();
            stationDTO.setStationId(station.getStationId());
            stationDTO.setStationCode(station.getStationCode());
            stationDTO.setStationName(station.getStationName());
            stationDTO.setCity(station.getCity());
            stationDTO.setState(station.getState());
            stationDTO.setPincode(station.getPincode());
            stationDTO.setLatitude(station.getLatitude());
            stationDTO.setLongitude(station.getLongitude());
            return stationDTO;
        });
    }
}
