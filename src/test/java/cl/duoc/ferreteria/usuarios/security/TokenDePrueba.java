package cl.duoc.ferreteria.usuarios.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Tokens firmados con el secreto de prueba de src/test/resources (contrato C6). Solo para tests. */
public final class TokenDePrueba {

    private static final SecretKey CLAVE = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
            "Y2xhdmUtZGUtcHJ1ZWJhLXNvbG8tcGFyYS10ZXN0cy1uby1lcy1sYS1yZWFs"));

    private TokenDePrueba() { }

    public static String access(long id, String rol) { return crear(id, rol, "access", 15); }
    public static String expirado(String rol)         { return crear(1, rol, "access", -5); }
    public static String refresh()                    { return crear(1, "CLIENTE", "refresh", 60); }

    private static String crear(long id, String rol, String tipo, long minutos) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer("ferreteria-ms-usuarios").subject(String.valueOf(id))
                .claim("correo", "prueba@ferreteria.cl").claim("nombre", "Usuario Prueba")
                .claim("rol", rol).claim("tipo", tipo)
                .issuedAt(Date.from(ahora.minus(10, ChronoUnit.MINUTES)))
                .expiration(Date.from(ahora.plus(minutos, ChronoUnit.MINUTES)))
                .signWith(CLAVE, Jwts.SIG.HS256).compact();
    }
}
