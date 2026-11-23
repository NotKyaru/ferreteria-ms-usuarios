package cl.duoc.ferreteria.usuarios.repository;

import cl.duoc.ferreteria.usuarios.model.Rol;
import cl.duoc.ferreteria.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Spring Data arma cada consulta a partir del nombre del método
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByRut(String rut);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    boolean existsByRutAndIdNot(String rut, Long id);

    long countByRolAndActivoTrue(Rol rol);

    List<Usuario> findAllByOrderByIdAsc();

    List<Usuario> findByRolOrderByIdAsc(Rol rol);
}
