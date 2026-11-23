package cl.duoc.ferreteria.usuarios.controller;

import cl.duoc.ferreteria.usuarios.dto.UsuarioAdmin;
import cl.duoc.ferreteria.usuarios.dto.UsuarioRespuesta;
import cl.duoc.ferreteria.usuarios.exception.ErrorRespuesta;
import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.security.UsuarioAutenticado;
import cl.duoc.ferreteria.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

// Quién puede entrar a cada ruta lo decide SecurityConfig: /me cualquier sesión, el resto solo ADMINISTRADOR
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Datos de la sesión y administración de usuarios")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Sin token, o token inválido o vencido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    @Operation(summary = "Datos del usuario de la sesión (cualquier rol)")
    @ApiResponse(responseCode = "200", description = "Usuario de la sesión")
    public UsuarioRespuesta me(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado actual) {
        return UsuarioRespuesta.desde(usuarioService.obtenerActual(actual));
    }

    @GetMapping
    @Operation(summary = "Lista usuarios, opcionalmente filtrados por rol (ADMINISTRADOR)")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public List<UsuarioRespuesta> listar(@RequestParam(required = false) Rol rol) {
        return usuarioService.listar(rol).stream().map(UsuarioRespuesta::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un usuario (ADMINISTRADOR)")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "No existe el usuario", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioRespuesta obtener(@PathVariable Long id) {
        return UsuarioRespuesta.desde(usuarioService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea un usuario con cualquier rol (ADMINISTRADOR)")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Correo o RUT ya registrado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<UsuarioRespuesta> crear(@Valid @RequestBody UsuarioAdmin entrada) {
        UsuarioRespuesta creado = UsuarioRespuesta.desde(usuarioService.crear(entrada));
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza datos y rol; contraseña vacía = no cambiar (ADMINISTRADOR)")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "No existe el usuario", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Duplicado, quitarse el propio rol o dejar sin administradores", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public UsuarioRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioAdmin entrada,
                                       @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado actual) {
        return UsuarioRespuesta.desde(usuarioService.actualizar(id, entrada, actual));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un usuario (ADMINISTRADOR)")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "No existe el usuario", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Eliminarse a sí mismo o al último administrador", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado actual) {
        usuarioService.eliminar(id, actual);
        return ResponseEntity.noContent().build();
    }
}
