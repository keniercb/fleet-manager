package com.fleet.management.client.enzona.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EnzonaQrInfoResponse {

    @JsonProperty("qr_code")
    private String qrCode;

    private String amount;

    private String currency;

    private String description;

    private String name;

    private String avatar;

    @JsonProperty("require_password")
    private String requirePassword;

    private String username;
}
