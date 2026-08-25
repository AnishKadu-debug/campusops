package com.campusops.incident.security.dev;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * DEV/TEST ONLY utility: mints signed demo JWTs for local development and runtime demos.
 * This is NOT a production login system - no auth server exists in CampusOps v1.
 */
public final class DevTokenGenerator {

    /** Same dev-only secret documented in application.properties. Never a production secret. */
    public static final String DEV_SECRET = "campusops-dev-jwt-secret-change-me-0123456789";

    private static final Duration DEFAULT_TTL = Duration.ofHours(8);

    private DevTokenGenerator() {
    }

    public static String mint(String secret, String subject, String role) {
        return mint(secret, subject, role, DEFAULT_TTL);
    }

    public static String mint(String secret, String subject, String role, Duration ttl) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .subject(subject)
                    .claim("roles", List.of(role))
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(ttl)))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(new MACSigner(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to mint dev token", e);
        }
    }

    /**
     * Usage: DevTokenGenerator <STUDENT|TECHNICIAN|MANAGER> [subject]
     * Secret resolution: env CAMPUSOPS_SECURITY_JWT_SECRET, else the documented dev default.
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: DevTokenGenerator <STUDENT|TECHNICIAN|MANAGER> [subject]");
            System.exit(1);
        }
        String role = args[0].toUpperCase();
        String subject = args.length == 2 ? args[1] : role.toLowerCase() + "-demo";
        String secret = System.getenv().getOrDefault(
                "CAMPUSOPS_SECURITY_JWT_SECRET", DEV_SECRET);

        String token = mint(secret, subject, role);
        System.out.println(token);
    }
}
