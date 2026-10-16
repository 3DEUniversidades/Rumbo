document.addEventListener("DOMContentLoaded", async () => {
    const acciones = document.getElementById("acciones");
    acciones.hidden = true;

    try {
        const sesion = await obtenerSesion();
        if (!sesion) return;

        const rol = sesion.rol;

        document.getElementById("saludo").textContent =
            "Hola, " + sesion.nombreCompleto;

        const rutas = {
            ESTUDIANTE: "/roles/estudiante.html",
            COLABORADOR: "/roles/colaborador.html",
            ENCARGADO_DE_CARRERAS: "/roles/encargado-carreras.html",
            SUPERVISOR_DE_USUARIOS: "/roles/supervisor-usuarios.html",
            SUPERVISOR_DE_MENSAJES: "/roles/supervisor-mensajes.html",
            ADMIN: "/roles/administrador.html",
            ADMINISTRADOR: "/roles/administrador.html"
        };

        document.getElementById("enlace-panel-rol").href =
            rutas[rol] || "/login.html";

        const tienePerfil =
            rol === "ESTUDIANTE" || rol === "COLABORADOR";

        document.getElementById("enlace-perfil").hidden =
            !tienePerfil;

        document.getElementById("enlace-foro").hidden =
            !tienePerfil;

        document.getElementById("enlace-linea-tiempo").hidden =
            rol !== "ESTUDIANTE";

        document.getElementById("enlace-gestion-carreras").hidden =
            rol !== "ENCARGADO_DE_CARRERAS";

        acciones.hidden = false;

    } catch (error) {
        document.getElementById("saludo").textContent =
            error.message;
    }
});