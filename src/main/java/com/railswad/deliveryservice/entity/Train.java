package com.railswad.deliveryservice.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "trains")
@Data
public class Train {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer trainId;

    @Column(name = "train_number", unique = true, nullable = false)
    private String trainNumber;

    @Column(name = "train_name", nullable = false)
    private String trainName;

    @Column(name = "train_type", nullable = false)
    private String trainType;

    @ManyToOne
    @JoinColumn(name = "source_station_id")
    private Station sourceStation;

    @ManyToOne
    @JoinColumn(name = "destination_station_id")
    private Station destinationStation;

    private String averageJourneyTime;
}