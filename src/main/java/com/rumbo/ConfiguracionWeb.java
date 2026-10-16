package com.rumbo;

import com.rumbo.modelo.SesionUsuario;
import com.rumbo.servicio.ExcepcionPermiso;
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

    public ConfiguracionWeb(GestorPermisos gestorPermisos) {
        this.gestorPermisos = gestorPermisos;
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
                        gestorPermisos.exigirSesion(sesion);
                        autenticado = true;

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

        // 2. Conserva las restricciones particulares de las pantallas por rol.
        registro.addInterceptor(
                new InterceptorPantallasRol(gestorPermisos)
        )
        .addPathPatterns(
                "/roles/**",
                "/gestion-carreras.html"
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

            String rutaPermitida;
            try {
                rutaPermitida = gestorPermisos.rutaParaRol(sesionUsuario.getRol());
            } catch (ExcepcionPermiso e) {
                respuesta.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                return false;
            }

            String rutaSolicitada = solicitud.getRequestURI()
                    .substring(solicitud.getContextPath().length());
            if ("/gestion-carreras.html".equals(rutaSolicitada)) {
                if (sesionUsuario.getRol() != Rol.ENCARGADO_DE_CARRERAS) {
                    respuesta.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Pantalla no existente"
                    );
                    return false;
                }

                respuesta.setHeader("Cache-Control", "no-store");
                return true;
            }

            if (!rutaPermitida.equals(rutaSolicitada)) {
                respuesta.sendRedirect(rutaPermitida);
                return false;
            }

            return true;
        }
    }
}
