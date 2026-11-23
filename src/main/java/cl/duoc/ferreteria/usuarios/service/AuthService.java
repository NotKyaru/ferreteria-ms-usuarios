package cl.duoc.ferreteria.usuarios.service;

import cl.duoc.ferreteria.usuarios.dto.AutenticacionRespuesta;
import cl.duoc.ferreteria.usuarios.dto.LoginEntrada;
import cl.duoc.ferreteria.usuarios.dto.RefreshEntrada;
import cl.duoc.ferreteria.usuarios.dto.UsuarioRegistro;
import cl.duoc.ferreteria.usuarios.dto.UsuarioRespuesta;
import cl.duoc.ferreteria.usuarios.exception.NoAutorizadoException;
import cl.duoc.ferreteria.usuarios.model.Usuario;
import cl.duoc.ferreteria.usuarios.repository.UsuarioRepository;
import cl.duoc.ferreteria.usuarios.security.JwtGenerador;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    // El mismo mensaje si el correo no existe, si la contraseña falla o si la cuenta está inactiva:
    // así no se revela qué correos están registrados (regla 4)
    private static final String CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos";
    private static final String SESION_EXPIRADA = "Sesión expirada, inicia sesión nuevamente";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtGenerador jwtGenerador;

    @Transactional(readOnly = true)
    public AutenticacionRespuesta login(LoginEntrada entrada) {
        Usuario usuario = usuarioRepository.findByCorreo(UsuarioService.normalizarCorreo(entrada.correo()))
                .filter(u -> passwordEncoder.matches(entrada.password(), u.getPassword()))
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new NoAutorizadoException(CREDENCIALES_INCORRECTAS));
        return responder(usuario);
    }

    @Transactional
    public AutenticacionRespuesta registrar(UsuarioRegistro entrada) {
        return responder(usuarioService.crearCliente(entrada));
    }

    // Lee el usuario desde la base: si el admin le cambió el rol o lo desactivó, se aplica al renovar
    @Transactional(readOnly = true)
    public AutenticacionRespuesta refrescar(RefreshEntrada entrada) {
        Long id;
        try {
            id = Long.valueOf(jwtGenerador.validarRefreshToken(entrada.refreshToken()).getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new NoAutorizadoException(SESION_EXPIRADA);
        }
        Usuario usuario = usuarioRepository.findById(id)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new NoAutorizadoException(SESION_EXPIRADA));
        return responder(usuario);
    }

    // Cada login, registro o renovación entrega un par nuevo (rotación del refresh token)
    private AutenticacionRespuesta responder(Usuario usuario) {
        return new AutenticacionRespuesta(
                jwtGenerador.generarAccessToken(usuario),
                jwtGenerador.generarRefreshToken(usuario),
                "Bearer",
                jwtGenerador.segundosAccess(),
                UsuarioRespuesta.desde(usuario));
    }
}
