package fingenie.com.fingenie.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PaymentSignatureUtils {

    private PaymentSignatureUtils() {
    }

    public static String hmacSha256Hex(String data, String secret) {
        return hmacHex(data, secret, "HmacSHA256");
    }

    public static String hmacSha512Hex(String data, String secret) {
        return hmacHex(data, secret, "HmacSHA512");
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash payload", ex);
        }
    }

    public static String buildCanonicalString(
            Map<String, String> input,
            boolean encodeKeys,
            boolean encodeValues
    ) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(input.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue();
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(encodeKeys ? urlEncode(key) : key);
            builder.append('=');
            builder.append(encodeValues ? urlEncode(value) : value);
        }
        return builder.toString();
    }

    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String hmacHex(String data, String secret, String algorithm) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(keySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign payload", ex);
        }
    }

    private static String toHex(byte[] input) {
        StringBuilder sb = new StringBuilder(input.length * 2);
        for (byte b : input) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
