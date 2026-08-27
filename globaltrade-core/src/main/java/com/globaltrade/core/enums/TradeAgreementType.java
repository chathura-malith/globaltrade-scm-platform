package com.globaltrade.core.enums;

import lombok.Getter;

@Getter
public enum TradeAgreementType {
    EU_SINGLE_MARKET(0.00, "European Single Market Tariff Exemption"),
    USMCA(0.02, "United States-Mexico-Canada Agreement Preferential Rate"),
    ASEAN_FTA(0.03, "ASEAN Free Trade Area Concession Rate"),
    GENERAL_MFN(0.085, "Most-Favoured-Nation Standard Customs Tariff");

    private final double preferentialDutyRate;
    private final String description;

    TradeAgreementType(double preferentialDutyRate, String description) {
        this.preferentialDutyRate = preferentialDutyRate;
        this.description = description;
    }
}