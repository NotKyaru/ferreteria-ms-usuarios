package cl.duoc.ferreteria.usuarios.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtFiltro jwtFiltro) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                       // API stateless con JWT en header, sin cookies
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()          // sin esto, los errores reales se ven como 401
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Reglas propias de ms-usuarios (contrato §7.4)
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro", "/api/v1/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me").authenticated()
                .requestMatchers("/api/v1/usuarios/**").hasRole("ADMINISTRADOR")
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> RespuestaErrorSeguridad.escribir(res, req, 401, "Debes iniciar sesión"))
                .accessDeniedHandler((req, res, ex) -> RespuestaErrorSeguridad.escribir(res, req, 403, "No tienes permiso para esta acción")))
            .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Sin esto, Spring Boot registra JwtFiltro también fuera de la cadena de seguridad y el filtro corre dos veces
    @Bean
    FilterRegistrationBean<JwtFiltro> registroJwtFiltro(JwtFiltro jwtFiltro) {
        FilterRegistrationBean<JwtFiltro> registro = new FilterRegistrationBean<>(jwtFiltro);
        registro.setEnabled(false);
        return registro;
    }

    // Evita que Spring Boot cree un usuario en memoria e imprima "Using generated security password"
    @Bean
    UserDetailsService sinUsuariosEnMemoria() {
        return username -> { throw new UsernameNotFoundException(username); };
    }

    // Solo ms-usuarios guarda contraseñas: BCrypt agrega sal y es lento a propósito
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
