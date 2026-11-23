package cl.duoc.ferreteria.usuarios.config;

import cl.duoc.ferreteria.usuarios.dto.UsuarioAdmin;
import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.repository.UsuarioRepository;
import cl.duoc.ferreteria.usuarios.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Crea los 3 usuarios de demo al arrancar, solo si la tabla está vacía. */
@Component
@RequiredArgsConstructor
public class CargaDatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaDatosIniciales.class);

    // Contraseña de demostración: aparece en el manual de usuario, no es un secreto
    private static final String PASSWORD_DEMO = "Maestros2026";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        // Se crean con el service: pasan las mismas validaciones y la contraseña se guarda con BCrypt
        crear("11111111-1", "Alex", "Muñoz Herrera", "admin@ferreteria.cl", Rol.ADMINISTRADOR);
        crear("22222222-2", "Valentina", "Soto Pizarro", "vendedor@ferreteria.cl", Rol.VENDEDOR);
        crear("33333333-3", "Camila", "Rojas Díaz", "cliente@ferreteria.cl", Rol.CLIENTE);
        log.info("Se crearon los 3 usuarios de demo");
    }

    private void crear(String rut, String nombre, String apellidos, String correo, Rol rol) {
        usuarioService.crear(new UsuarioAdmin(rut, nombre, apellidos, correo, PASSWORD_DEMO,
                null, null, null, null, rol, true));
    }
}
