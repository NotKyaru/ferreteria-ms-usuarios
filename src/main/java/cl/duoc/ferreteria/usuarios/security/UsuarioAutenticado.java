package cl.duoc.ferreteria.usuarios.security;

/** Datos del usuario que vienen en el token. Los controllers lo reciben con @AuthenticationPrincipal. */
public record UsuarioAutenticado(Long id, String correo, String nombre, String rol) { }
