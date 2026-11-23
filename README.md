# ms-usuarios · Ferretería Los Maestros

Microservicio de usuarios de la tienda online de **Ferretería Los Maestros** (DSY1104 Desarrollo Fullstack II, Duoc UC, Evaluación Parcial 3).

Se encarga del registro de clientes, el inicio de sesión, la **emisión y renovación de los JWT** y la administración de usuarios. Es el único servicio que firma tokens: `ms-catalogo` y `ms-ventas` solo los validan con el mismo secreto.

## Stack

- Java 17 · Spring Boot 4.1.2 · Maven (wrapper incluido)
- Spring Web, Spring Data JPA, Spring Security, Bean Validation
- MySQL 8 (schema `db_usuarios`) · H2 solo para los tests
- JWT con jjwt 0.12.6 · Swagger con springdoc 3.2.0 · Lombok

## Cómo levantarlo en local

Requisitos: JDK 17 o superior y Docker Desktop abierto.

1. Levantar MySQL (una sola vez; después basta `docker start ferreteria-mysql`):

   ```bash
   docker run -d --name ferreteria-mysql -p 3306:3306 \
     -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e MYSQL_DATABASE=db_usuarios \
     -e MYSQL_USER=ferreteria -e MYSQL_PASSWORD=<tu-contraseña> \
     -v ferreteria-mysql-datos:/var/lib/mysql mysql:8
   ```

2. Copiar `.env.example` a `.env` y completar los valores (ver la tabla de abajo). El `.env` **no se sube** al repositorio.

3. Arrancar el servicio:

   ```bash
   ./mvnw spring-boot:run        # en Windows: mvnw.cmd spring-boot:run
   ```

4. Abrir Swagger en http://localhost:8081/swagger-ui.html

Al primer arranque se crean 3 usuarios de demostración (solo si la tabla está vacía):

| Correo | Rol | Contraseña |
|---|---|---|
| `admin@ferreteria.cl` | ADMINISTRADOR | `Maestros2026` |
| `vendedor@ferreteria.cl` | VENDEDOR | `Maestros2026` |
| `cliente@ferreteria.cl` | CLIENTE | `Maestros2026` |

## Variables de entorno

| Variable | Ejemplo | Nota |
|---|---|---|
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | En Docker Compose, `mysql` |
| `DB_USER` / `DB_PASSWORD` | `ferreteria` / — | Usuario de aplicación de MySQL |
| `JWT_SECRET` | — | Base64 de 32 bytes o más (`openssl rand -base64 32`). **El mismo en los 3 servicios.** Sin valor por defecto: si falta, el servicio no arranca |
| `JWT_ACCESS_MINUTOS` | `15` | Duración del access token |
| `JWT_REFRESH_DIAS` | `7` | Duración del refresh token |

## Endpoints (`/api/v1`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| POST | `/auth/login` | Inicia sesión y entrega el par de tokens | Público |
| POST | `/auth/registro` | Crea una cuenta de cliente e inicia sesión | Público |
| POST | `/auth/refresh` | Renueva el par de tokens con el refresh token | Público |
| GET | `/usuarios/me` | Datos del usuario de la sesión | Cualquier sesión |
| GET | `/usuarios?rol=` | Lista usuarios (filtro por rol opcional) | ADMINISTRADOR |
| GET | `/usuarios/{id}` | Detalle de un usuario | ADMINISTRADOR |
| POST | `/usuarios` | Crea un usuario con cualquier rol | ADMINISTRADOR |
| PUT | `/usuarios/{id}` | Actualiza datos y rol (contraseña vacía = no cambiar) | ADMINISTRADOR |
| DELETE | `/usuarios/{id}` | Elimina un usuario | ADMINISTRADOR |

Los códigos de respuesta de cada endpoint están documentados en Swagger. Para probar los endpoints protegidos: `POST /auth/login` → copiar el `accessToken` → botón **Authorize**.

## Tests

```bash
./mvnw test        # en Windows: mvnw.cmd test
```

Incluye tests unitarios de las reglas de negocio (`ValidadorRutTest`, `UsuarioServiceTest`, `AuthServiceTest`, `JwtGeneradorTest`) y tests de integración de seguridad con MockMvc (`SeguridadBaseTest`, `SeguridadUsuariosTest`).

## Docker

```bash
docker build -t ms-usuarios .
```

El `Dockerfile` compila el proyecto en una primera etapa y deja solo el `.jar` sobre un JRE en la imagen final. En el despliegue lo levanta el `docker-compose.yml` de `ferreteria-los-maestros/despliegue/`.
