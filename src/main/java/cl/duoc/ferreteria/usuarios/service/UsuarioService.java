package cl.duoc.ferreteria.usuarios.service;

import cl.duoc.ferreteria.usuarios.dto.UsuarioRegistro;
import cl.duoc.ferreteria.usuarios.exception.ConflictoException;
import cl.duoc.ferreteria.usuarios.exception.SolicitudInvalidaException;
import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.model.Usuario;
import cl.duoc.ferreteria.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    // 8 a 64 caracteres, con al menos una letra y un número (regla 3)
    private static final String PATRON_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Registro público: la cuenta siempre queda como CLIENTE y activa. */
    @Transactional
    public Usuario crearCliente(UsuarioRegistro entrada) {
        // Primero lo que se valida sin ir a la base, después los duplicados
        String rut = validarRut(entrada.rut());
        validarPassword(entrada.password());
        String correo = normalizarCorreo(entrada.correo());

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo");
        }
        if (usuarioRepository.existsByRut(rut)) {
            throw new ConflictoException("Ya existe una cuenta con ese RUT");
        }

        Usuario usuario = new Usuario();
        usuario.setRut(rut);
        usuario.setNombre(entrada.nombre().trim());
        usuario.setApellidos(entrada.apellidos().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(entrada.password()));
        usuario.setTelefono(opcional(entrada.telefono()));
        usuario.setRegion(opcional(entrada.region()));
        usuario.setComuna(opcional(entrada.comuna()));
        usuario.setDireccion(opcional(entrada.direccion()));
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }

    private static String validarRut(String rut) {
        if (!ValidadorRut.esValido(rut)) {
            throw new SolicitudInvalidaException("rut", "RUT inválido");
        }
        return ValidadorRut.normalizar(rut);
    }

    private static void validarPassword(String password) {
        if (password == null || !password.matches(PATRON_PASSWORD)) {
            throw new SolicitudInvalidaException("password",
                    "La contraseña debe tener entre 8 y 64 caracteres, con letras y números");
        }
    }

    // Un campo opcional vacío se guarda como null, no como ""
    private static String opcional(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
