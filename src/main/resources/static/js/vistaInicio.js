document.addEventListener("DOMContentLoaded", async () => {
    const acciones = document.getElementById("acciones");
    const mensaje = document.getElementById("mensaje-inicio");
    const botonCerrarSesion = document.getElementById("cerrar-sesion");
    const enlaceForo = document.getElementById("enlace-foro");
    const enlaceLineaTiempo =
    document.getElementById("enlace-linea-tiempo");

    acciones.hidden = true;

    try {
        const sesion = await obtenerSesion();
        if (!sesion) return;

        const rol = String(sesion.rol || "").toUpperCase();
        enlaceLineaTiempo.hidden = rol !== "ESTUDIANTE";

        document.getElementById("saludo").textContent =
            "Hola, " + sesion.nombreCompleto;
        document.getElementById("encabezado-usuario").textContent =
            "Orientación vocacional para " + sesion.nombreCompleto;

        await mostrarForoSiEstaVerificado();

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
});