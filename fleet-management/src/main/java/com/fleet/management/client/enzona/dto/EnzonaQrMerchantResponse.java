package com.fleet.management.client.enzona.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EnzonaQrMerchantResponse {

    @JsonProperty("qr_code")
    private String vendorIdentityCode;

    @JsonProperty("create_at")
    private String createdAt;

    @JsonProperty("update_at")
    private String updatedAt;

    private String image;
}
