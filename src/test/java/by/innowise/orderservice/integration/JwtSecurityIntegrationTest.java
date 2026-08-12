package by.innowise.orderservice.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import by.innowise.orderservice.config.SecurityConfig;
import by.innowise.orderservice.controller.OrderController;
import by.innowise.orderservice.handler.GlobalExceptionHandler;
import by.innowise.orderservice.service.OrderService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class,
    JwtSecurityIntegrationTest.JwtDecoderTestConfiguration.class})
class JwtSecurityIntegrationTest {

  private static final long USER_ID = 7L;

  private static final String KEY_ID = "order-test-key";
  private static final String AUDIENCE = "order-service";
  private static final String JWKS_PATH = "/realms/shopper/protocol/openid-connect/certs";

  private static final RSAKey SIGNING_KEY = createRsaKey(KEY_ID);

  private static final WireMockServer WIREMOCK = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    WIREMOCK.start();

    JWKSet publicKeys = new JWKSet(SIGNING_KEY.toPublicJWK());

    WIREMOCK.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(JWKS_PATH)
        .willReturn(okJson(publicKeys.toString())));
  }

  @DynamicPropertySource
  static void registerJwtProperties(DynamicPropertyRegistry registry) {
    registry.add("test.jwt.issuer", JwtSecurityIntegrationTest::issuer);

    registry.add("test.jwt.jwk-set-uri", JwtSecurityIntegrationTest::jwkSetUri);
  }

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private OrderService orderService;

  @MockitoBean(name = "jpaMappingContext")
  private JpaMetamodelMappingContext jpaMappingContext;

  @Test
  void shouldAcceptValidSignedJwt() throws Exception {
    when(orderService.getAll(
        eq(USER_ID),
        eq(false),
        any(Pageable.class)
    )).thenReturn(Page.empty());

    String token = createToken(
        SIGNING_KEY,
        issuer(),
        AUDIENCE,
        USER_ID
    );

    mockMvc.perform(get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());

    verify(orderService).getAll(
        eq(USER_ID),
        eq(false),
        any(Pageable.class)
    );
  }

  @Test
  void shouldAllowAdminToListAllOrders() throws Exception {
    when(orderService.getAll(
        eq(USER_ID),
        eq(true),
        any(Pageable.class)
    ))
        .thenReturn(Page.empty());

    String token = createToken(
        SIGNING_KEY,
        issuer(),
        AUDIENCE,
        USER_ID,
        List.of("ADMIN")
    );

    mockMvc.perform(
            get("/api/v1/orders")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + token
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());

    verify(orderService).getAll(
        eq(USER_ID),
        eq(true),
        any(Pageable.class)
    );
  }

  @Test
  void shouldRejectJwtWithoutRequiredRole() throws Exception {
    String token = createToken(
        SIGNING_KEY,
        issuer(),
        AUDIENCE,
        USER_ID,
        List.of("OTHER")
    );

    mockMvc.perform(
            get("/api/v1/orders")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + token
                )
        )
        .andExpect(status().isForbidden());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldRejectJwtWithInvalidSignature() throws Exception {
    RSAKey unknownKey = createRsaKey("unknown-key");

    String token = createToken(
        unknownKey,
        issuer(),
        AUDIENCE,
        USER_ID
    );

    mockMvc.perform(get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldRejectJwtWithWrongIssuer() throws Exception {
    String token = createToken(
        SIGNING_KEY,
        "http://invalid-issuer",
        AUDIENCE,
        USER_ID
    );

    mockMvc.perform(get("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldRejectJwtWithWrongAudience()
      throws Exception {
    String token = createToken(
        SIGNING_KEY,
        issuer(),
        "user-service",
        USER_ID
    );

    mockMvc.perform(
            get("/api/v1/orders")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + token
                )
        )
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(orderService);
  }

  @AfterAll
  static void stopWireMock() {
    WIREMOCK.stop();
  }

  private static String createToken(
      RSAKey signingKey,
      String tokenIssuer,
      String tokenAudience,
      long userId
  ) {
    return createToken(
        signingKey,
        tokenIssuer,
        tokenAudience,
        userId,
        List.of("USER")
    );
  }

  private static String createToken(
      RSAKey signingKey,
      String tokenIssuer,
      String tokenAudience,
      long userId,
      List<String> roles
  ) {
    JWKSource<SecurityContext> keySource = new ImmutableJWKSet<>(new JWKSet(signingKey));

    JwtEncoder encoder = new NimbusJwtEncoder(keySource);

    Instant now = Instant.now();

    JwtClaimsSet claims = JwtClaimsSet.builder().issuer(tokenIssuer).subject("user-" + userId)
        .audience(List.of(tokenAudience)).issuedAt(now).expiresAt(now.plusSeconds(300))
        .claim("userId", Long.toString(userId))
        .claim("realm_access", Map.of("roles", roles)).build();

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(signingKey.getKeyID())
        .build();

    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private static RSAKey createRsaKey(String keyId) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

      generator.initialize(2048);

      KeyPair keyPair = generator.generateKeyPair();

      return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic()).privateKey(
          (RSAPrivateKey) keyPair.getPrivate()).algorithm(JWSAlgorithm.RS256).keyID(keyId).build();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create RSA test key", exception);
    }
  }

  private static String issuer() {
    return WIREMOCK.baseUrl() + "/realms/shopper";
  }

  private static String jwkSetUri() {
    return WIREMOCK.baseUrl() + JWKS_PATH;
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class JwtDecoderTestConfiguration {

    @Bean
    JwtDecoder jwtDecoder(@Value("${test.jwt.jwk-set-uri}") String jwkSetUri,
        @Value("${test.jwt.issuer}") String issuer) {
      NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

      decoder.setJwtValidator(
          new DelegatingOAuth2TokenValidator<>(
              JwtValidators.createDefaultWithIssuer(
                  issuer
              ),
              new JwtAudienceValidator(AUDIENCE)
          )
      );

      return decoder;
    }
  }
}
