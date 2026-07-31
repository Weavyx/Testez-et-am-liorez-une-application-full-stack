package com.openclassrooms.starterjwt.security.jwt;

import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    // Meme secret de test que src/test/resources/application-test.yml (Base64, >= 64 octets decodes pour HS512)
    private static final String TEST_SECRET =
            "YYexzxisawYoUVKeTed4fwqYEMpXAdVpRcBvM3uNGSfXe0FWQxGKJUmqvpPnooe+1lphKR8TRuGK3DFszHMzCQ==";
    private static final int TEST_EXPIRATION_MS = 86_400_000;

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", TEST_EXPIRATION_MS);
    }

    private Authentication authenticationFor(String username) {
        UserDetailsImpl userDetails = UserDetailsImpl.builder()
                .id(1L)
                .username(username)
                .firstName("Jean")
                .lastName("Dupont")
                .admin(false)
                .password("encodedPassword")
                .build();
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void should_generateValidDecodableToken_when_generateJwtTokenIsCalled() {
        Authentication authentication = authenticationFor("yoga@studio.com");

        String token = jwtUtils.generateJwtToken(authentication);

        assertThat(token).isNotBlank();
        Claims claims = Jwts.parser().setSigningKey(TEST_SECRET).build().parseClaimsJws(token).getBody();
        assertThat(claims.getSubject()).isEqualTo("yoga@studio.com");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void should_extractSubject_when_getUserNameFromJwtTokenIsCalled() {
        String token = jwtUtils.generateJwtToken(authenticationFor("yoga@studio.com"));

        String username = jwtUtils.getUserNameFromJwtToken(token);

        assertThat(username).isEqualTo("yoga@studio.com");
    }

    @Test
    void should_returnTrue_when_validateJwtTokenIsCalled_with_validToken() {
        String token = jwtUtils.generateJwtToken(authenticationFor("yoga@studio.com"));

        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
    }

    @Test
    void should_returnFalse_when_validateJwtTokenIsCalled_with_invalidSignature() {
        String otherSecret = Base64.getEncoder().encodeToString(new byte[64]);
        String token = Jwts.builder()
                .setSubject("yoga@studio.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SignatureAlgorithm.HS512, otherSecret)
                .compact();

        assertThat(jwtUtils.validateJwtToken(token)).isFalse();
    }

    @Test
    void should_returnFalse_when_validateJwtTokenIsCalled_with_malformedToken() {
        assertThat(jwtUtils.validateJwtToken("this-is-not-a-valid-jwt-token")).isFalse();
    }

    @Test
    void should_returnFalse_when_validateJwtTokenIsCalled_with_expiredToken() {
        String token = Jwts.builder()
                .setSubject("yoga@studio.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 2_000))
                .setExpiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(SignatureAlgorithm.HS512, TEST_SECRET)
                .compact();

        assertThat(jwtUtils.validateJwtToken(token)).isFalse();
    }

    @Test
    void should_returnFalse_when_validateJwtTokenIsCalled_with_unsupportedToken() {
        String token = Jwts.builder()
                .setSubject("yoga@studio.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .compact();

        assertThat(jwtUtils.validateJwtToken(token)).isFalse();
    }

    @Test
    void should_returnFalse_when_validateJwtTokenIsCalled_with_nullToken() {
        assertThat(jwtUtils.validateJwtToken(null)).isFalse();
    }

    @Test
    void should_returnFalse_when_validateJwtTokenIsCalled_with_emptyToken() {
        assertThat(jwtUtils.validateJwtToken("")).isFalse();
    }
}
