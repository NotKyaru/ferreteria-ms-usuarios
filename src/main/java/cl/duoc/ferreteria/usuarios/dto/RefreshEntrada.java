package cl.duoc.ferreteria.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshEntrada(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken) { }
