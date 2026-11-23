package cl.duoc.ferreteria.usuarios.security;

import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test unitario sin Spring: se arman las clases a mano con el secreto de prueba
class JwtGeneradorTest {

    private static final String SECRETO = "Y2xhdmUtZGUtcHJ1ZWJhLXNvbG8tcGFyYS10ZXN0cy1uby1lcy1sYS1yZWFs";

    private final JwtUtil jwtUtil = new JwtUtil(SECRETO);
    private final JwtGenerador generador = new JwtGenerador(jwtUtil, 15, 7);

    private Usuario camila() {
        Usuario u = new Usuario();
        u.setId(3L);
        u.setNombre("Camila");
        u.setApellidos("Rojas Díaz");
        u.setCorreo("cliente@ferreteria.cl");
        u.setRol(Rol.CLIENTE);
        return u;
    }

    @Test
    void elAccessTokenTraeLosClaimsDelContrato() {
        Claims claims = jwtUtil.validarAccessToken(generador.generarAccessToken(camila()));

        assertEquals("ferreteria-ms-usuarios", claims.getIssuer());
        assertEquals("3", claims.getSubject());
        assertEquals("cliente@ferreteria.cl", claims.get("correo", String.class));
        assertEquals("Camila Rojas Díaz", claims.get("nombre", String.class));
        assertEquals("CLIENTE", claims.get("rol", String.class));
        assertEquals("access", claims.get("tipo", String.class));
    }

    @Test
    void elRefreshNoSirveComoAccess() {
        String refresh = generador.generarRefreshToken(camila());
        assertThrows(JwtException.class, () -> jwtUtil.validarAccessToken(refresh));
    }

    @Test
    void elAccessNoSirveComoRefresh() {
        String access = generador.generarAccessToken(camila());
        assertThrows(JwtException.class, () -> generador.validarRefreshToken(access));
    }

    @Test
    void elRefreshSeValidaYTraeSoloElId() {
        Claims claims = generador.validarRefreshToken(generador.generarRefreshToken(camila()));
        assertEquals("3", claims.getSubject());
        assertEquals(null, claims.get("rol"));
    }

    @Test
    void unAccessVencidoSeRechaza() {
        JwtGenerador vencido = new JwtGenerador(jwtUtil, -1, 7);
        String token = vencido.generarAccessToken(camila());
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.validarAccessToken(token));
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        JwtUtil otroServicio = new JwtUtil("b3RyYS1jbGF2ZS1kaXN0aW50YS1kZS0zMi1ieXRlcy1vLW1hcw==");
        String token = generador.generarAccessToken(camila());
        assertThrows(JwtException.class, () -> otroServicio.validarAccessToken(token));
    }

    @Test
    void expiraEnSeDaEnSegundos() {
        assertEquals(900, generador.segundosAccess());
    }
}
