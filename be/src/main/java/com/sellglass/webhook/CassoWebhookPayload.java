package com.sellglass.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CassoWebhookPayload {

    private int error;
    private Data data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data {
        private long id;
        private long amount;
        private String description;

        @JsonProperty("transaction_type")
        private String transactionType;

        @JsonProperty("bank_sub_acc_id")
        private String bankSubAccId;
    }
}
