package cl.duoc.ferreteria.usuarios.dto;

/** Respuesta de login, registro y refresh (contrato §6.3). tipo = "Bearer", expiraEn en segundos. */
public record AutenticacionRespuesta(String accessToken, String refreshToken, String tipo,
                                     long expiraEn, UsuarioRespuesta usuario) { }
