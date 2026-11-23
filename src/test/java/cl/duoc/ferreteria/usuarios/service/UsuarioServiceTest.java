package cl.duoc.ferreteria.usuarios.service;

import cl.duoc.ferreteria.usuarios.dto.UsuarioAdmin;
import cl.duoc.ferreteria.usuarios.dto.UsuarioRegistro;
import cl.duoc.ferreteria.usuarios.exception.ConflictoException;
import cl.duoc.ferreteria.usuarios.exception.NoAutorizadoException;
import cl.duoc.ferreteria.usuarios.exception.NoEncontradoException;
import cl.duoc.ferreteria.usuarios.exception.SolicitudInvalidaException;
import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.model.Usuario;
import cl.duoc.ferreteria.usuarios.repository.UsuarioRepository;
import cl.duoc.ferreteria.usuarios.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UsuarioService usuarioService;

    private UsuarioRegistro registro(String rut, String correo, String password) {
        return new UsuarioRegistro(rut, " Camila ", "Rojas Díaz", correo, password, "", null, null, null);
    }

    @Test
    void crearClienteNormalizaYGuardaLaContrasenaHasheada() {
        when(passwordEncoder.encode("Maestros2026")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario u = usuarioService.crearCliente(registro("33.333.333-3", "Cliente@Ferreteria.CL", "Maestros2026"));

        assertEquals("33333333-3", u.getRut());
        assertEquals("cliente@ferreteria.cl", u.getCorreo());
        assertEquals("Camila", u.getNombre());
        assertEquals("hash-bcrypt", u.getPassword());
        assertEquals(Rol.CLIENTE, u.getRol());
        assertNull(u.getTelefono());          // "" se guarda como null
        verify(passwordEncoder).encode("Maestros2026");
    }

    @Test
    void correoDuplicadoResponde409() {
        when(usuarioRepository.existsByCorreo("cliente@ferreteria.cl")).thenReturn(true);

        var error = assertThrows(ConflictoException.class,
                () -> usuarioService.crearCliente(registro("33333333-3", "cliente@ferreteria.cl", "Maestros2026")));
        assertEquals("Ya existe una cuenta con ese correo", error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rutDuplicadoResponde409() {
        when(usuarioRepository.existsByCorreo("nueva@ferreteria.cl")).thenReturn(false);
        when(usuarioRepository.existsByRut("33333333-3")).thenReturn(true);

        var error = assertThrows(ConflictoException.class,
                () -> usuarioService.crearCliente(registro("33333333-3", "nueva@ferreteria.cl", "Maestros2026")));
        assertEquals("Ya existe una cuenta con ese RUT", error.getMessage());
    }

    @Test
    void rutInvalidoResponde400SinConsultarLaBase() {
        var error = assertThrows(SolicitudInvalidaException.class,
                () -> usuarioService.crearCliente(registro("12345678-9", "nueva@ferreteria.cl", "Maestros2026")));
        assertEquals(Map.of("rut", "RUT inválido"), error.getDetalles());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void contrasenaSinNumeroResponde400() {
        var error = assertThrows(SolicitudInvalidaException.class,
                () -> usuarioService.crearCliente(registro("33333333-3", "nueva@ferreteria.cl", "SoloLetras")));
        assertEquals("password", error.getDetalles().keySet().iterator().next());
    }

    @Test
    void contrasenaCortaResponde400() {
        assertThrows(SolicitudInvalidaException.class,
                () -> usuarioService.crearCliente(registro("33333333-3", "nueva@ferreteria.cl", "abc123")));
    }

    // ── Administración (paso 6) ─────────────────────────────

    private static final UsuarioAutenticado ADMIN_SESION = new UsuarioAutenticado(1L, "admin@ferreteria.cl", "Alex Muñoz Herrera", "ADMINISTRADOR");

    private Usuario existente(long id, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRut("22222222-2");
        u.setCorreo("vendedor@ferreteria.cl");
        u.setPassword("hash-viejo");
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    private UsuarioAdmin edicion(Rol rol, Boolean activo, String password) {
        return new UsuarioAdmin("22222222-2", "Valentina", "Soto Pizarro", "vendedor@ferreteria.cl",
                password, null, null, null, null, rol, activo);
    }

    @Test
    void crearSinContrasenaResponde400() {
        assertThrows(SolicitudInvalidaException.class, () -> usuarioService.crear(edicion(Rol.VENDEDOR, true, null)));
    }

    @Test
    void obtenerUnIdInexistenteResponde404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        var error = assertThrows(NoEncontradoException.class, () -> usuarioService.obtener(99L));
        assertEquals("No existe el usuario 99", error.getMessage());
    }

    @Test
    void obtenerActualDeUnUsuarioDesactivadoResponde401() {
        Usuario desactivado = existente(1L, Rol.ADMINISTRADOR);
        desactivado.setActivo(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(desactivado));

        assertThrows(NoAutorizadoException.class, () -> usuarioService.obtenerActual(ADMIN_SESION));
    }

    @Test
    void editarConContrasenaVaciaNoLaCambia() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(existente(2L, Rol.VENDEDOR)));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario u = usuarioService.actualizar(2L, edicion(Rol.VENDEDOR, true, ""), ADMIN_SESION);

        assertEquals("hash-viejo", u.getPassword());
        assertEquals("Valentina", u.getNombre());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void editarConElCorreoDeOtroUsuarioResponde409() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(existente(2L, Rol.VENDEDOR)));
        when(usuarioRepository.existsByCorreoAndIdNot("vendedor@ferreteria.cl", 2L)).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> usuarioService.actualizar(2L, edicion(Rol.VENDEDOR, true, ""), ADMIN_SESION));
    }

    @Test
    void elAdminNoPuedeQuitarseSuPropioRol() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente(1L, Rol.ADMINISTRADOR)));

        var error = assertThrows(ConflictoException.class,
                () -> usuarioService.actualizar(1L, edicion(Rol.CLIENTE, true, ""), ADMIN_SESION));
        assertEquals("No puedes quitarte el rol de administrador", error.getMessage());
    }

    @Test
    void elAdminNoPuedeEliminarseASiMismo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente(1L, Rol.ADMINISTRADOR)));

        var error = assertThrows(ConflictoException.class, () -> usuarioService.eliminar(1L, ADMIN_SESION));
        assertEquals("No puedes eliminar tu propia cuenta", error.getMessage());
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void noSePuedeDegradarAlUltimoAdministrador() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente(5L, Rol.ADMINISTRADOR)));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(1L);

        var error = assertThrows(ConflictoException.class,
                () -> usuarioService.actualizar(5L, edicion(Rol.VENDEDOR, true, ""), ADMIN_SESION));
        assertEquals("Debe quedar al menos un administrador", error.getMessage());
    }

    @Test
    void sePuedeDegradarAUnAdministradorSiQuedanOtros() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente(5L, Rol.ADMINISTRADOR)));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(2L);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario u = usuarioService.actualizar(5L, edicion(Rol.VENDEDOR, true, ""), ADMIN_SESION);

        assertEquals(Rol.VENDEDOR, u.getRol());
    }

    @Test
    void noSePuedeDesactivarAlUltimoAdministrador() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente(5L, Rol.ADMINISTRADOR)));
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(1L);

        assertThrows(ConflictoException.class,
                () -> usuarioService.actualizar(5L, edicion(Rol.ADMINISTRADOR, false, ""), ADMIN_SESION));
    }

    @Test
    void eliminarUnUsuarioComunLoBorra() {
        Usuario vendedor = existente(2L, Rol.VENDEDOR);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(vendedor));

        usuarioService.eliminar(2L, ADMIN_SESION);

        verify(usuarioRepository).delete(vendedor);
    }
}
