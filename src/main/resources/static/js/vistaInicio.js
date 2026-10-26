document.addEventListener("DOMContentLoaded", async () => {
    const acciones = document.getElementById("acciones");
    const mensaje = document.getElementById("mensaje-inicio");
    const botonCerrarSesion = document.getElementById("cerrar-sesion");
    const enlaceForo = document.getElementById("enlace-foro");
    const enlaceSupervision = document.getElementById("enlace-supervision");
    const enlaceLineaTiempo =
    document.getElementById("enlace-linea-tiempo");

    acciones.hidden = true;

    try {
        const sesion = await obtenerSesion();
        if (!sesion) return;

        const rol = String(sesion.rol || "").toUpperCase();
        enlaceLineaTiempo.hidden = rol !== "ESTUDIANTE";
        enlaceSupervision.hidden = rol !== "ADMIN"
            && rol !== "SUPERVISOR_DE_USUARIOS";

        document.getElementById("saludo").textContent =
            "Hola, " + sesion.nombreCompleto;
        document.getElementById("encabezado-usuario").textContent =
            "Orientación vocacional para " + sesion.nombreCompleto;

        await mostrarForoSiEstaVerificado();
        await cargarNotificaciones();

        acciones.hidden = false;

    } catch (error) {
        mensaje.textContent = error.message || "No se pudo cargar el menú.";
    }

    botonCerrarSesion.addEventListener("click", async () => {
        botonCerrarSesion.disabled = true;
        mensaje.textContent = "";

        try {
            const respuesta = await fetch("/api/auth/logout", {
                method: "POST",
                credentials: "same-origin",
                cache: "no-store"
            });

            if (!respuesta.ok && respuesta.status !== 401) {
                throw new Error("No se pudo cerrar la sesión.");
            }

            window.location.replace("/login.html");
        } catch (error) {
            botonCerrarSesion.disabled = false;
            mensaje.textContent = error.message;
        }
    });

    async function mostrarForoSiEstaVerificado() {
        const respuesta = await fetch("/api/verificacion/estado", {
            credentials: "same-origin",
            cache: "no-store"
        });

        if (!respuesta.ok) {
            throw new Error("No se pudo consultar el estado de verificación.");
        }

        const estado = await respuesta.json();
        enlaceForo.hidden = estado.estadoVerificacion !== "verificado";
    }

    async function cargarNotificaciones() {
        const seccion = document.getElementById("notificaciones-inicio");
        const lista = document.getElementById("lista-notificaciones-inicio");
        const respuesta = await fetch("/api/supervision/usuarios/notificaciones", {
            credentials: "same-origin",
            cache: "no-store"
        });
        if (!respuesta.ok) {
            return;
        }
        const notificaciones = await respuesta.json();
        if (notificaciones.length === 0) {
            return;
        }
        seccion.hidden = false;
        notificaciones.forEach(notificacion => {
            const elemento = document.createElement("article");
            elemento.className = notificacion.leida
                ? "notificacion"
                : "notificacion notificacion-no-leida";
            elemento.textContent = `${notificacion.titulo}: ${notificacion.mensaje}`;
            if (!notificacion.leida) {
                const boton = document.createElement("button");
                boton.className = "btn btn-pequeno";
                boton.type = "button";
                boton.textContent = "Marcar como leída";
                boton.addEventListener("click", async () => {
                    await fetch(`/api/supervision/usuarios/notificaciones/${notificacion.idNotificacion}/leida`, {
                        method: "PUT",
                        credentials: "same-origin"
                    });
                    boton.disabled = true;
                    elemento.classList.remove("notificacion-no-leida");
                });
                elemento.appendChild(boton);
            }
            lista.appendChild(elemento);
        });
    }
});