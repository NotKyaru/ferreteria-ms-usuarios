package cl.duoc.ferreteria.usuarios.config;

import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.model.Usuario;
import cl.duoc.ferreteria.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Al levantar el contexto, CargaDatosIniciales ya corrió una vez sobre la base H2 vacía
@SpringBootTest
class CargaDatosInicialesTest {

    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CargaDatosIniciales cargaDatosIniciales;

    @Test
    void creaLosTresUsuariosDeDemoConSusRoles() {
        List<Usuario> usuarios = usuarioRepository.findAllByOrderByIdAsc();

        assertEquals(3, usuarios.size());
        assertEquals(Rol.ADMINISTRADOR, usuarioRepository.findByCorreo("admin@ferreteria.cl").orElseThrow().getRol());
        assertEquals(Rol.VENDEDOR, usuarioRepository.findByCorreo("vendedor@ferreteria.cl").orElseThrow().getRol());
        assertEquals(Rol.CLIENTE, usuarioRepository.findByCorreo("cliente@ferreteria.cl").orElseThrow().getRol());
    }

    @Test
    void laContrasenaSeGuardaHasheada() {
        String guardada = usuarioRepository.findByCorreo("admin@ferreteria.cl").orElseThrow().getPassword();

        assertTrue(guardada.startsWith("$2"));                       // formato BCrypt
        assertTrue(passwordEncoder.matches("Maestros2026", guardada));
    }

    @Test
    void siYaHayUsuariosNoLosDuplica() {
        cargaDatosIniciales.run();

        assertEquals(3, usuarioRepository.count());
    }
}
