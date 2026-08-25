package com.campusops.incident.security;

import com.campusops.incident.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    private static final String ROLE_PREFIX = "ROLE_";

    public CurrentUser resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Authenticated user required");
        }
        UserRole role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> UserRole.valueOf(authority.substring(ROLE_PREFIX.length())))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("No role assigned to caller"));
        return new CurrentUser(authentication.getName(), role);
    }
}
