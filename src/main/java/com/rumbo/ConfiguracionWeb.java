package com.rumbo;

import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.GestorPermisos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.rumbo.modelo.Rol;
import com.rumbo.servicio.ExcepcionAutenticacion;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final GestorPermisos gestorPermisos;

    private final com.rumbo.persistencia.UsuarioRepository usuarios;

    public ConfiguracionWeb(GestorPermisos gestorPermisos, com.rumbo.persistencia.UsuarioRepository usuarios) {
        this.gestorPermisos = gestorPermisos;
        this.usuarios = usuarios;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {

        // 1. Exige sesión para todas las rutas salvo las públicas.
        registro.addInterceptor(new HandlerInterceptor() {

            @Override
            public boolean preHandle(
                    HttpServletRequest solicitud,
                    HttpServletResponse respuesta,
                    Object handler) throws Exception {

                respuesta.setHeader("Cache-Control", "no-store");

                HttpSession sesion = solicitud.getSession(false);
                boolean autenticado = false;

                if (sesion != null) {
                    try {
                        SesionUsuario cuenta = gestorPermisos.exigirSesion(sesion);
                        if (usuarios.existsByIdUsuarioAndEliminadoTrue(cuenta.getIdUsuario())) {
                            sesion.invalidate();
                        } else {
                            autenticado = true;
                        }

                    } catch (ExcepcionAutenticacion e) {
                        autenticado = false;
                    }
                }

                if (autenticado) {
                    return true;
                }

                String ruta = solicitud.getRequestURI()
                        .substring(solicitud.getContextPath().length());

                if (ruta.startsWith("/api/")) {
                    respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    respuesta.setContentType("application/json");
                    respuesta.setCharacterEncoding("UTF-8");

                    respuesta.getWriter().write(
                            "{\"error\":\"Debes iniciar sesion\"}"
                    );

                } else {
                    respuesta.sendRedirect(
                            solicitud.getContextPath() + "/login.html"
                    );
                }

                return false;
            }

        })
        .addPathPatterns("/**")
        .excludePathPatterns(
                "/",
                "/index.html",
                "/login.html",
                "/api/auth/login",
                "/api/auth/registro",
                "/style.css",
                "/js/**",
                "/manifest.json",
                "/service-worker.js",
                "/favicon.ico",
                "/icons/**",
                "/error"
        )
        .order(0);

        // 2. Conserva la restricción de acceso a la gestión de carreras.
        registro.addInterceptor(
                new InterceptorPantallasRol(gestorPermisos)
        )
        .addPathPatterns(
                "/administracion-usuarios.html",
                "/administracion-publicaciones.html",
                "/registros-administrativos.html",
                "/test-vocacional.html",
                "/gestion-carreras.html",
                "/gestion-actividades.html"
        )
        .addPathPatterns(
                "/supervision-usuarios.html"
        )
        .addPathPatterns(
                "/roles/supervisor-mensajes.html"
        )
        .order(1);
    }

    private static class InterceptorPantallasRol implements HandlerInterceptor {

        private final GestorPermisos gestorPermisos;

        private InterceptorPantallasRol(GestorPermisos gestorPermisos) {
            this.gestorPermisos = gestorPermisos;
        }

        @Override
        public boolean preHandle(HttpServletRequest solicitud, HttpServletResponse respuesta, Object handler) throws Exception {
            HttpSession sesion = solicitud.getSession(false);
            if (sesion == null) {
                respuesta.sendRedirect("/login.html");
                return false;
            }

            SesionUsuario sesionUsuario =
                    (SesionUsuario) sesion.getAttribute(GestorPermisos.ATRIBUTO_SESION);
            if (sesionUsuario == null || !sesionUsuario.isActiva()) {
                respuesta.sendRedirect("/login.html");
                return false;
            }

            String rutaSolicitada = solicitud.getRequestURI()
                    .substring(solicitud.getContextPath().length());
            if ("/administracion-usuarios.html".equals(rutaSolicitada)
                    || "/administracion-publicaciones.html".equals(rutaSolicitada)
                    || "/registros-administrativos.html".equals(rutaSolicitada)) {
                if (sesionUsuario.getRol() != Rol.ADMIN) {
                    respuesta.sendError(HttpServletResponse.SC_FORBIDDEN, "Se requiere rol de administrador");
                    return false;
                }
                return true;
            }

            if ("/test-vocacional.html".equals(rutaSolicitada)) {
                if (sesionUsuario.getRol() != Rol.ESTUDIANTE) {
                    respuesta.sendError(HttpServletResponse.SC_FORBIDDEN, "Se requiere rol de estudiante");
                    return false;
                }
                return true;
            }

            if ("/gestion-carreras.html".equals(rutaSolicitada)) {
                Rol rol = sesionUsuario.getRol();
                if (rol != Rol.ENCARGADO_DE_CARRERAS && rol != Rol.ADMIN) {
                    respuesta.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Pantalla no existente"
                    );
                    return false;
                }

                respuesta.setHeader("Cache-Control", "no-store");
                return true;
            }

            if ("/gestion-actividades.html".equals(rutaSolicitada)) {
                Rol rol = sesionUsuario.getRol();
                if (rol != Rol.ENCARGADO_DE_CARRERAS && rol != Rol.ADMIN) {
                    respuesta.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Pantalla no existente"
                    );
                    return false;
                }

                respuesta.setHeader("Cache-Control", "no-store");
                return true;
            }

            if ("/supervision-usuarios.html".equals(rutaSolicitada)) {
                if (!gestorPermisos.puedeSupervisarUsuarios(sesionUsuario.getRol())) {
                    respuesta.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Pantalla no existente"
                    );
                    return false;
                }

                respuesta.setHeader("Cache-Control", "no-store");
                return true;
            }

            if ("/roles/supervisor-mensajes.html".equals(rutaSolicitada)) {
                if (sesionUsuario.getRol() != Rol.SUPERVISOR_DE_MENSAJES) {
                    respuesta.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Pantalla no existente"
                    );
                    return false;
                }

                respuesta.setHeader("Cache-Control", "no-store");
                return true;
            }

            return true;
        }
    }
}
