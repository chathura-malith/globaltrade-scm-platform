package com.globaltrade.core.entity;

import com.globaltrade.core.enums.ShipmentStatus;
import com.globaltrade.core.enums.TransportMode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "tracking_number", length = 60, nullable = false, unique = true)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
    private ShipmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", length = 30, nullable = false)
    private TransportMode transportMode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "origin_street")),
            @AttributeOverride(name = "city", column = @Column(name = "origin_city")),
            @AttributeOverride(name = "state", column = @Column(name = "origin_state")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "origin_postal")),
            @AttributeOverride(name = "country", column = @Column(name = "origin_country"))
    })
    private Address originAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "dest_street")),
            @AttributeOverride(name = "city", column = @Column(name = "dest_city")),
            @AttributeOverride(name = "state", column = @Column(name = "dest_state")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "dest_postal")),
            @AttributeOverride(name = "country", column = @Column(name = "dest_country"))
    })
    private Address destinationAddress;

    @Column(name = "cargo_weight_kg")
    private Double cargoWeightKg;

    @Column(name = "cargo_volume_cbm")
    private Double cargoVolumeCbm;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ShipmentStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}