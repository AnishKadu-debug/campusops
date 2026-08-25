package com.campusops.incident.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CampusOpsJwtAuthoritiesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, extractAuthorities(jwt));
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object claim = jwt.getClaim(ROLES_CLAIM);
        List<String> roles = new ArrayList<>();
        if (claim instanceof Collection<?> collection) {
            collection.forEach(role -> roles.add(String.valueOf(role)));
        } else if (claim instanceof String text && !text.isBlank()) {
            for (String role : text.split("[,\\s]+")) {
                if (!role.isBlank()) {
                    roles.add(role);
                }
            }
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(ROLE_PREFIX + role.trim()))
                .toList();
    }
}
