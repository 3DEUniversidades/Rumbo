document.addEventListener("DOMContentLoaded", async () => {
    const acciones = document.getElementById("acciones");
    const mensaje = document.getElementById("mensaje-inicio");
    const botonCerrarSesion = document.getElementById("cerrar-sesion");
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
            "Tu proceso de orientación, paso a paso.";

        iniciarRecorridoPrimeraVisita(sesion);

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

    function iniciarRecorridoPrimeraVisita(sesion) {
        const dialogo = document.getElementById("recorrido-inicial");
        const tarjetas = [...dialogo.querySelectorAll("[data-paso]")];
        const botonSiguiente = document.getElementById("recorrido-siguiente");
        const botonSaltar = document.getElementById("recorrido-saltar");
        const contador = document.getElementById("recorrido-contador");
        const indicadores = document.getElementById("recorrido-indicadores");
        const clave = `rumbo-recorrido-inicial-v2:${sesion.idUsuario}`;

        try {
            if (window.localStorage.getItem(clave) === "completado") return;
        } catch {
            // El recorrido sigue disponible si el navegador restringe el almacenamiento local.
        }

        let pasoActual = 0;
        tarjetas.forEach((_, indice) => {
            const indicador = document.createElement("span");
            indicador.className = "recorrido-indicador";
            indicador.dataset.indicador = String(indice);
            indicadores.appendChild(indicador);
        });

        function actualizarPaso() {
            tarjetas.forEach((tarjeta, indice) => {
                tarjeta.hidden = indice !== pasoActual;
            });
            indicadores.querySelectorAll(".recorrido-indicador").forEach((indicador, indice) => {
                indicador.classList.toggle("activo", indice === pasoActual);
            });

            const tarjetaActiva = tarjetas[pasoActual];
            const titulo = tarjetaActiva.querySelector("h2");
            const descripcion = tarjetaActiva.querySelector("p");
            dialogo.querySelectorAll("#recorrido-titulo-activo, #recorrido-descripcion-activa")
                .forEach(elemento => elemento.removeAttribute("id"));
            titulo.id = "recorrido-titulo-activo";
            descripcion.id = "recorrido-descripcion-activa";
            dialogo.setAttribute("aria-labelledby", titulo.id);
            dialogo.setAttribute("aria-describedby", descripcion.id);
            contador.textContent = `${pasoActual + 1} de ${tarjetas.length}`;

            const esUltimo = pasoActual === tarjetas.length - 1;
            botonSiguiente.querySelector("span").textContent = esUltimo
                ? "Entrar a Rumbo"
                : "Continuar";
            botonSiguiente.setAttribute("aria-label", esUltimo
                ? "Finalizar recorrido y entrar a Rumbo"
                : `Avanzar al paso ${pasoActual + 2}`);

            if (pasoActual === 0) {
                const personaje = tarjetaActiva.querySelector(".recorrido-mascota");
                personaje.dataset.rumboMovimiento = "saludo";
                window.setTimeout(() => delete personaje.dataset.rumboMovimiento, 900);
            }
        }

        function finalizarRecorrido() {
            try {
                window.localStorage.setItem(clave, "completado");
            } catch {
                // Si el navegador bloquea el almacenamiento, el recorrido podrá repetirse en otra visita.
            }
            dialogo.close();
            document.getElementById("saludo").focus();
        }

        botonSiguiente.addEventListener("click", () => {
            if (pasoActual === tarjetas.length - 1) {
                finalizarRecorrido();
                return;
            }
            pasoActual += 1;
            actualizarPaso();
        });

        botonSaltar.addEventListener("click", finalizarRecorrido);
        dialogo.addEventListener("cancel", evento => evento.preventDefault());
        actualizarPaso();
        dialogo.showModal();
        botonSiguiente.focus();
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
                    actualizarBadgeNotificaciones();
                });
                elemento.appendChild(boton);
            }
            lista.appendChild(elemento);
        });
    }

    function actualizarBadgeNotificaciones() {
        const badge = document.getElementById("badge-notificaciones-no-leidas");
        if (!badge) {
            return;
        }
        const noLeidas = document.querySelectorAll("#lista-notificaciones-inicio .notificacion-no-leida").length;
        badge.textContent = noLeidas > 99 ? "99+" : String(noLeidas);
        badge.hidden = noLeidas === 0;
    }
});
