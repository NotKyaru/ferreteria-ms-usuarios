package cl.duoc.ferreteria.usuarios.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Escribe el JSON de error desde el filtro JWT y los manejadores 401/403, que corren antes de los controllers. */
public final class RespuestaErrorSeguridad {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private RespuestaErrorSeguridad() { }

    // Los mensajes son textos fijos sin comillas, por eso el JSON se puede armar a mano sin escapar
    public static void escribir(HttpServletResponse response, HttpServletRequest request,
                                int status, String mensaje) throws IOException {
        String json = """
                {"timestamp":"%s","status":%d,"error":"%s","mensaje":"%s","ruta":"%s"}"""
                .formatted(LocalDateTime.now(ZoneId.of("America/Santiago")).format(FORMATO),
                           status, HttpStatus.valueOf(status).getReasonPhrase(),
                           mensaje, request.getRequestURI());
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }
}
