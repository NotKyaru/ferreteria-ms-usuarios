package cl.duoc.ferreteria.usuarios.security;

import cl.duoc.ferreteria.usuarios.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Firma los tokens. Solo existe en ms-usuarios; los otros servicios solo los validan con JwtUtil. */
@Component
public class JwtGenerador {

    private final JwtUtil jwtUtil;
    private final long minutosAccess;
    private final long diasRefresh;

    public JwtGenerador(JwtUtil jwtUtil,
                        @Value("${jwt.access-minutos:15}") long minutosAccess,
                        @Value("${jwt.refresh-dias:7}") long diasRefresh) {
        this.jwtUtil = jwtUtil;
        this.minutosAccess = minutosAccess;
        this.diasRefresh = diasRefresh;
    }

    public String generarAccessToken(Usuario u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(JwtUtil.EMISOR)
                .subject(String.valueOf(u.getId()))
                .claim("correo", u.getCorreo())
                .claim("nombre", u.getNombreCompleto())
                .claim("rol", u.getRol().name())
                .claim("tipo", "access")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutosAccess, ChronoUnit.MINUTES)))
                .signWith(jwtUtil.getClave(), Jwts.SIG.HS256)
                .compact();
    }

    // El refresh solo lleva el id: al renovar, los datos y el rol se vuelven a leer de la base
    public String generarRefreshToken(Usuario u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(JwtUtil.EMISOR)
                .subject(String.valueOf(u.getId()))
                .claim("tipo", "refresh")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(diasRefresh, ChronoUnit.DAYS)))
                .signWith(jwtUtil.getClave(), Jwts.SIG.HS256)
                .compact();
    }

    /** Igual a validarAccessToken pero exige tipo "refresh". Lanza JwtException si no sirve. */
    public Claims validarRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtUtil.getClave())
                .requireIssuer(JwtUtil.EMISOR)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"refresh".equals(claims.get("tipo", String.class))) {
            throw new JwtException("El token no es de renovación");
        }
        return claims;
    }

    /** "expiraEn" de la respuesta de login, en segundos. */
    public long segundosAccess() {
        return minutosAccess * 60;
    }
}
