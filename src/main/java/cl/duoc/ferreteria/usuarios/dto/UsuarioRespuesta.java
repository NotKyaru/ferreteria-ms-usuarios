package cl.duoc.ferreteria.usuarios.dto;

import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.model.Usuario;

import java.time.LocalDateTime;

// Nunca incluye la contraseña: por eso la API devuelve este DTO y no la entidad
public record UsuarioRespuesta(Long id, String rut, String nombre, String apellidos, String correo,
                               String telefono, String region, String comuna, String direccion,
                               Rol rol, boolean activo, LocalDateTime creadoEn) {

    public static UsuarioRespuesta desde(Usuario u) {
        return new UsuarioRespuesta(u.getId(), u.getRut(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getTelefono(), u.getRegion(), u.getComuna(), u.getDireccion(),
                u.getRol(), u.isActivo(), u.getCreadoEn());
    }
}
