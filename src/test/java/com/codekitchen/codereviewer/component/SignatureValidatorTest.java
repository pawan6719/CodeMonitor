package com.codekitchen.codereviewer.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class SignatureValidatorTest {

    private static final String SECRET = "434ce5fdcbd712c43d0ca8f16b4741512cf7ca827725a9ab41ab88b2c21f67c5";
    private static final String GITHUB_SIGNATURE = "sha256=3904b49567aeb2f1c02f3261029f4dd2b23ad6ca069fa3621ce4ec16b64c2a7c";
    private SignatureValidator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        validator = new SignatureValidator(SECRET);
        ReflectionTestUtils.setField(validator, "isSignatureValidation", true);
    }

    private String calculateExpectedSignature(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return "sha256=" + HexFormat.of().formatHex(hash);
    }

    private String readResource(String resourceName) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourceName);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("test minified wire JSON which matches the signature exactly")
    void testRootCauseMinifiedPayloadMatchesSignature() throws Exception {
        String formattedJson = readResource("signature_validation.json");
        
        // Minify the JSON to restore the exact raw byte stream sent over the wire by GitHub
        JsonNode jsonNode = objectMapper.readTree(formattedJson);
        String minifiedWireJson = objectMapper.writeValueAsString(jsonNode);

        // Verify that SignatureValidator returns true for GitHub's wire payload and signature
        boolean isValid = validator.isValid(GITHUB_SIGNATURE, minifiedWireJson);
        assertTrue(isValid, "HMAC validation succeeds because GitHub signs the minified JSON on the wire");

        // Verify that passing the pretty-printed JSON fails because HMAC is byte-exact
        boolean isFormattedValid = validator.isValid(GITHUB_SIGNATURE, formattedJson);
        assertFalse(isFormattedValid, "Pretty-printed JSON fails HMAC because whitespace/newlines alter the byte hash");
    }

    @Test
    @DisplayName("Should validate successfully against signature_validation.json when signature is calculated from its exact bytes")
    void testValidateWithResourceFile() throws Exception {
        String payload = readResource("signature_validation.json");
        String validSignature = calculateExpectedSignature(SECRET, payload);

        boolean isValid = validator.isValid(validSignature, payload);

        assertTrue(isValid, "Validator must return true when signature matches raw payload bytes from file");
    }

    @Test
    @DisplayName("RCA Test 1: Even a single whitespace difference causes signature mismatch")
    void testWhitespaceSensitivity() throws Exception {
        String rawWirePayload = "{\"action\":\"opened\"}";
        String prettyPrintedPayload = "{\n  \"action\": \"opened\"\n}";

        // GitHub signs the exact raw bytes transmitted on the wire
        String signatureOfRaw = calculateExpectedSignature(SECRET, rawWirePayload);

        // If controller or test validates against formatted JSON, it will fail
        boolean isValid = validator.isValid(signatureOfRaw, prettyPrintedPayload);

        assertFalse(isValid, "HMAC fails if payload formatting or whitespace differs from wire bytes");
    }

    @Test
    @DisplayName("RCA Test 2: Line ending differences (CRLF vs LF) cause signature mismatch")
    void testLineEndingSensitivity() throws Exception {
        String lfPayload = "{\n\"action\": \"edited\"\n}";
        String crlfPayload = "{\r\n\"action\": \"edited\"\r\n}";

        String signatureOfLf = calculateExpectedSignature(SECRET, lfPayload);

        // Validating CRLF payload against LF signature fails
        assertFalse(validator.isValid(signatureOfLf, crlfPayload), 
                "CRLF payload must not match LF signature due to extra \\r byte");
    }

    @Test
    @DisplayName("RCA Test 3: Incorrect secret key causes signature mismatch")
    void testWrongSecret() throws Exception {
        String payload = "{\"action\":\"opened\",\"number\":9}";
        String wrongSecret = "different_secret_key";
        String signatureWithWrongSecret = calculateExpectedSignature(wrongSecret, payload);

        boolean isValid = validator.isValid(signatureWithWrongSecret, payload);

        assertFalse(isValid, "Validator must reject signature generated with a different secret");
    }

    @Test
    @DisplayName("Should return false when signature header is missing or malformed")
    void testMalformedSignatureHeader() {
        String payload = "{\"action\":\"opened\"}";

        assertFalse(validator.isValid(null, payload), "Null header must return false");
        assertFalse(validator.isValid("", payload), "Empty header must return false");
        assertFalse(validator.isValid("invalid_header", payload), "Header without sha256= prefix must return false");
        assertFalse(validator.isValid("sha1=abcdef", payload), "sha1 prefix must return false");
    }

    @Test
    @DisplayName("Should return false when webhook secret is null or blank")
    void testMissingConfiguredSecret() {
        SignatureValidator unconfiguredValidator = new SignatureValidator("");
        ReflectionTestUtils.setField(unconfiguredValidator, "isSignatureValidation", true);

        String payload = "{\"action\":\"opened\"}";
        assertFalse(unconfiguredValidator.isValid("sha256=123456", payload), 
                "Must return false when secret is empty");
    }

    @Test
    @DisplayName("Should skip validation and return true when validation is disabled")
    void testValidationDisabled() {
        ReflectionTestUtils.setField(validator, "isSignatureValidation", false);

        String payload = "{\"action\":\"opened\"}";
        assertTrue(validator.isValid("invalid_signature", payload), 
                "Must return true when signature validation is disabled");
    }
}
