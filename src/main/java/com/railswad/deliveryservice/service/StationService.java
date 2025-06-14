    package com.railswad.deliveryservice.service;

    import com.railswad.deliveryservice.dto.StationDTO;
    import com.railswad.deliveryservice.entity.Station;
    import com.railswad.deliveryservice.exception.ResourceNotFoundException;
    import com.railswad.deliveryservice.exception.DuplicateResourceException;
    import com.railswad.deliveryservice.repository.OrderRepository;
    import com.railswad.deliveryservice.repository.StationRepository;
    import com.railswad.deliveryservice.repository.VendorRepository;
    import jakarta.persistence.criteria.Predicate;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.cache.annotation.CacheEvict;
    import org.springframework.cache.annotation.Cacheable;
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

        @Autowired
        private VendorRepository vendorRepository;

        @Autowired
        private OrderRepository orderRepository;

        @CacheEvict(value = {"stations", "station"}, allEntries = true)
        public StationDTO createStation(StationDTO stationDTO) {
            Specification<Station> spec = StationSpecification.checkDuplicate(
                    stationDTO.getStationName(),
                    stationDTO.getCity(),
                    stationDTO.getStationCode());

            if (!stationRepository.findAll(spec).isEmpty()) {
                throw new DuplicateResourceException(
                        "Station already exists with name: " + stationDTO.getStationName() +
                                ", city: " + stationDTO.getCity() +
                                ", code: " + stationDTO.getStationCode());
            }

            Station savedStation = stationRepository.save(mapToEntity(stationDTO));
            return mapToDTO(savedStation);
        }

        @CacheEvict(value = {"stations", "station"}, allEntries = true)
        public List<StationDTO> bulkCreateStations(List<StationDTO> stationDTOs) {
            List<StationDTO> savedStations = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (StationDTO dto : stationDTOs) {
                try {
                    Specification<Station> spec = StationSpecification.checkDuplicate(
                            dto.getStationName(),
                            dto.getCity(),
                            dto.getStationCode());

                    if (!stationRepository.findAll(spec).isEmpty()) {
                        errors.add("Duplicate station: " + dto.getStationName());
                        continue;
                    }

                    Station saved = stationRepository.save(mapToEntity(dto));
                    savedStations.add(mapToDTO(saved));

                } catch (Exception e) {
                    errors.add("Error processing " + dto.getStationName() + ": " + e.getMessage());
                }
            }

            if (!errors.isEmpty()) {
                throw new RuntimeException("Bulk upload completed with errors: " + String.join("; ", errors));
            }

            return savedStations;
        }

        @CacheEvict(value = {"stations", "station"}, allEntries = true)
        public StationDTO updateStation(Integer stationId, StationDTO dto) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));

            Specification<Station> spec = StationSpecification.checkDuplicate(dto.getStationName(), dto.getCity(), dto.getStationCode());
            List<Station> duplicates = stationRepository.findAll(spec);

            if (!duplicates.isEmpty() && !duplicates.get(0).getStationId().equals(stationId)) {
                throw new DuplicateResourceException(
                        "Another station already exists with name: " + dto.getStationName() +
                                ", city: " + dto.getCity() +
                                ", code: " + dto.getStationCode());
            }

            station.setStationCode(dto.getStationCode());
            station.setStationName(dto.getStationName());
            station.setCity(dto.getCity());
            station.setState(dto.getState());
            station.setPincode(dto.getPincode());
            station.setLatitude(dto.getLatitude());
            station.setLongitude(dto.getLongitude());

            return mapToDTO(stationRepository.save(station));
        }

        @CacheEvict(value = {"stations", "station"}, allEntries = true)
        public void deleteStation(Integer stationId) {
            if (hasDependencies(stationId)) {
                throw new IllegalStateException("Cannot delete station with ID " + stationId + " because it is referenced.");
            }

            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));

            stationRepository.delete(station);
        }

        @Cacheable(value = "station", key = "#stationId")
        public StationDTO getStationById(Integer stationId) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + stationId));
            return mapToDTO(station);
        }

        @Cacheable(value = "stations", key = "#stationName + '-' + #stationCode + '-' + #city")
        public List<StationDTO> getStations(String stationName, String stationCode, String city) {
            List<Station> stations;

            if (StringUtils.hasText(stationName) || StringUtils.hasText(stationCode) || StringUtils.hasText(city)) {
                Specification<Station> spec = StationSpecification.filterBy(stationName, stationCode, city);
                stations = stationRepository.findAll(spec);
            } else {
                stations = stationRepository.findAll();
            }

            return stations.stream().map(this::mapToDTO).collect(Collectors.toList());
        }

        public Page<StationDTO> findStationsByFilters(String stationName, String stationCode, String state, Pageable pageable) {
            Specification<Station> spec = StationSpecification.filterBy(stationName, stationCode, state);
            return stationRepository.findAll(spec, pageable).map(this::mapToDTO);
        }

        @Cacheable(value = "stations", key = "'all-' + #pageable.pageNumber")
        public Page<StationDTO> getAllStations(Pageable pageable) {
            return stationRepository.findAll(pageable).map(this::mapToDTO);
        }

        private StationDTO mapToDTO(Station station) {
            StationDTO dto = new StationDTO();
            dto.setStationId(station.getStationId());
            dto.setStationCode(station.getStationCode());
            dto.setStationName(station.getStationName());
            dto.setCity(station.getCity());
            dto.setState(station.getState());
            dto.setPincode(station.getPincode());
            dto.setLatitude(station.getLatitude());
            dto.setLongitude(station.getLongitude());
            return dto;
        }

        private Station mapToEntity(StationDTO dto) {
            Station station = new Station();
            station.setStationCode(dto.getStationCode());
            station.setStationName(dto.getStationName());
            station.setCity(dto.getCity());
            station.setState(dto.getState());
            station.setPincode(dto.getPincode());
            station.setLatitude(dto.getLatitude());
            station.setLongitude(dto.getLongitude());
            return station;
        }

        public boolean hasDependencies(Integer stationId) {
            long vendorCount = vendorRepository.countByStationStationId(stationId);
            long orderCount = orderRepository.countByDeliveryStationStationId(stationId);
            return vendorCount > 0 || orderCount > 0;
        }

        public static class StationSpecification {
            public static Specification<Station> filterBy(String stationName, String stationCode, String state) {
                return (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (StringUtils.hasText(stationName)) {
                        predicates.add(cb.like(cb.lower(root.get("stationName")), "%" + stationName.toLowerCase() + "%"));
                    }
                    if (StringUtils.hasText(stationCode)) {
                        predicates.add(cb.like(cb.lower(root.get("stationCode")), "%" + stationCode.toLowerCase() + "%"));
                    }
                    if (StringUtils.hasText(state)) {
                        predicates.add(cb.like(cb.lower(root.get("state")), "%" + state.toLowerCase() + "%"));
                    }
                    return cb.and(predicates.toArray(new Predicate[0]));
                };
            }

            public static Specification<Station> checkDuplicate(String stationName, String city, String stationCode) {
                return (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (StringUtils.hasText(stationName)) {
                        predicates.add(cb.equal(cb.lower(root.get("stationName")), stationName.toLowerCase()));
                    }
                    if (StringUtils.hasText(city)) {
                        predicates.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
                    }
                    if (StringUtils.hasText(stationCode)) {
                        predicates.add(cb.equal(cb.lower(root.get("stationCode")), stationCode.toLowerCase()));
                    }
                    return cb.and(predicates.toArray(new Predicate[0]));
                };
            }
        }
    }
