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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    // 8 a 64 caracteres, con al menos una letra y un número (regla 3)
    private static final String PATRON_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Usuario> listar(Rol rol) {
        return rol == null ? usuarioRepository.findAllByOrderByIdAsc() : usuarioRepository.findByRolOrderByIdAsc(rol);
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id));
    }

    // Si el usuario del token fue borrado o desactivado, la sesión deja de servir (C9.7)
    @Transactional(readOnly = true)
    public Usuario obtenerActual(UsuarioAutenticado actual) {
        return usuarioRepository.findById(actual.id())
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new NoAutorizadoException("Sesión expirada, inicia sesión nuevamente"));
    }

    /** Registro público: la cuenta siempre queda como CLIENTE y activa. */
    @Transactional
    public Usuario crearCliente(UsuarioRegistro r) {
        return crear(new UsuarioAdmin(r.rut(), r.nombre(), r.apellidos(), r.correo(), r.password(),
                r.telefono(), r.region(), r.comuna(), r.direccion(), Rol.CLIENTE, true));
    }

    @Transactional
    public Usuario crear(UsuarioAdmin entrada) {
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
        copiarDatos(usuario, entrada, rut, correo);
        usuario.setPassword(passwordEncoder.encode(entrada.password()));
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizar(Long id, UsuarioAdmin entrada, UsuarioAutenticado actual) {
        Usuario usuario = obtener(id);
        String rut = validarRut(entrada.rut());
        String correo = normalizarCorreo(entrada.correo());
        boolean cambiaPassword = entrada.password() != null && !entrada.password().isBlank();
        if (cambiaPassword) {
            validarPassword(entrada.password());
        }

        boolean quedaActivo = entrada.activo() == null || entrada.activo();
        boolean dejaDeSerAdmin = usuario.getRol() == Rol.ADMINISTRADOR && usuario.isActivo()
                && (entrada.rol() != Rol.ADMINISTRADOR || !quedaActivo);

        // Regla 5: el administrador no puede quitarse el rol ni desactivarse a sí mismo
        if (dejaDeSerAdmin && id.equals(actual.id())) {
            throw new ConflictoException(entrada.rol() != Rol.ADMINISTRADOR
                    ? "No puedes quitarte el rol de administrador"
                    : "No puedes desactivar tu propia cuenta");
        }
        // Regla 6: siempre debe quedar al menos un administrador activo
        if (dejaDeSerAdmin && esElUltimoAdministrador()) {
            throw new ConflictoException("Debe quedar al menos un administrador");
        }

        // Los duplicados se buscan entre los OTROS usuarios: conservar el propio correo no es conflicto
        if (usuarioRepository.existsByCorreoAndIdNot(correo, id)) {
            throw new ConflictoException("Ya existe una cuenta con ese correo");
        }
        if (usuarioRepository.existsByRutAndIdNot(rut, id)) {
            throw new ConflictoException("Ya existe una cuenta con ese RUT");
        }

        copiarDatos(usuario, entrada, rut, correo);
        if (cambiaPassword) {
            usuario.setPassword(passwordEncoder.encode(entrada.password()));
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(Long id, UsuarioAutenticado actual) {
        Usuario usuario = obtener(id);
        if (id.equals(actual.id())) {
            throw new ConflictoException("No puedes eliminar tu propia cuenta");
        }
        if (usuario.getRol() == Rol.ADMINISTRADOR && usuario.isActivo() && esElUltimoAdministrador()) {
            throw new ConflictoException("Debe quedar al menos un administrador");
        }
        usuarioRepository.delete(usuario);
    }

    static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }

    private boolean esElUltimoAdministrador() {
        return usuarioRepository.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1;
    }

    private static void copiarDatos(Usuario usuario, UsuarioAdmin entrada, String rut, String correo) {
        usuario.setRut(rut);
        usuario.setNombre(entrada.nombre().trim());
        usuario.setApellidos(entrada.apellidos().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(opcional(entrada.telefono()));
        usuario.setRegion(opcional(entrada.region()));
        usuario.setComuna(opcional(entrada.comuna()));
        usuario.setDireccion(opcional(entrada.direccion()));
        usuario.setRol(entrada.rol());
        usuario.setActivo(entrada.activo() == null || entrada.activo());
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
