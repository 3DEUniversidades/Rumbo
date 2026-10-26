document.addEventListener("DOMContentLoaded", async () => {
    const panel = document.getElementById("verificacion-estudiante");
    const mensaje = document.getElementById("mensaje-verificacion");
    const boton = document.getElementById("solicitar-verificacion");

    if (!panel || !mensaje || !boton) return;

    let estadoCargado = false;
    let ocupado = false;

    boton.disabled = true;

    try {
        const sesion = await obtenerSesion();

        if (!sesion || sesion.rol !== "ESTUDIANTE") return;

        panel.hidden = false;
        await cargarEstado();

    } catch (error) {
        panel.hidden = false;
        mensaje.textContent =
            error.message || "No se pudo consultar la sesión.";

        boton.hidden = false;
        boton.disabled = false;
        boton.textContent = "Reintentar";

        boton.addEventListener("click", () => {
            window.location.reload();
        });

        return;
    }

    boton.addEventListener("click", async () => {
        if (ocupado) return;

        if (!estadoCargado) {
            await cargarEstado();
            return;
        }

        ocupado = true;
        boton.disabled = true;

        try {
            await solicitar("/api/verificacion/solicitudes", {
                method: "POST"
            });

            await cargarEstado();

        } catch (error) {
            mensaje.textContent = error.message;

        } finally {
            ocupado = false;
            boton.disabled = false;
        }
    });

    async function cargarEstado() {
        boton.disabled = true;

        try {
            const estado = await solicitar("/api/verificacion/estado");

            estadoCargado = true;
            mensaje.textContent = estado.mensaje;
            boton.textContent = "Solicitar verificación";

            boton.hidden =
                estado.estadoVerificacion === "verificado"
                || estado.solicitudPendiente;

        } catch (error) {
            estadoCargado = false;
            mensaje.textContent = error.message;
            boton.hidden = false;
            boton.textContent = "Reintentar consulta";

        } finally {
            boton.disabled = false;
        }
    }

    async function solicitar(url, opciones = {}) {
        const respuesta = await fetch(url, {
            ...opciones,
            credentials: "same-origin",
            cache: "no-store"
        });

        const cuerpo = await respuesta.json().catch(() => ({}));

        if (respuesta.status === 401) {
            window.location.replace("/login.html");
            throw new Error("La sesión expiró.");
        }

        if (!respuesta.ok) {
            const detalles = cuerpo.campos
                ? Object.values(cuerpo.campos).join(" ")
                : "";

            throw new Error(
                detalles
                || cuerpo.error
                || "No se pudo completar la operación."
            );
        }

        return cuerpo;
    }
});