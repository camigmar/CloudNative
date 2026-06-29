package cl.duoc.gestionguias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // H2 console sin autenticacion (solo desarrollo)
                .requestMatchers("/h2-console/**").permitAll()

                // Solo rol DESCARGA puede descargar guias
                .requestMatchers(HttpMethod.GET, "/guias/*/descargar")
                    .hasAuthority("ROLE_descarga")

                // Rol GESTION puede usar el resto de endpoints
                .requestMatchers(HttpMethod.POST, "/guias").hasAuthority("ROLE_gestion")
                .requestMatchers(HttpMethod.POST, "/guias/*/subir").hasAuthority("ROLE_gestion")
                .requestMatchers(HttpMethod.PUT, "/guias/*").hasAuthority("ROLE_gestion")
                .requestMatchers(HttpMethod.DELETE, "/guias/*").hasAuthority("ROLE_gestion")
                .requestMatchers(HttpMethod.GET, "/guias/**").hasAuthority("ROLE_gestion")

                // Cualquier otra peticion requiere autenticacion
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(issuerValidator);
        decoder.setJwtValidator(validator);

        return decoder;
    }

    /**
     * Convierte el claim "extension_role" del token JWT de Azure AD B2C
     * en un GrantedAuthority con prefijo ROLE_
     * Ej: "gestion" -> ROLE_gestion, "descarga" -> ROLE_descarga
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();
        // El claim personalizado que creamos en Azure AD B2C
        rolesConverter.setAuthoritiesClaimName("extension_role");
        rolesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesConverter);
        return converter;
    }
}
