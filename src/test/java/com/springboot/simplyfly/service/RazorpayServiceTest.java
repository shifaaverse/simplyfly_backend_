package com.springboot.simplyfly.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

// Only the parts that do not call Razorpay are tested here.
class RazorpayServiceTest {

    private RazorpayService razorpayService;

    @BeforeEach
    void setUp() {
        razorpayService = new RazorpayService();
        ReflectionTestUtils.setField(razorpayService, "keyId", "rzp_test_abc");
        ReflectionTestUtils.setField(razorpayService, "keySecret", "secret123");
    }

    // Same calculation as Razorpay: HMAC SHA256 of "orderId|paymentId"
    private String sign(String orderId, String paymentId, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void isEnabledTest() {
        assertTrue(razorpayService.isEnabled());
        assertEquals("rzp_test_abc", razorpayService.getKeyId());
    }

    @Test
    void isEnabledTest_KeysEmpty() {
        ReflectionTestUtils.setField(razorpayService, "keyId", "");
        ReflectionTestUtils.setField(razorpayService, "keySecret", "");

        assertFalse(razorpayService.isEnabled());
    }

    @Test
    void verifySignatureTest() throws Exception {
        String signature = sign("order_1", "pay_1", "secret123");

        assertTrue(razorpayService.verifySignature("order_1", "pay_1", signature));
    }

    @Test
    void verifySignatureTest_WrongSignature() {
        assertFalse(razorpayService.verifySignature("order_1", "pay_1", "fake-signature"));
    }

    @Test
    void verifySignatureTest_SignedWithOtherSecret() throws Exception {
        String signature = sign("order_1", "pay_1", "other-secret");

        assertFalse(razorpayService.verifySignature("order_1", "pay_1", signature));
    }

    @Test
    void verifySignatureTest_PaymentOfOtherOrder() throws Exception {
        String signature = sign("order_1", "pay_1", "secret123");

        // same payment, but claimed for another order
        assertFalse(razorpayService.verifySignature("order_2", "pay_1", signature));
    }
}
