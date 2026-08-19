package com.globaltrade.core.entity;

import com.globaltrade.core.enums.CustomsStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customs_declarations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomsDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "declaration_number", length = 60, nullable = false, unique = true)
    private String declarationNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "customs_status", length = 30, nullable = false)
    private CustomsStatus customsStatus;

    @Column(name = "duty_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal dutyAmount = BigDecimal.ZERO;

    @Column(name = "penalty_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by_user_id")
    private User inspectedBy;

    @Column(name = "clearance_date")
    private LocalDateTime clearanceDate;

    @Lob
    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @PrePersist
    protected void onCreate() {
        if (this.customsStatus == null) {
            this.customsStatus = CustomsStatus.DRAFT;
        }
        if (this.dutyAmount == null) {
            this.dutyAmount = BigDecimal.ZERO;
        }
        if (this.penaltyAmount == null) {
            this.penaltyAmount = BigDecimal.ZERO;
        }
    }
}