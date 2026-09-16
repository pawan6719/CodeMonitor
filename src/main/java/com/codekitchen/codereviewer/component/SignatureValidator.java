package com.codekitchen.codereviewer.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class SignatureValidator {

    private final String webhookSecret;
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Logger log = LoggerFactory.getLogger(SignatureValidator.class);

    @Value("${app.signature.validation: true}")
    private boolean isSignatureValidation;
    public SignatureValidator(@Value("${github.webhook.secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isValid(String signatureHeader, String payload) {
        if(!isSignatureValidation) return true;
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Webhook secret is null or Blank");
            return false;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            log.warn("signature header wass null or does not start with sha256=");
            return false;
        }

        String signature = signatureHeader.substring(7);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8)
        );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Exception while checking for signature validity", e);
            return false;
        }
    }
}
