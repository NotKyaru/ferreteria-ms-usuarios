package cl.duoc.ferreteria.usuarios.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prueba la cadena de seguridad antes de que existan los controllers: el filtro y las reglas responden primero
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadBaseTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void swaggerEsPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión"))
                .andExpect(jsonPath("$.ruta").value("/api/v1/usuarios/me"));
    }

    @Test
    void tokenVencidoResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + TokenDePrueba.expirado("ADMINISTRADOR")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token inválido o expirado"));
    }

    @Test
    void refreshTokenNoSirveComoAccess() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + TokenDePrueba.refresh()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolSinPermisoResponde403() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + TokenDePrueba.access(3, "CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes permiso para esta acción"));
    }

    @Test
    void rutaNoDeclaradaSeRechazaAunqueSeaAdministrador() throws Exception {
        mockMvc.perform(get("/api/v1/otra-cosa").header("Authorization", "Bearer " + TokenDePrueba.access(1, "ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }
}
