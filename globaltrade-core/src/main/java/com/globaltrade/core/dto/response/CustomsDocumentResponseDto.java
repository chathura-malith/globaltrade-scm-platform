package com.globaltrade.core.dto.response;

import com.globaltrade.core.enums.CustomsDocType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomsDocumentResponseDto implements Serializable {

    private String documentRef;
    private CustomsDocType docType;
    private String issuingAuthority;
    private String documentPayload;
    private LocalDateTime issuedAt;
}