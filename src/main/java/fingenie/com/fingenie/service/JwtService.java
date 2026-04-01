package fingenie.com.fingenie.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public Long extractAccountId(String token) {
        return extractClaim(token, claims -> claims.get("accountId", Long.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, null);
    }

    public String generateToken(UserDetails userDetails, Long accountId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        if (accountId != null) {
            claims.put("accountId", accountId);
        }
        return buildToken(claims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateRefreshToken(userDetails, null);
    }

    public String generateRefreshToken(UserDetails userDetails, Long accountId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        if (accountId != null) {
            claims.put("accountId", accountId);
        }
        return buildToken(claims, userDetails, refreshExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setId(UUID.randomUUID().toString())
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = decodeSecretKey(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decodeSecretKey(String configuredSecretKey) {
        if (configuredSecretKey == null || configuredSecretKey.isBlank()) {
            throw new IllegalStateException("jwt.secret must not be empty");
        }

        String normalized = configuredSecretKey.trim();

        try {
            return Decoders.BASE64URL.decode(normalized);
        } catch (DecodingException ignored) {
            // Fallback below for non-URL-safe secrets.
        }

        try {
            return Decoders.BASE64.decode(normalized);
        } catch (DecodingException ignored) {
            // Final fallback for plain text secrets in local/dev environments.
            return normalized.getBytes(StandardCharsets.UTF_8);
        }
    }
}
