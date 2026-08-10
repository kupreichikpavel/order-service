package by.innowise.orderservice.config;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
      JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable).sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
        authorize -> authorize
            .requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
            )
            .permitAll()
            .requestMatchers(
                "/api/v1/orders",
                "/api/v1/orders/**"
            )
            .hasAnyRole("USER", "ADMIN")
            .anyRequest()
            .authenticated()
    ).oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(this::extractRealmRoles);

    return converter;
  }

  private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

    if (realmAccess == null) {
      return List.of();
    }

    Object rolesValue = realmAccess.get("roles");

    if (!(rolesValue instanceof Collection<?> roles)) {
      return List.of();
    }

    return roles.stream().filter(String.class::isInstance).map(String.class::cast).map(String::trim)
        .filter(role -> !role.isBlank()).<GrantedAuthority>map(
            role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT))).toList();
  }
}
