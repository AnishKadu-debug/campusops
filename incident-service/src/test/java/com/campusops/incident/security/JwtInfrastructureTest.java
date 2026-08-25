package com.campusops.incident.security;

import com.campusops.incident.security.dev.DevTokenGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtInfrastructureTest {

    private static final String SECRET = DevTokenGenerator.DEV_SECRET;

    private JwtDecoder decoder() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Test
    @DisplayName("minted dev token should validate and carry subject + roles claim")
    void mintedToken_shouldRoundTripThroughDecoder() {
        String token = DevTokenGenerator.mint(SECRET, "student-123", "STUDENT");

        Jwt jwt = decoder().decode(token);

        assertThat(jwt.getSubject()).isEqualTo("student-123");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("STUDENT");
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("token signed with a different secret should be rejected")
    void tokenSignedWithDifferentSecret_shouldBeRejected() {
        String otherSecret = "another-secret-value-that-is-long-enough-123456";
        String token = DevTokenGenerator.mint(otherSecret, "student-123", "STUDENT");

        assertThatThrownBy(() -> decoder().decode(token))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    @DisplayName("expired token should be rejected")
    void expiredToken_shouldBeRejected() {
        String token = DevTokenGenerator.mint(
                SECRET, "student-123", "STUDENT", java.time.Duration.ofSeconds(-60));

        assertThatThrownBy(() -> decoder().decode(token))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    @DisplayName("authorities converter should map roles claim list to ROLE_ authorities")
    void converter_shouldMapRolesListToAuthorities() {
        CampusOpsJwtAuthoritiesConverter converter = new CampusOpsJwtAuthoritiesConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("student-123")
                .claim("roles", List.of("STUDENT"))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
        assertThat(authentication.getName()).isEqualTo("student-123");
    }

    @Test
    @DisplayName("authorities converter should tolerate delimited string claims")
    void converter_shouldTolerateDelimitedStringClaims() {
        CampusOpsJwtAuthoritiesConverter converter = new CampusOpsJwtAuthoritiesConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("manager-1")
                .claim("roles", "MANAGER, TECHNICIAN")
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_MANAGER", "ROLE_TECHNICIAN");
    }

    @Test
    @DisplayName("authorities converter with missing claim should produce no authorities")
    void converter_missingClaim_shouldProduceNoAuthorities() {
        CampusOpsJwtAuthoritiesConverter converter = new CampusOpsJwtAuthoritiesConverter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("student-123")
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).isEmpty();
    }
}

