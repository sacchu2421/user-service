package com.userservice.user.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // Extract roles from JWT claims
        Map<String, Object> claims = jwt.getClaims();
        Object realmAccess = claims.get("realm_access");
        
        if (realmAccess instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> realmAccessMap = (Map<String, Object>) realmAccess;
            Object roles = realmAccessMap.get("roles");
            
            if (roles instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> roleList = (List<String>) roles;
                return roleList.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
            }
        }
        
        // Fallback to standard claim
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
