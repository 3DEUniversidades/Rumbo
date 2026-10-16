document.addEventListener("DOMContentLoaded", () => {
    const panel = document.getElementById("contenido-rol");
    if (!panel) return;

    const aviso = document.createElement("p");
    aviso.setAttribute("role", "status");
    aviso.className = "rumbo-estado";
    panel.before(aviso);

    const reintentar = document.createElement("button");
    reintentar.type = "button";
    reintentar.className = "btn btn-secundario";
    reintentar.textContent = "Reintentar";
    reintentar.hidden = true;
    aviso.after(reintentar);

    reintentar.addEventListener("click", () => {
        window.location.reload();
    });

    document.getElementById("cerrar-sesion")
        .addEventListener("click", async function () {
            this.disabled = true;

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
                this.disabled = false;
                aviso.textContent = error.message;
            }
        });

    cargar();

    async function cargar() {
        panel.hidden = true;
        reintentar.hidden = true;
        aviso.textContent = "Cargando sesión...";

        try {
            const sesion = await obtenerSesion();
            if (!sesion) return;

            document.getElementById("nombre-usuario").textContent =
                sesion.nombreCompleto;

            document.getElementById("rol-usuario").textContent =
                sesion.rol;

            if (!panel.querySelector('a[href="/inicio.html"]')) {
                const enlace = document.createElement("a");
                enlace.href = "/inicio.html";
                enlace.className = "btn btn-secundario";
                enlace.textContent = "Ir al inicio";
                panel.appendChild(enlace);
            }

            aviso.textContent = "";
            panel.hidden = false;

        } catch (error) {
            aviso.textContent =
                error.message || "No se pudo cargar la sesión.";

            reintentar.hidden = false;
        }
    }
});