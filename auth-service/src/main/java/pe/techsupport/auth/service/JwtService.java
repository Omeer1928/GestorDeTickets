package pe.techsupport.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import pe.techsupport.auth.config.JwtProperties;

/**
 * Emisión y validación de JWT firmados con HMAC-SHA256. Claims: sub (email), userId, role, fullName,
 * jti (para la lista negra en logout), iat y exp.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.getSecret()));
    }

    public IssuedToken issueAccessToken(CredentialSnapshot credential) {
        Duration ttl = props.accessTtlFor(credential.role());
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .issuer(props.getIssuer())
                .subject(credential.email())
                .claim("userId", credential.userId())
                .claim("role", credential.role())
                .claim("fullName", credential.fullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, jti, ttl.toSeconds());
    }

    /** @throws JwtException si la firma, el emisor o la expiración no son válidos */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record IssuedToken(String token, String jti, long expiresInSeconds) {
    }
}
