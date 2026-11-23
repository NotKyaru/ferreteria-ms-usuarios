package cl.duoc.ferreteria.usuarios.dto;

import cl.duoc.ferreteria.usuarios.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// POST y PUT de /usuarios. A diferencia del registro, el administrador sí elige el rol
public record UsuarioAdmin(
        @NotBlank(message = "El RUT es obligatorio")
        String rut,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre admite hasta 60 caracteres")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 80, message = "Los apellidos admiten hasta 80 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        @Size(max = 120, message = "El correo admite hasta 120 caracteres")
        String correo,

        String password,                 // obligatoria al crear; al editar, vacía = no cambiar (lo valida el service)

        @Size(max = 20, message = "El teléfono admite hasta 20 caracteres") String telefono,
        @Size(max = 60, message = "La región admite hasta 60 caracteres") String region,
        @Size(max = 60, message = "La comuna admite hasta 60 caracteres") String comuna,
        @Size(max = 150, message = "La dirección admite hasta 150 caracteres") String direccion,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        Boolean activo) { }              // null = true
