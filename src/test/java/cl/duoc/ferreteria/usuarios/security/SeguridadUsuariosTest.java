package cl.duoc.ferreteria.usuarios.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de punta a punta: login real con los usuarios de demo y tokens firmados por JwtGenerador.
// @Transactional deshace al final de cada test lo que se haya creado
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadUsuariosTest {

    @Autowired
    MockMvc mockMvc;

    private String login(String correo) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"Maestros2026\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String accessToken(String correo) throws Exception {
        return JsonPath.read(login(correo), "$.accessToken");
    }

    @Test
    void loginConContrasenaMalaResponde401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin@ferreteria.cl\",\"password\":\"Incorrecta1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    void meSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión"));
    }

    @Test
    void clienteYVendedorNoPuedenListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + accessToken("cliente@ferreteria.cl")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + accessToken("vendedor@ferreteria.cl")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListaLosUsuariosSinContrasenas() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + accessToken("admin@ferreteria.cl")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void tokenVencidoResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + TokenDePrueba.expirado("ADMINISTRADOR")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token inválido o expirado"));
    }

    @Test
    void refreshConUnAccessTokenEnElCuerpoResponde401() throws Exception {
        String access = accessToken("cliente@ferreteria.cl");
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + access + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meConUnRefreshTokenEnElHeaderResponde401() throws Exception {
        String refresh = JsonPath.read(login("cliente@ferreteria.cl"), "$.refreshToken");
        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void renovarEntregaUnAccessQueFunciona() throws Exception {
        String refresh = JsonPath.read(login("cliente@ferreteria.cl"), "$.refreshToken");

        String renovado = mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + JsonPath.read(renovado, "$.accessToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("cliente@ferreteria.cl"));
    }

    // Por esto el front NO debe mandar el header Authorization al llamar a /auth/refresh
    @Test
    void refreshConUnAccessVencidoEnElHeaderResponde401AunqueSeaPublico() throws Exception {
        String refresh = JsonPath.read(login("cliente@ferreteria.cl"), "$.refreshToken");
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + TokenDePrueba.expirado("CLIENTE"))
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registroIgnoraElRolDelCuerpoYNoDevuelveLaContrasena() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rut":"12345678-5","nombre":"Intento","apellidos":"De Admin",
                                 "correo":"intento@correo.cl","password":"Clave2026x","rol":"ADMINISTRADOR"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/usuarios/")))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void validacionDevuelveDetallesPorCampo() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"\",\"nombre\":\"X\",\"apellidos\":\"Y\",\"correo\":\"no-es-correo\",\"password\":\"Clave2026x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.rut").value("El RUT es obligatorio"))
                .andExpect(jsonPath("$.detalles.correo").value("Formato de correo inválido"));
    }
}
