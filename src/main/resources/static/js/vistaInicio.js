document.addEventListener("DOMContentLoaded", async () => {
    try {
        const sesion = await obtenerSesion();

        if (!sesion) return;

        document.getElementById("saludo").textContent =
            "Hola, " + sesion.nombreCompleto;

        document.getElementById("enlace-gestion-carreras").hidden =
            sesion.rol !== "ENCARGADO_DE_CARRERAS";

    } catch (error) {
        document.getElementById("saludo").textContent = error.message;
    }
});