package cl.duoc.ferreteria.usuarios.service;

import cl.duoc.ferreteria.usuarios.dto.UsuarioRegistro;
import cl.duoc.ferreteria.usuarios.exception.ConflictoException;
import cl.duoc.ferreteria.usuarios.exception.SolicitudInvalidaException;
import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.model.Usuario;
import cl.duoc.ferreteria.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

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
}
