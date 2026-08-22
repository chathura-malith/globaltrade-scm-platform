package com.globaltrade.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogRequestDto implements Serializable {

    @NotBlank(message = "Action is required")
    private String action;

    @NotBlank(message = "Entity name is required")
    private String entityName;

    private Long entityId;

    @NotBlank(message = "Performed by user is required")
    private String performedBy;

    private String details;
}