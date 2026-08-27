package com.globaltrade.core.entity;

import com.globaltrade.core.enums.CustomsStatus;
import com.globaltrade.core.enums.TradeAgreementType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_agreement_type", length = 40, nullable = false)
    private TradeAgreementType tradeAgreementType;

    @Column(name = "origin_country", length = 100, nullable = false)
    private String originCountry;

    @Column(name = "destination_country", length = 100, nullable = false)
    private String destinationCountry;

    @Column(name = "total_declared_value", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalDeclaredValue = BigDecimal.ZERO;

    @Column(name = "duty_rate_percentage", nullable = false)
    @Builder.Default
    private Double dutyRatePercentage = 0.0;

    @Column(name = "duty_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal dutyAmount = BigDecimal.ZERO;

    @Column(name = "penalty_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "sanction_flagged", nullable = false)
    @Builder.Default
    private boolean sanctionFlagged = false;

    @Column(name = "submission_deadline")
    private LocalDateTime submissionDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by_user_id")
    private User inspectedBy;

    @Column(name = "clearance_date")
    private LocalDateTime clearanceDate;

    @Lob
    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @OneToMany(mappedBy = "customsDeclaration", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustomsDocument> documents = new ArrayList<>();

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
        if (this.customsStatus == null) {
            this.customsStatus = CustomsStatus.DRAFT;
        }
        if (this.tradeAgreementType == null) {
            this.tradeAgreementType = TradeAgreementType.GENERAL_MFN;
        }
        if (this.dutyAmount == null) {
            this.dutyAmount = BigDecimal.ZERO;
        }
        if (this.penaltyAmount == null) {
            this.penaltyAmount = BigDecimal.ZERO;
        }
        if (this.totalDeclaredValue == null) {
            this.totalDeclaredValue = BigDecimal.ZERO;
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}