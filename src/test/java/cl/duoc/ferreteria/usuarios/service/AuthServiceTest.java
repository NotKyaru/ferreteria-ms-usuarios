package cl.duoc.ferreteria.usuarios.service;

import cl.duoc.ferreteria.usuarios.dto.AutenticacionRespuesta;
import cl.duoc.ferreteria.usuarios.dto.LoginEntrada;
import cl.duoc.ferreteria.usuarios.dto.RefreshEntrada;
import cl.duoc.ferreteria.usuarios.dto.UsuarioRegistro;
import cl.duoc.ferreteria.usuarios.exception.NoAutorizadoException;
import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.model.Usuario;
import cl.duoc.ferreteria.usuarios.repository.UsuarioRepository;
import cl.duoc.ferreteria.usuarios.security.JwtGenerador;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioService usuarioService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtGenerador jwtGenerador;

    @InjectMocks AuthService authService;

    private Usuario camila(boolean activo) {
        Usuario u = new Usuario();
        u.setId(3L);
        u.setNombre("Camila");
        u.setApellidos("Rojas Díaz");
        u.setCorreo("cliente@ferreteria.cl");
        u.setPassword("hash-bcrypt");
        u.setRol(Rol.CLIENTE);
        u.setActivo(activo);
        return u;
    }

    private void tokensDePrueba(Usuario u) {
        when(jwtGenerador.generarAccessToken(u)).thenReturn("access-nuevo");
        when(jwtGenerador.generarRefreshToken(u)).thenReturn("refresh-nuevo");
        when(jwtGenerador.segundosAccess()).thenReturn(900L);
    }

    @Test
    void loginCorrectoDevuelveElParDeTokens() {
        Usuario camila = camila(true);
        when(usuarioRepository.findByCorreo("cliente@ferreteria.cl")).thenReturn(Optional.of(camila));
        when(passwordEncoder.matches("Maestros2026", "hash-bcrypt")).thenReturn(true);
        tokensDePrueba(camila);

        // El correo llega con mayúsculas y espacios: se normaliza antes de buscar
        AutenticacionRespuesta r = authService.login(new LoginEntrada(" Cliente@Ferreteria.CL ", "Maestros2026"));

        assertEquals("access-nuevo", r.accessToken());
        assertEquals("refresh-nuevo", r.refreshToken());
        assertEquals("Bearer", r.tipo());
        assertEquals(900, r.expiraEn());
        assertEquals(3L, r.usuario().id());
    }

    @Test
    void correoInexistenteYContrasenaMalaDanElMismoMensaje() {
        when(usuarioRepository.findByCorreo("nadie@ferreteria.cl")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("cliente@ferreteria.cl")).thenReturn(Optional.of(camila(true)));
        when(passwordEncoder.matches("incorrecta1", "hash-bcrypt")).thenReturn(false);

        var sinCuenta = assertThrows(NoAutorizadoException.class,
                () -> authService.login(new LoginEntrada("nadie@ferreteria.cl", "Maestros2026")));
        var claveMala = assertThrows(NoAutorizadoException.class,
                () -> authService.login(new LoginEntrada("cliente@ferreteria.cl", "incorrecta1")));

        assertEquals("Correo o contraseña incorrectos", sinCuenta.getMessage());
        assertEquals(sinCuenta.getMessage(), claveMala.getMessage());
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() {
        when(usuarioRepository.findByCorreo("cliente@ferreteria.cl")).thenReturn(Optional.of(camila(false)));
        when(passwordEncoder.matches("Maestros2026", "hash-bcrypt")).thenReturn(true);

        var error = assertThrows(NoAutorizadoException.class,
                () -> authService.login(new LoginEntrada("cliente@ferreteria.cl", "Maestros2026")));
        assertEquals("Correo o contraseña incorrectos", error.getMessage());
    }

    @Test
    void registroCreaElClienteYDevuelveTokens() {
        Usuario camila = camila(true);
        UsuarioRegistro entrada = new UsuarioRegistro("33333333-3", "Camila", "Rojas Díaz",
                "cliente@ferreteria.cl", "Maestros2026", null, null, null, null);
        when(usuarioService.crearCliente(entrada)).thenReturn(camila);
        tokensDePrueba(camila);

        AutenticacionRespuesta r = authService.registrar(entrada);

        assertEquals("access-nuevo", r.accessToken());
        assertEquals(Rol.CLIENTE, r.usuario().rol());
    }

    @Test
    void refreshValidoDevuelveUnParNuevo() {
        Usuario camila = camila(true);
        when(jwtGenerador.validarRefreshToken("refresh-viejo")).thenReturn(Jwts.claims().subject("3").build());
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(camila));
        tokensDePrueba(camila);

        AutenticacionRespuesta r = authService.refrescar(new RefreshEntrada("refresh-viejo"));

        assertEquals("access-nuevo", r.accessToken());
        assertEquals("refresh-nuevo", r.refreshToken());
    }

    @Test
    void refreshInvalidoResponde401() {
        when(jwtGenerador.validarRefreshToken("basura")).thenThrow(new JwtException("firma inválida"));

        var error = assertThrows(NoAutorizadoException.class,
                () -> authService.refrescar(new RefreshEntrada("basura")));
        assertEquals("Sesión expirada, inicia sesión nuevamente", error.getMessage());
    }

    @Test
    void refreshDeUsuarioInactivoResponde401() {
        when(jwtGenerador.validarRefreshToken("refresh-viejo")).thenReturn(Jwts.claims().subject("3").build());
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(camila(false)));

        assertThrows(NoAutorizadoException.class, () -> authService.refrescar(new RefreshEntrada("refresh-viejo")));
    }
}
