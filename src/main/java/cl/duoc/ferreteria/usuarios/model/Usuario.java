package cl.duoc.ferreteria.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

// Sin @Data: su toString() imprimiría el hash de la contraseña en los logs
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;                    // "12345678-5", sin puntos, K en mayúscula

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;                 // siempre en minúsculas

    @Column(nullable = false, length = 60)
    private String password;               // hash BCrypt, nunca el texto

    @Column(length = 20)
    private String telefono;

    @Column(length = 60)
    private String region;

    @Column(length = 60)
    private String comuna;

    @Column(length = 150)
    private String direccion;

    // STRING guarda "CLIENTE" y no un número: si se reordena el enum, los datos no se corrompen
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void alCrear() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Santiago"));
    }

    // Va en el claim "nombre" del token (aclaración C9.1)
    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}
