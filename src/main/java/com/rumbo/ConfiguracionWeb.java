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

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final GestorPermisos gestorPermisos;

    public ConfiguracionWeb(GestorPermisos gestorPermisos) {
        this.gestorPermisos = gestorPermisos;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new InterceptorPantallasRol(gestorPermisos))
                .addPathPatterns("/roles/**", "/gestion-carreras.html");
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
