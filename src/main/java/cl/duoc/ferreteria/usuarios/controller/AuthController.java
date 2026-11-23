package cl.duoc.ferreteria.usuarios.controller;

import cl.duoc.ferreteria.usuarios.dto.AutenticacionRespuesta;
import cl.duoc.ferreteria.usuarios.dto.LoginEntrada;
import cl.duoc.ferreteria.usuarios.dto.RefreshEntrada;
import cl.duoc.ferreteria.usuarios.dto.UsuarioRegistro;
import cl.duoc.ferreteria.usuarios.exception.ErrorRespuesta;
import cl.duoc.ferreteria.usuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

// Sin lógica: valida la entrada (@Valid), llama al service y devuelve el DTO
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, registro de clientes y renovación del token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y entrega el par de tokens")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public AutenticacionRespuesta login(@Valid @RequestBody LoginEntrada entrada) {
        return authService.login(entrada);
    }

    @PostMapping("/registro")
    @Operation(summary = "Crea una cuenta de cliente e inicia sesión")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos (RUT, contraseña, formato)", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Correo o RUT ya registrado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<AutenticacionRespuesta> registro(@Valid @RequestBody UsuarioRegistro entrada) {
        AutenticacionRespuesta respuesta = authService.registrar(entrada);
        URI ubicacion = URI.create("/api/v1/usuarios/" + respuesta.usuario().id());
        return ResponseEntity.created(ubicacion).body(respuesta);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renueva el par de tokens con el refresh token")
    @ApiResponse(responseCode = "200", description = "Par nuevo de tokens")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido, vencido o de un usuario inactivo", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public AutenticacionRespuesta refresh(@Valid @RequestBody RefreshEntrada entrada) {
        return authService.refrescar(entrada);
    }
}
