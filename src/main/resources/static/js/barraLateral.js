/**
 * Rumbo · Sistema de Orientación Vocacional (UVG)
 * Lógica y control de la barra lateral de navegación por rol
 */

document.addEventListener("DOMContentLoaded", async () => {
    const barraLateral = document.getElementById("barra-lateral");
    if (!barraLateral) return;

    // Elementos de la barra lateral
    const btnAbrirMenu = document.getElementById("btn-abrir-menu");
    const backdropMenu = document.getElementById("backdrop-menu");
    const contenedorUsuario = document.getElementById("usuario-bloque-perfil");
    const textoPerfil = document.getElementById("texto-enlace-perfil");
    const inicialesElem = document.getElementById("usuario-iniciales");
    const nombreElem = document.getElementById("usuario-nombre");
    const rolEtiquetaElem = document.getElementById("usuario-rol-etiqueta");
    const notificacionesContenedor = document.getElementById("usuario-notificaciones-contenedor");
    const badgeNotificaciones = document.getElementById("badge-notificaciones-no-leidas");
    const btnCerrarSesion = document.getElementById("cerrar-sesion");

    // Control responsive de apertura / cierre
    const toggleMenu = (abrir) => {
        const estaAbierta = barraLateral.classList.contains("abierta");
        const nuevoEstado = abrir !== undefined ? abrir : !estaAbierta;
        barraLateral.classList.toggle("abierta", nuevoEstado);
        if (backdropMenu) {
            backdropMenu.classList.toggle("activo", nuevoEstado);
        }
        if (btnAbrirMenu) {
            btnAbrirMenu.setAttribute("aria-expanded", String(nuevoEstado));
        }
    };

    if (btnAbrirMenu) {
        btnAbrirMenu.addEventListener("click", () => toggleMenu());
    }
    if (backdropMenu) {
        backdropMenu.addEventListener("click", () => toggleMenu(false));
    }
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && barraLateral.classList.contains("abierta")) {
            toggleMenu(false);
        }
    });

    // Cerrar el panel al hacer clic en un enlace de navegación en móvil
    barraLateral.querySelectorAll(".barra-lateral-nav a").forEach((enlace) => {
        enlace.addEventListener("click", () => {
            if (window.innerWidth <= 1024) {
                toggleMenu(false);
            }
        });
    });

    // Manejador de cierre de sesión para páginas secundarias
    if (btnCerrarSesion && !window.location.pathname.endsWith("inicio.html") && window.location.pathname !== "/") {
        btnCerrarSesion.addEventListener("click", async () => {
            btnCerrarSesion.disabled = true;
            try {
                await fetch("/api/auth/logout", {
                    method: "POST",
                    credentials: "same-origin",
                    cache: "no-store"
                });
            } catch (e) {
                console.error("Error al cerrar sesión:", e);
            }
            window.location.replace("/login.html");
        });
    }

    try {
        const sesion = await obtenerSesion();
        if (!sesion) return;

        const rol = String(sesion.rol || "").toUpperCase();

        // 1. Datos del usuario en la zona inferior
        if (nombreElem) {
            nombreElem.textContent = sesion.nombreCompleto || "Usuario";
            nombreElem.title = sesion.nombreCompleto || "";
        }

        if (inicialesElem) {
            inicialesElem.textContent = calcularIniciales(sesion.nombreCompleto);
        }

        if (rolEtiquetaElem) {
            rolEtiquetaElem.textContent = formatearRol(rol);
        }

        // Configurar texto de perfil según el rol
        if (textoPerfil) {
            const esPerfilEditable = rol === "ESTUDIANTE" || rol === "COLABORADOR";
            textoPerfil.textContent = esPerfilEditable ? "Mi perfil" : "Mi cuenta";
        }

        // Asegurar que la zona del usuario redirija al perfil
        if (contenedorUsuario) {
            contenedorUsuario.href = "/perfil.html";
            contenedorUsuario.title = "Ver información de mi cuenta";
        }

        // 2. Visibilidad de opciones de navegación según el rol (Sección 4.2)
        aplicarOpcionesPorRol(rol);

        // 3. Marcar página actual
        resaltarPaginaActual();

        // 4. Notificaciones para cualquier usuario autenticado
        if (notificacionesContenedor) {
            notificacionesContenedor.hidden = false;
        }
        await verificarNotificacionesNoLeidas(badgeNotificaciones);

        // 5. Estado de verificación solo para estudiantes
        if (rol === "ESTUDIANTE") {
            await verificarEstadoForoEstudiante();
        }

        // 6. Contenido del inicio según rol (Sección 5 del documento)
        await activarContenidoInicioPorRol(rol);

    } catch (error) {
        console.error("Error al inicializar la barra lateral:", error);
    }

    // Funciones auxiliares
    function calcularIniciales(nombreCompleto) {
        if (!nombreCompleto) return "RU";
        const partes = nombreCompleto.trim().split(/\s+/);
        if (partes.length === 1) {
            return partes[0].substring(0, 2).toUpperCase();
        }
        return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
    }

    function formatearRol(rol) {
        const nombres = {
            ESTUDIANTE: "Estudiante",
            COLABORADOR: "Colaborador",
            SUPERVISOR_DE_USUARIOS: "Supervisor de usuarios",
            SUPERVISOR_DE_MENSAJES: "Supervisor de mensajes",
            ENCARGADO_DE_CARRERAS: "Encargado de carreras",
            ADMIN: "Administrador"
        };
        return nombres[rol] || rol;
    }

    function aplicarOpcionesPorRol(rol) {
        // Enlaces del menú
        const navInicio = document.getElementById("enlace-inicio");
        const navLineaTiempo = document.getElementById("enlace-linea-tiempo");
        const navPruebas = document.getElementById("enlace-pruebas");
        const navBitacora = document.getElementById("enlace-bitacora");
        const navCarreras = document.getElementById("enlace-carreras");
        const navComparador = document.getElementById("enlace-comparador");
        const navForo = document.getElementById("enlace-foro");
        const navSupervision = document.getElementById("enlace-supervision");
        const navCambiosPerfil = document.getElementById("enlace-cambios-perfil");
        const navRevisionMensajes = document.getElementById("enlace-revision-mensajes");
        const navGestionActividades = document.getElementById("enlace-gestion-actividades");
        const navGestionCarreras = document.getElementById("enlace-gestion-carreras");

        // Ocultar todos por defecto
        [
            navLineaTiempo, navPruebas, navBitacora, navCarreras, navComparador,
            navForo, navSupervision, navCambiosPerfil, navRevisionMensajes,
            navGestionActividades, navGestionCarreras
        ].forEach((elem) => {
            if (elem) elem.hidden = true;
        });

        // Inicio siempre visible
        if (navInicio) navInicio.hidden = false;

        // Mostrar opciones según rol (Sección 4.2 del documento de diseño)
        switch (rol) {
            case "ESTUDIANTE":
                if (navLineaTiempo) navLineaTiempo.hidden = false;
                if (navPruebas) navPruebas.hidden = false;
                if (navBitacora) navBitacora.hidden = false;
                if (navCarreras) navCarreras.hidden = false;
                if (navComparador) navComparador.hidden = false;
                if (navForo) navForo.hidden = false;
                break;

            case "COLABORADOR":
                if (navForo) navForo.hidden = false;
                break;

            case "SUPERVISOR_DE_USUARIOS":
                if (navSupervision) {
                    navSupervision.hidden = false;
                    navSupervision.querySelector("span").textContent = "Verificaciones";
                }
                if (navCambiosPerfil) navCambiosPerfil.hidden = false;
                break;

            case "SUPERVISOR_DE_MENSAJES":
                if (navRevisionMensajes) navRevisionMensajes.hidden = false;
                break;

            case "ENCARGADO_DE_CARRERAS":
                if (navGestionCarreras) navGestionCarreras.hidden = false;
                if (navGestionActividades) {
                    navGestionActividades.hidden = false;
                    navGestionActividades.querySelector("span").textContent = "Actividades";
                }
                break;

            case "ADMIN":
                if (navSupervision) {
                    navSupervision.hidden = false;
                    navSupervision.querySelector("span").textContent = "Usuarios";
                }
                if (navForo) navForo.hidden = false;
                if (navGestionCarreras) navGestionCarreras.hidden = false;
                if (navGestionActividades) {
                    navGestionActividades.hidden = false;
                    navGestionActividades.querySelector("span").textContent = "Registros";
                }
                break;
        }
    }

    function resaltarPaginaActual() {
        const rutaActual = window.location.pathname.toLowerCase();
        const enlaces = barraLateral.querySelectorAll(".barra-lateral-nav a");

        enlaces.forEach((enlace) => {
            const rutaEnlace = enlace.getAttribute("href");
            if (!rutaEnlace) return;

            const esPaginaActual =
                (rutaActual === "/" || rutaActual.endsWith("/inicio.html")) &&
                rutaEnlace.includes("inicio.html") ||
                rutaActual.includes(rutaEnlace.replace(".html", ""));

            if (esPaginaActual) {
                enlace.classList.add("activo");
                enlace.setAttribute("aria-current", "page");
            } else {
                enlace.classList.remove("activo");
                enlace.removeAttribute("aria-current");
            }
        });
    }

    async function verificarNotificacionesNoLeidas(badge) {
        if (!badge) return;
        try {
            const res = await fetch("/api/supervision/usuarios/notificaciones", {
                credentials: "same-origin",
                cache: "no-store"
            });
            if (!res.ok) return;
            const notificaciones = await res.json();
            const noLeidas = notificaciones.filter((n) => !n.leida).length;
            if (noLeidas > 0) {
                badge.textContent = noLeidas > 99 ? "99+" : String(noLeidas);
                badge.hidden = false;
            } else {
                badge.hidden = true;
            }
        } catch {
            badge.hidden = true;
        }
    }

    async function verificarEstadoForoEstudiante() {
        const enlaceForo = document.getElementById("enlace-foro");
        if (!enlaceForo) return;

        try {
            const res = await fetch("/api/verificacion/estado", {
                credentials: "same-origin",
                cache: "no-store"
            });
            if (!res.ok) return;

            const datos = await res.json();
            const estaVerificado = datos.estadoVerificacion === "verificado";

            const avisoInicio = document.getElementById("aviso-verificacion-inicio");
            if (avisoInicio) {
                avisoInicio.hidden = estaVerificado;
            }

            // Si no está verificado, mantener el foro visible con etiqueta «Requiere verificación»
            if (!estaVerificado) {
                enlaceForo.hidden = false;
                let etiqueta = enlaceForo.querySelector(".nav-item-etiqueta");
                if (!etiqueta) {
                    etiqueta = document.createElement("span");
                    etiqueta.className = "nav-item-etiqueta";
                    etiqueta.textContent = "Requiere verificación";
                    enlaceForo.appendChild(etiqueta);
                }
                enlaceForo.addEventListener("click", (e) => {
                    e.preventDefault();
                    mostrarAvisoVerificacionForo();
                });
            }
        } catch (e) {
            console.error("Error al consultar estado de verificación:", e);
        }
    }

    async function activarContenidoInicioPorRol(rol) {
        const esInicio = window.location.pathname === "/" || window.location.pathname.endsWith("inicio.html");
        if (!esInicio) return;

        const secEstudiante = document.getElementById("resumen-estudiante-inicio");
        const secColaborador = document.getElementById("resumen-colaborador-inicio");
        const secSupervisor = document.getElementById("resumen-supervisor-inicio");
        const secMensajes = document.getElementById("resumen-mensajes-inicio");
        const secEncargado = document.getElementById("resumen-encargado-inicio");
        const secAdmin = document.getElementById("resumen-admin-inicio");

        if (rol === "ESTUDIANTE" && secEstudiante) {
            secEstudiante.hidden = false;
            try {
                const res = await fetch("/api/mi-progreso/resumen", { credentials: "same-origin", cache: "no-store" });
                if (res.ok) {
                    const data = await res.json();
                    const barra = document.getElementById("progreso-estudiante-inicio");
                    const texto = document.getElementById("texto-progreso-inicio");
                    const pct = Math.round(data.porcentaje || 0);
                    if (barra) barra.value = pct;
                    if (texto) {
                        texto.textContent = `${data.actividadesCompletadas || 0} de ${data.totalActividades || 0} actividades completadas (${pct}%)`;
                    }
                }
            } catch {
                // Silencioso
            }
        } else if (rol === "COLABORADOR" && secColaborador) {
            secColaborador.hidden = false;
        } else if (rol === "SUPERVISOR_DE_USUARIOS" && secSupervisor) {
            secSupervisor.hidden = false;
        } else if (rol === "SUPERVISOR_DE_MENSAJES" && secMensajes) {
            secMensajes.hidden = false;
        } else if (rol === "ENCARGADO_DE_CARRERAS" && secEncargado) {
            secEncargado.hidden = false;
        } else if (rol === "ADMIN" && secAdmin) {
            secAdmin.hidden = false;
        }
    }

    function mostrarAvisoVerificacionForo() {
        let dialogo = document.getElementById("modal-aviso-verificacion");
        if (!dialogo) {
            dialogo = document.createElement("dialog");
            dialogo.id = "modal-aviso-verificacion";
            dialogo.className = "modal-edicion";
            dialogo.innerHTML = `
                <h2>Acceso al Foro</h2>
                <p style="margin: 1rem 0; line-height: 1.5;">
                    Para participar e interactuar en el foro necesitas estar verificado.
                    Puedes solicitar tu verificación desde tu perfil.
                </p>
                <div class="acciones-form">
                    <a href="/perfil.html#verificacion-estudiante" class="btn btn-destaque">Ir a mi perfil</a>
                    <button type="button" class="btn btn-secundario" id="btn-cerrar-aviso-verificacion">Cerrar</button>
                </div>
            `;
            document.body.appendChild(dialogo);
            dialogo.querySelector("#btn-cerrar-aviso-verificacion").addEventListener("click", () => {
                dialogo.close();
            });
        }
        dialogo.showModal();
    }
});
