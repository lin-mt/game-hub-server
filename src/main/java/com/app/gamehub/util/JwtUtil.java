package com.app.gamehub.util;

import com.app.gamehub.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private final JwtConfig jwtConfig;

  public JwtUtil(JwtConfig jwtConfig) {
    this.jwtConfig = jwtConfig;
  }

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
  }

  public String generateToken(Long userId, String openid) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

    return Jwts.builder()
        .subject(userId.toString())
        .claim("openid", openid)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  public Long getUserIdFromToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

    return Long.parseLong(claims.getSubject());
  }

  public String getOpenidFromToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

    return claims.get("openid", String.class);
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public boolean isTokenExpired(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

      return claims.getExpiration().before(new Date());
    } catch (JwtException | IllegalArgumentException e) {
      return true;
    }
  }
}
