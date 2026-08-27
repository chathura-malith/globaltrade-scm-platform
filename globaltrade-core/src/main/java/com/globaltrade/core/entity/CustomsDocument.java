package com.globaltrade.core.entity;

import com.globaltrade.core.enums.CustomsDocType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customs_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomsDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customs_declaration_id", nullable = false)
    private CustomsDeclaration customsDeclaration;

    @Column(name = "document_ref", length = 80, nullable = false, unique = true)
    private String documentRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", length = 40, nullable = false)
    private CustomsDocType docType;

    @Column(name = "issuing_authority", length = 150, nullable = false)
    private String issuingAuthority;

    @Lob
    @Column(name = "document_payload", columnDefinition = "TEXT", nullable = false)
    private String documentPayload;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
    }
}