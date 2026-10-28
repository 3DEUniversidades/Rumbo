document.addEventListener("DOMContentLoaded", async function () {
    const lista = document.getElementById("lista-entradas");
    const mensaje = document.getElementById("mensaje-bitacora");
    const formulario = document.getElementById("formulario-entrada");
    const contenido = document.getElementById("contenido-entrada");
    const actividad = document.getElementById("actividad-entrada");
    const botonGuardar = document.getElementById("guardar-entrada");
    const mensajeNueva = document.getElementById("mensaje-nueva-entrada");
    const dialogo = document.getElementById("dialogo-editar-entrada");
    const formularioEditar = document.getElementById("formulario-editar-entrada");
    const contenidoEditar = document.getElementById("contenido-editar");
    const actividadEditar = document.getElementById("actividad-editar");
    const mensajeEditar = document.getElementById("mensaje-editar-entrada");
    const botonGuardarEdicion = document.getElementById("guardar-edicion");
    const botonCancelarEdicion = document.getElementById("cancelar-edicion");

    let actividades = [];
    let entradaEnEdicion = null;

    function mostrarMensaje(texto, clase) {
        mensaje.textContent = texto;
        mensaje.className = clase || "estado-vista";
    }

    async function consultarJson(ruta, opciones) {
        const respuesta = await fetch(ruta, {
            credentials: "same-origin",
            cache: "no-store",
            ...opciones
        });

        if (respuesta.status === 401) {
            window.location.replace("/login.html");
            throw new Error("La sesión terminó. Inicia sesión nuevamente.");
        }

        if (respuesta.status === 403) {
            throw new Error("No tienes permiso para acceder a la bitácora.");
        }

        if (respuesta.status === 204) {
            return null;
        }

        if (!respuesta.ok) {
            const cuerpo = await respuesta.json().catch(() => null);
            const detalle = cuerpo && cuerpo.campos
                ? Object.values(cuerpo.campos).join(" ")
                : cuerpo && cuerpo.error;

            throw new Error(detalle || "No se pudo completar la operación.");
        }

        return respuesta.json();
    }

    function llenarSelector(select) {
        select.replaceChildren();

        const sinActividad = document.createElement("option");
        sinActividad.value = "";
        sinActividad.textContent = "Sin actividad relacionada";
        select.appendChild(sinActividad);

        actividades.forEach(function (item) {
            const opcion = document.createElement("option");
            opcion.value = item.idActividad;
            opcion.textContent = item.fecha
                ? `${item.descripcion} (${item.fecha})`
                : item.descripcion;
            select.appendChild(opcion);
        });
    }

    async function cargarActividades() {
        const datos = await consultarJson("/api/mi-progreso/actividades");
        actividades = Array.isArray(datos) ? datos : [];

        llenarSelector(actividad);
        llenarSelector(actividadEditar);
    }

    async function cargarEntradas() {
        const entradas = await consultarJson("/api/bitacora");
        lista.replaceChildren();

        if (!entradas || entradas.length === 0) {
            const vacio = document.createElement("p");
            vacio.className = "texto-secundario";
            vacio.textContent =
                "Todavía no tienes entradas. Escribe tu primera reflexión.";
            lista.appendChild(vacio);
            return;
        }

        entradas.forEach(function (entrada) {
            lista.appendChild(crearTarjeta(entrada));
        });
    }

    function crearTarjeta(entrada) {
        const tarjeta = document.createElement("article");
        tarjeta.className = "perfil-card";

        const encabezado = document.createElement("div");
        encabezado.className = "perfil-card-encabezado";

        const titulo = document.createElement("h3");
        titulo.textContent = "Entrada de bitácora";

        const acciones = document.createElement("div");
        acciones.className = "acciones-form";

        const botonEditar = document.createElement("button");
        botonEditar.type = "button";
        botonEditar.className = "btn btn-secundario btn-pequeno";
        botonEditar.textContent = "Editar";
        botonEditar.addEventListener("click", function () {
            abrirEdicion(entrada);
        });

        const botonEliminar = document.createElement("button");
        botonEliminar.type = "button";
        botonEliminar.className = "btn btn-secundario btn-pequeno";
        botonEliminar.textContent = "Eliminar";
        botonEliminar.addEventListener("click", function () {
            eliminarEntrada(entrada);
        });

        acciones.append(botonEditar, botonEliminar);
        encabezado.append(titulo, acciones);

        const fechas = document.createElement("p");
        fechas.className = "texto-secundario";
        fechas.textContent = entrada.fechaEdicion
            ? `Creada: ${entrada.fechaCreacion} · Editada: ${entrada.fechaEdicion}`
            : `Creada: ${entrada.fechaCreacion}`;

        const texto = document.createElement("p");
        texto.className = "entrada-bitacora-contenido";
        texto.textContent = entrada.contenido;

        tarjeta.append(encabezado, fechas, texto);

        if (entrada.idActividad) {
            const actividadRelacionada = document.createElement("p");
            actividadRelacionada.className = "texto-secundario";
            actividadRelacionada.textContent =
                `Actividad relacionada: ${entrada.descripcionActividad}`;
            tarjeta.appendChild(actividadRelacionada);
        }

        return tarjeta;
    }

    function abrirEdicion(entrada) {
        entradaEnEdicion = entrada;
        contenidoEditar.value = entrada.contenido;
        actividadEditar.value = entrada.idActividad || "";
        mensajeEditar.textContent = "";
        mensajeEditar.className = "mensaje-formulario";
        dialogo.showModal();
        contenidoEditar.focus();
    }

    function cerrarEdicion() {
        entradaEnEdicion = null;
        dialogo.close();
    }

    async function eliminarEntrada(entrada) {
        if (!window.confirm(
            "¿Eliminar esta entrada de tu bitácora? Esta acción no se puede deshacer."
        )) {
            return;
        }

        try {
            await consultarJson(
                `/api/bitacora/${encodeURIComponent(entrada.idEntrada)}`,
                { method: "DELETE" }
            );
            await cargarEntradas();
            mostrarMensaje(
                "Entrada eliminada correctamente.",
                "mensaje-formulario mensaje-exito"
            );
        } catch (error) {
            mostrarMensaje(
                error.message,
                "mensaje-formulario mensaje-error"
            );
        }
    }

    formulario.addEventListener("submit", async function (evento) {
        evento.preventDefault();

        const texto = contenido.value.trim();

        if (texto === "") {
            mensajeNueva.textContent = "El contenido es obligatorio.";
            mensajeNueva.className = "mensaje-formulario mensaje-error";
            contenido.focus();
            return;
        }

        botonGuardar.disabled = true;
        mensajeNueva.textContent = "Guardando...";
        mensajeNueva.className = "mensaje-formulario";

        try {
            await consultarJson("/api/bitacora", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    contenido: texto,
                    idActividad: actividad.value || null
                })
            });

            formulario.reset();
            actividad.value = "";
            mensajeNueva.textContent = "Entrada guardada correctamente.";
            mensajeNueva.className =
                "mensaje-formulario mensaje-exito";
            await cargarEntradas();
        } catch (error) {
            mensajeNueva.textContent = error.message;
            mensajeNueva.className = "mensaje-formulario mensaje-error";
        } finally {
            botonGuardar.disabled = false;
        }
    });

    formularioEditar.addEventListener("submit", async function (evento) {
        evento.preventDefault();

        if (!entradaEnEdicion) {
            return;
        }

        const texto = contenidoEditar.value.trim();

        if (texto === "") {
            mensajeEditar.textContent = "El contenido es obligatorio.";
            mensajeEditar.className = "mensaje-formulario mensaje-error";
            contenidoEditar.focus();
            return;
        }

        const actividadElegida = actividadEditar.value || null;
        const actividadActual = entradaEnEdicion.idActividad || null;
        const contenidoCambio = texto !== entradaEnEdicion.contenido;
        const actividadCambio = actividadElegida !== actividadActual;

        if (!contenidoCambio && !actividadCambio) {
            mensajeEditar.textContent = "No hay cambios para guardar.";
            mensajeEditar.className = "mensaje-formulario";
            return;
        }

        botonGuardarEdicion.disabled = true;
        mensajeEditar.textContent = "Guardando...";
        mensajeEditar.className = "mensaje-formulario";

        try {
            const ruta =
                `/api/bitacora/${encodeURIComponent(entradaEnEdicion.idEntrada)}`;

            if (contenidoCambio) {
                await consultarJson(ruta, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ contenido: texto })
                });
            }

            if (actividadCambio) {
                await consultarJson(`${ruta}/actividad`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ idActividad: actividadElegida })
                });
            }

            cerrarEdicion();
            await cargarEntradas();
            mostrarMensaje(
                "Entrada actualizada correctamente.",
                "mensaje-formulario mensaje-exito"
            );
        } catch (error) {
            mensajeEditar.textContent = error.message;
            mensajeEditar.className = "mensaje-formulario mensaje-error";
        } finally {
            botonGuardarEdicion.disabled = false;
        }
    });

    botonCancelarEdicion.addEventListener("click", cerrarEdicion);

    dialogo.addEventListener("click", function (evento) {
        if (evento.target === dialogo) {
            cerrarEdicion();
        }
    });

    try {
        const sesion = await obtenerSesion();

        if (!sesion) {
            return;
        }

        if (String(sesion.rol || "").toUpperCase() !== "ESTUDIANTE") {
            mostrarMensaje(
                "Esta sección está disponible solo para estudiantes.",
                "mensaje-formulario mensaje-error"
            );
            formulario.querySelectorAll("textarea, select, button")
                .forEach(function (elemento) {
                    elemento.disabled = true;
                });
            return;
        }

        await cargarActividades();
        await cargarEntradas();
    } catch (error) {
        mostrarMensaje(
            error.message,
            "mensaje-formulario mensaje-error"
        );
    }
});
