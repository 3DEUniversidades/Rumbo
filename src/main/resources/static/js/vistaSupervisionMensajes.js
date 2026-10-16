document.addEventListener("DOMContentLoaded", () => {
    const obtener = id => document.getElementById(id);

    const panel = obtener("panel-supervision");
    const acceso = obtener("estado-acceso-supervision");
    const aviso = obtener("mensaje-supervision");
    const estado = obtener("estado-pendientes");
    const lista = obtener("lista-pendientes");
    const historial = obtener("cuerpo-revisiones");
    const actualizar = obtener("actualizar-supervision");

    let ocupado = false;

    actualizar.addEventListener("click", recargar);

    obtener("cerrar-sesion").addEventListener("click", async function () {
        this.disabled = true;

        try {
            const respuesta = await fetch("/api/auth/logout", {
                method: "POST",
                credentials: "same-origin"
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

    iniciar();

    async function iniciar() {
        try {
            const sesion = await obtenerSesion();
            if (!sesion) return;

            if (sesion.rol !== "SUPERVISOR_DE_MENSAJES") {
                window.location.replace("/inicio.html");
                return;
            }

            obtener("nombre-supervisor").textContent =
                sesion.nombreCompleto;

            acceso.textContent = "";
            panel.hidden = false;
            await recargar();

        } catch (error) {
            acceso.textContent = error.message;
        }
    }

    async function api(url, opciones = {}) {
        const respuesta = await fetch(url, {
            ...opciones,
            credentials: "same-origin",
            cache: "no-store"
        });

        const datos = await respuesta.json().catch(() => ({}));

        if (respuesta.status === 401) {
            window.location.replace("/login.html");
        }

        if (respuesta.status === 403) {
            panel.hidden = true;
            acceso.textContent = datos.error
                || "No tienes permiso para supervisar mensajes.";
        }

        if (!respuesta.ok) {
            throw new Error(
                datos.error || "No se pudo completar la operación."
            );
        }

        return datos;
    }

    async function recargar() {
        if (ocupado) return;

        bloquear(true);

        try {
            await cargarDatos();
        } finally {
            bloquear(false);
        }
    }

    async function cargarDatos() {
        estado.textContent = "Actualizando tablero...";

        try {
            const [pendientes, revisiones] = await Promise.all([
                api("/api/moderacion/foro/pendientes"),
                api("/api/moderacion/foro/mis-revisiones")
            ]);

            mostrarPendientes(pendientes);
            mostrarRevisiones(revisiones);

            estado.textContent = pendientes.length
                ? ""
                : "No hay publicaciones pendientes.";

        } catch (error) {
            estado.textContent =
                "No se pudo actualizar el tablero: " + error.message;
        }
    }

    function mostrarPendientes(publicaciones) {
        lista.replaceChildren();

        publicaciones.forEach(p => {
            const tarjeta = crear("article", "", "carrera-card");

            tarjeta.append(
                crear("h3", p.titulo),
                crear(
                    "p",
                    `${p.tipo} · @${p.autor.nombre} · ${fecha(p.fecha)}`,
                    "texto-secundario"
                ),
                crear("p", p.contenido, "supervision-contenido"),
                crear(
                    "p",
                    p.carreras.length
                        ? p.carreras.map(c => c.nombre).join(" · ")
                        : "Publicación general",
                    "carrera-area"
                )
            );

            const acciones = crear("div", "", "acciones-form");

            [
                ["Validar mensaje", "validar"],
                ["Eliminar mensaje", "eliminar"]
            ].forEach(([etiqueta, accion]) => {
                const boton = crear(
                    "button",
                    etiqueta,
                    "btn btn-pequeno accion-supervision"
                );

                if (accion === "eliminar") boton.classList.add("btn-peligro");
                boton.type = "button";
                boton.disabled = ocupado;
                boton.addEventListener(
                    "click", () => revisar(p.id, accion)
                );

                acciones.appendChild(boton);
            });

            tarjeta.appendChild(acciones);
            lista.appendChild(tarjeta);
        });
    }

    async function revisar(id, accion) {
        if (ocupado) return;

        if (accion === "eliminar"
            && !window.confirm(
                "¿Eliminar esta publicación del foro?"
            )) {
            return;
        }

        bloquear(true);
        aviso.textContent = "Guardando decisión...";

        try {
            await api(
                `/api/moderacion/foro/publicaciones/${id}/${accion}`,
                { method: "POST" }
            );

            aviso.textContent = accion === "validar"
                ? "Publicación validada. Permanece visible en el foro."
                : "Publicación eliminada del foro.";

        } catch (error) {
            aviso.textContent = error.message;

        } finally {
            await cargarDatos();
            bloquear(false);
        }
    }

    function mostrarRevisiones(revisiones) {
        historial.replaceChildren();

        obtener("estado-revisiones").textContent = revisiones.length
            ? ""
            : "Todavía no has registrado revisiones.";

        obtener("tabla-revisiones").hidden =
            revisiones.length === 0;

        revisiones.forEach(r => {
            const fila = crear("tr");

            [
                fecha(r.fecha),
                r.tituloPublicacion,
                r.decision === "VALIDAR" ? "Validada" : "Eliminada",
                r.supervisor.nombre
            ].forEach(texto => {
                fila.appendChild(crear("td", texto));
            });

            historial.appendChild(fila);
        });
    }

    function bloquear(valor) {
        ocupado = valor;
        actualizar.disabled = valor;

        document.querySelectorAll(".accion-supervision")
            .forEach(boton => {
                boton.disabled = valor;
            });
    }

    function crear(etiqueta, texto = "", clase = "") {
        const elemento = document.createElement(etiqueta);
        elemento.textContent = texto;

        if (clase) elemento.className = clase;

        return elemento;
    }

    function fecha(valor) {
        return new Date(valor).toLocaleString("es-GT", {
            dateStyle: "medium",
            timeStyle: "short"
        });
    }
});