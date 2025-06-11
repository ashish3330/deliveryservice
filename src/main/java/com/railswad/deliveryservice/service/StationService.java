package com.railswad.deliveryservice.service;

import com.railswad.deliveryservice.dto.StationDTO;
import com.railswad.deliveryservice.entity.Station;
import com.railswad.deliveryservice.exception.ResourceNotFoundException;
import com.railswad.deliveryservice.exception.DuplicateResourceException;
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
import java.util.stream.Collectors;

@Service
public class StationService {

    @Autowired
    private StationRepository stationRepository;

    public StationDTO createStation(StationDTO stationDTO) {
        // Check if station already exists with same name, city, and code
        Specification<Station> spec = StationSpecification.checkDuplicate(
                stationDTO.getStationName(),
                stationDTO.getCity(),
                stationDTO.getStationCode());
        List<Station> existingStations = stationRepository.findAll(spec);

        if (!existingStations.isEmpty()) {
            throw new DuplicateResourceException(
                    "Station already exists with name: " + stationDTO.getStationName() +
                            ", city: " + stationDTO.getCity() +
                            ", code: " + stationDTO.getStationCode());
        }

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

    public List<StationDTO> bulkCreateStations(List<StationDTO> stationDTOs) {
        List<StationDTO> savedStations = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (StationDTO stationDTO : stationDTOs) {
            try {
                // Check for duplicate
                Specification<Station> spec = StationSpecification.checkDuplicate(
                        stationDTO.getStationName(),
                        stationDTO.getCity(),
                        stationDTO.getStationCode());
                List<Station> existingStations = stationRepository.findAll(spec);

                if (!existingStations.isEmpty()) {
                    errors.add("Station already exists with name: " + stationDTO.getStationName() +
                            ", city: " + stationDTO.getCity() +
                            ", code: " + stationDTO.getStationCode());
                    continue;
                }

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
                savedStations.add(stationDTO);

            } catch (Exception e) {
                errors.add("Error processing station " + stationDTO.getStationName() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("Bulk upload completed with errors: " + String.join("; ", errors));
        }

        return savedStations;
    }

    public StationDTO updateStation(Integer stationId, StationDTO stationDTO) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));

        // Check if updated values would create a duplicate
        Specification<Station> spec = StationSpecification.checkDuplicate(
                stationDTO.getStationName(),
                stationDTO.getCity(),
                stationDTO.getStationCode());
        List<Station> existingStations = stationRepository.findAll(spec);

        if (!existingStations.isEmpty() && !existingStations.get(0).getStationId().equals(stationId)) {
            throw new DuplicateResourceException(
                    "Another station already exists with name: " + stationDTO.getStationName() +
                            ", city: " + stationDTO.getCity() +
                            ", code: " + stationDTO.getStationCode());
        }

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

    public List<StationDTO> getStations(String stationName, String stationCode, String state) {
        List<Station> stations;
        if (StringUtils.hasText(stationName) || StringUtils.hasText(stationCode) || StringUtils.hasText(state)) {
            Specification<Station> spec = StationSpecification.filterBy(stationName, stationCode, state);
            stations = stationRepository.findAll(spec);
        } else {
            stations = stationRepository.findAll();
        }
        return stations.stream().map(station -> {
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
        }).collect(Collectors.toList());
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

        public static Specification<Station> checkDuplicate(String stationName, String city, String stationCode) {
            return (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (StringUtils.hasText(stationName)) {
                    predicates.add(criteriaBuilder.equal(
                            criteriaBuilder.lower(root.get("stationName")),
                            stationName.toLowerCase()));
                }

                if (StringUtils.hasText(city)) {
                    predicates.add(criteriaBuilder.equal(
                            criteriaBuilder.lower(root.get("city")),
                            city.toLowerCase()));
                }

                if (StringUtils.hasText(stationCode)) {
                    predicates.add(criteriaBuilder.equal(
                            criteriaBuilder.lower(root.get("stationCode")),
                            stationCode.toLowerCase()));
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
        }
    }



}