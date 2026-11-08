package pe.techsupport.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;

import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.Test;

import pe.techsupport.auth.config.JwtProperties;
import pe.techsupport.auth.service.CredentialSnapshot;
import pe.techsupport.auth.service.JwtService;

class JwtServiceTest {

    private static final String SECRET = "z/WUV74XyHKHBbIUa+5OgGZc9f8Adbhc0wqzhVtlw3yEdNFk8arMsKFonx08TjFL";

    private JwtService service(String secret) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setAccessTtl(Map.of("ADMIN", Duration.ofMinutes(15), "CLIENTE", Duration.ofMinutes(30)));
        return new JwtService(props);
    }

    @Test
    void issuesTokenWithRoleBasedExpirationAndClaims() {
        JwtService jwt = service(SECRET);
        var admin = new CredentialSnapshot("USR-A0001", "admin@techsupport.pe", "x", "ADMIN", "Admin", "ACTIVO");
        var client = new CredentialSnapshot("USR-C0001", "c@techsupport.pe", "x", "CLIENTE", "Cliente", "ACTIVO");

        var adminToken = jwt.issueAccessToken(admin);
        var clientToken = jwt.issueAccessToken(client);

        assertThat(adminToken.expiresInSeconds()).isEqualTo(900);
        assertThat(clientToken.expiresInSeconds()).isEqualTo(1800);
        var claims = jwt.parse(adminToken.token());
        assertThat(claims.getSubject()).isEqualTo("admin@techsupport.pe");
        assertThat(claims.get("userId", String.class)).isEqualTo("USR-A0001");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getId()).isEqualTo(adminToken.jti());
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String otherSecret = "Wm9vb29vb29vb29vb29vb29vb29vb29vb29vb29vb29vb29vb29vb29vb29vb28=";
        var token = service(otherSecret).issueAccessToken(
                new CredentialSnapshot("USR-C0001", "c@x.pe", "x", "CLIENTE", "C", "ACTIVO"));
        assertThatThrownBy(() -> service(SECRET).parse(token.token())).isInstanceOf(JwtException.class);
    }
}
