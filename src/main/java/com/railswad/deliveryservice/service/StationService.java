package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.StationDTO;
import com.railswad.deliveryservice.entity.Station;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.repository.StationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

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

    public Page<StationDTO> findStationsByFilters(String stationName, String stationCode, String state, Pageable pageable) {
        Specification<Station> spec = StationSpecification.filterBy(stationName, stationCode, state);
        return stationRepository.findAll(spec, pageable).map(station -> {
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

    // Specification class for dynamic filtering
    public static class StationSpecification {
        public static Specification<Station> filterBy(String stationName, String stationCode, String state) {
            return (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (StringUtils.hasText(stationName)) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("stationName")),
                            "%" + stationName.toLowerCase() + "%"));
                }

                if (StringUtils.hasText(stationCode)) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("stationCode")),
                            "%" + stationCode.toLowerCase() + "%"));
                }

                if (StringUtils.hasText(state)) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("state")),
                            "%" + state.toLowerCase() + "%"));
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
        }
    }


}