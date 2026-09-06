package com.fleet.management.client.enzona.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnzonaQrMerchantRequest {

    @JsonProperty("merchant_uuid")
    private String merchantUuid;

    private String amount;

    private String currency;

    private String description;

    @JsonProperty("terminal_id")
    private String terminalId;

    @JsonProperty("return_url")
    private String returnUrl;

    @JsonProperty("notify_url")
    private String notifyUrl;

    private String permanent;
}
