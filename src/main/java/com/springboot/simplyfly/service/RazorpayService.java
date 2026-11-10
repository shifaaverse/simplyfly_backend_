package com.springboot.simplyfly.service;

import com.springboot.simplyfly.enums.PaymentMethod;
import com.springboot.simplyfly.exception.InvalidCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/*
 * Talks to Razorpay (test mode). Keys come from application.properties.
 * If the keys are empty, Razorpay is "not enabled" and the demo payment is used.
 * */
@Service
@Slf4j
public class RazorpayService {

    public static final String CURRENCY = "INR";

    private static final String BASE_URL = "https://api.razorpay.com/v1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${razorpay.key.id:}")
    private String keyId;

    @Value("${razorpay.key.secret:}")
    private String keySecret;

    private final RestClient restClient = RestClient.create();

    // True when both Razorpay keys are configured.
    public boolean isEnabled() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    public String getKeyId() {
        return keyId;
    }

    // Razorpay works in paise (1 rupee = 100 paise).
    public static long toPaise(double amount) {
        return Math.round(amount * 100);
    }

    // Header value for Razorpay's basic authentication.
    private String getAuthHeader() {
        String credentials = keyId + ":" + keySecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    // Create a Razorpay order for the amount (in rupees) and return its id.
    public String createOrder(double amount, String receipt) {
        Map<String, Object> body = Map.of(
                "amount", toPaise(amount),
                "currency", CURRENCY,
                "receipt", receipt);

        try {
            Map<?, ?> response = restClient.post()
                    .uri(BASE_URL + "/orders")
                    .header("Authorization", getAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return String.valueOf(response.get("id"));
        } catch (RestClientException e) {
            log.warn("Razorpay order failed: {}", e.getMessage());
            throw new InvalidCallException("Could not start the payment. Please check the Razorpay keys");
        }
    }

    // Check the signature sent by Razorpay: HMAC SHA256 of "orderId|paymentId" using the secret key.
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));

            byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    // Find how the passenger paid (card, upi or netbanking). Falls back to CARD if Razorpay cannot be reached.
    public PaymentMethod getPaymentMethod(String paymentId) {
        try {
            Map<?, ?> response = restClient.get()
                    .uri(BASE_URL + "/payments/" + paymentId)
                    .header("Authorization", getAuthHeader())
                    .retrieve()
                    .body(Map.class);

            String method = String.valueOf(response.get("method"));

            return switch (method) {
                case "upi" -> PaymentMethod.UPI;
                case "netbanking" -> PaymentMethod.NET_BANKING;
                default -> PaymentMethod.CARD;
            };
        } catch (RestClientException e) {
            log.warn("Could not read payment method from Razorpay: {}", e.getMessage());
            return PaymentMethod.CARD;
        }
    }
}
