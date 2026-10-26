document.addEventListener("DOMContentLoaded", async () => {
    const mensaje = document.getElementById("mensaje-supervision");
    const listaVerificaciones = document.getElementById("lista-verificaciones");
    const listaPerfiles = document.getElementById("lista-perfiles");
    const listaNotificaciones = document.getElementById("lista-notificaciones");

    try {
        const sesion = await obtenerSesion();
        const rol = String(sesion.rol || "").toUpperCase();
        if (rol !== "ADMIN" && rol !== "SUPERVISOR_DE_USUARIOS") {
            window.location.replace("/inicio.html");
            return;
        }
        const [verificaciones, perfiles, notificaciones] = await Promise.all([
            solicitar("/api/supervision/usuarios/verificaciones"),
            solicitar("/api/supervision/usuarios/perfiles"),
            solicitar("/api/supervision/usuarios/notificaciones")
        ]);
        renderizarSolicitudes(listaVerificaciones, verificaciones, "verificaciones");
        renderizarSolicitudes(listaPerfiles, perfiles, "perfiles");
        renderizarNotificaciones(listaNotificaciones, notificaciones);
    } catch (error) {
        mensaje.textContent = error.message || "No se pudo cargar el tablero.";
    }

    async function decidir(tipo, idSolicitud, decision) {
        let motivo = "";
        if (decision === "RECHAZAR") {
            motivo = window.prompt("Indica el motivo del rechazo:", "") || "";
            if (!motivo.trim()) {
                return;
            }
        }
        await solicitar(`/api/supervision/usuarios/${tipo}/${idSolicitud}`, {
            method: "PUT",
            body: JSON.stringify({ decision, motivo })
        });
        window.location.reload();
    }

    function renderizarSolicitudes(contenedor, solicitudes, tipo) {
        if (solicitudes.length === 0) {
            contenedor.textContent = "No hay solicitudes pendientes.";
            return;
        }
        solicitudes.forEach(solicitud => {
            const tarjeta = document.createElement("article");
            tarjeta.className = "supervision-solicitud";
            tarjeta.innerHTML = `
                <h3>${escapar(solicitud.nombreCompleto)} (@${escapar(solicitud.nombreUsuario)})</h3>
                <p><strong>Correo:</strong> ${escapar(solicitud.correo)}</p>
                <p><strong>Rol:</strong> ${escapar(solicitud.rol)}</p>
                <p><strong>Nivel:</strong> ${escapar(solicitud.nivelEstudios || "No indicado")}</p>
                <p><strong>Carrera:</strong> ${escapar(solicitud.carreraNombre || "No indicada")}</p>
                <p><strong>Descripción:</strong> ${escapar(solicitud.descripcionPersonal || "No indicada")}</p>
                ${tipo === "perfiles" ? `<p><strong>Propuesto:</strong> ${escapar(solicitud.nombreUsuarioSolicitado)} / ${escapar(solicitud.nombreCompletoSolicitado)}</p>` : ""}
            `;
            const aprobar = document.createElement("button");
            aprobar.className = "btn";
            aprobar.textContent = "Aprobar";
            aprobar.addEventListener("click", () => decidir(tipo, solicitud.idSolicitud, "APROBAR"));
            const rechazar = document.createElement("button");
            rechazar.className = "btn btn-secundario";
            rechazar.textContent = "Rechazar";
            rechazar.addEventListener("click", () => decidir(tipo, solicitud.idSolicitud, "RECHAZAR"));
            tarjeta.append(aprobar, rechazar);
            contenedor.appendChild(tarjeta);
        });
    }

    function renderizarNotificaciones(contenedor, notificaciones) {
        if (notificaciones.length === 0) {
            contenedor.textContent = "No tienes notificaciones.";
            return;
        }
        notificaciones.forEach(notificacion => {
            const elemento = document.createElement("article");
            elemento.className = notificacion.leida
                ? "notificacion"
                : "notificacion notificacion-no-leida";
            elemento.innerHTML = `<strong>${escapar(notificacion.titulo)}</strong><p>${escapar(notificacion.mensaje)}</p>`;
            if (!notificacion.leida) {
                const boton = document.createElement("button");
                boton.className = "btn btn-pequeno";
                boton.textContent = "Marcar como leída";
                boton.addEventListener("click", async () => {
                    await solicitar(`/api/supervision/usuarios/notificaciones/${notificacion.idNotificacion}/leida`, { method: "PUT" });
                    boton.disabled = true;
                    elemento.classList.remove("notificacion-no-leida");
                });
                elemento.appendChild(boton);
            }
            contenedor.appendChild(elemento);
        });
    }

    async function solicitar(url, opciones = {}) {
        const respuesta = await fetch(url, {
            ...opciones,
            credentials: "same-origin",
            headers: { "Content-Type": "application/json", ...(opciones.headers || {}) }
        });
        const datos = await respuesta.json().catch(() => ({}));
        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo completar la operación.");
        }
        return datos;
    }

    function escapar(valor) {
        return String(valor).replace(/[&<>"']/g, caracter => ({
            "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;"
        }[caracter]));
    }
});
