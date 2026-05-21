package id.ac.ui.cs.advprog.bewallettransaksi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCallbackRequest {
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("status_code")
    private String statusCode;

    @JsonProperty("gross_amount")
    private String grossAmount;

    @JsonProperty("transaction_status")
    private String transactionStatus;

    @JsonProperty("signature_key")
    private String signatureKey;
}
