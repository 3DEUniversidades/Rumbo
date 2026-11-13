document.addEventListener("DOMContentLoaded", async function () {
    const lista = document.getElementById("lista-entradas");
    const mensaje = document.getElementById("mensaje-bitacora");
    const formulario = document.getElementById("formulario-entrada");
    const contenido = document.getElementById("contenido-entrada");
    const actividad = document.getElementById("actividad-entrada");
    const archivosEntrada = document.getElementById("archivos-entrada");
    const botonGuardar = document.getElementById("guardar-entrada");
    const mensajeNueva = document.getElementById("mensaje-nueva-entrada");
    const dialogo = document.getElementById("dialogo-editar-entrada");
    const formularioEditar = document.getElementById("formulario-editar-entrada");
    const contenidoEditar = document.getElementById("contenido-editar");
    const actividadEditar = document.getElementById("actividad-editar");
    const mensajeEditar = document.getElementById("mensaje-editar-entrada");
    const botonGuardarEdicion = document.getElementById("guardar-edicion");
    const botonCancelarEdicion = document.getElementById("cancelar-edicion");
    const botonPdf = document.getElementById("descargar-bitacora-pdf");
    const botonZip = document.getElementById("descargar-bitacora-zip");
    const estadoExportacion = document.getElementById("estado-exportacion-bitacora");
    const seccionExportacion = document.getElementById("exportacion-bitacora");

    let actividades = [];
    let entradaEnEdicion = null;
    let hayEntradas = false;
    let exportando = false;

    function actualizarBotonesExportacion() {
        botonPdf.disabled = !hayEntradas || exportando;
        botonZip.disabled = !hayEntradas || exportando;
    }

    async function descargarExportacion(formato) {
        if (!hayEntradas || exportando) return;
        exportando = true;
        actualizarBotonesExportacion();
        seccionExportacion.setAttribute("aria-busy", "true");
        estadoExportacion.className = "mensaje-formulario";
        estadoExportacion.textContent = formato === "zip"
            ? "Preparando el PDF y recuperando tus adjuntos. Puede tardar unos momentos…"
            : "Preparando el PDF de tu bitácora…";

        try {
            const respuesta = await fetch(`/api/bitacora/exportar/${formato}`, {
                credentials: "same-origin",
                cache: "no-store"
            });
            if (respuesta.status === 401) {
                window.location.replace("/login.html");
                return;
            }
            if (!respuesta.ok) {
                const cuerpo = await respuesta.json().catch(() => ({}));
                const detalle = cuerpo.campos ? Object.values(cuerpo.campos).join(" ") : cuerpo.error;
                throw new Error(detalle || "No se pudo generar la descarga. Intenta nuevamente.");
            }
            const tipoEsperado = formato === "pdf" ? "application/pdf" : "application/zip";
            if (!(respuesta.headers.get("Content-Type") || "").startsWith(tipoEsperado)) {
                throw new Error("La descarga no tiene el formato esperado. Intenta nuevamente.");
            }
            const archivo = await respuesta.blob();
            const url = URL.createObjectURL(archivo);
            const enlace = document.createElement("a");
            enlace.href = url;
            enlace.download = `bitacora.${formato}`;
            enlace.hidden = true;
            document.body.appendChild(enlace);
            enlace.click();
            enlace.remove();
            // Permite al navegador iniciar la descarga antes de liberar el recurso.
            window.setTimeout(() => URL.revokeObjectURL(url), 60000);
            estadoExportacion.textContent = "Archivo listo. Se inició la descarga.";
            estadoExportacion.className = "mensaje-formulario mensaje-exito";
        } catch (error) {
            estadoExportacion.textContent = error.message || "No se pudo descargar la bitácora.";
            estadoExportacion.className = "mensaje-formulario mensaje-error";
        } finally {
            exportando = false;
            seccionExportacion.setAttribute("aria-busy", "false");
            actualizarBotonesExportacion();
        }
    }

    botonPdf.addEventListener("click", () => descargarExportacion("pdf"));
    botonZip.addEventListener("click", () => descargarExportacion("zip"));

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
        hayEntradas = Array.isArray(entradas) && entradas.length > 0;
        actualizarBotonesExportacion();
        if (!exportando) {
            estadoExportacion.textContent = hayEntradas ? ""
                : "Guarda tu primera entrada para descargar tu bitácora.";
            estadoExportacion.className = "mensaje-formulario";
        }
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

    async function subirAdjuntos(idEntrada, archivos) {
        for (const archivo of Array.from(archivos)) {
            const formularioArchivo = new FormData();
            formularioArchivo.append("archivo", archivo);

            await consultarJson(
                `/api/bitacora/${encodeURIComponent(idEntrada)}/adjuntos`,
                {
                    method: "POST",
                    body: formularioArchivo
                }
            );
        }
    }

    function crearListaAdjuntos(entrada) {
        const adjuntos = Array.isArray(entrada.adjuntos)
            ? entrada.adjuntos
            : [];

        if (adjuntos.length === 0) {
            return null;
        }

        const bloque = document.createElement("div");
        bloque.className = "adjuntos-bitacora";

        const titulo = document.createElement("p");
        titulo.className = "texto-secundario";
        titulo.textContent = "Adjuntos";
        bloque.appendChild(titulo);

        adjuntos.forEach(function (adjunto) {
            const fila = document.createElement("div");
            fila.className = "adjunto-bitacora";

            const enlace = document.createElement("a");
            enlace.href = adjunto.urlContenido;
            enlace.target = "_blank";
            enlace.rel = "noopener";
            enlace.className = "adjunto-bitacora-enlace";

            if (adjunto.esImagen) {
                const miniatura = document.createElement("img");
                miniatura.src = adjunto.urlContenido;
                miniatura.alt = adjunto.nombreOriginal;
                miniatura.className = "adjunto-bitacora-miniatura";
                enlace.appendChild(miniatura);
            } else {
                enlace.textContent = "Abrir PDF";
            }

            const nombre = document.createElement("span");
            nombre.textContent = adjunto.nombreOriginal
                + " (" + formatearTamano(adjunto.tamanoBytes) + ")";

            const botonEliminar = document.createElement("button");
            botonEliminar.type = "button";
            botonEliminar.className = "btn btn-secundario btn-pequeno";
            botonEliminar.textContent = "Eliminar";
            botonEliminar.addEventListener("click", async function () {
                if (!window.confirm("¿Eliminar este adjunto?")) {
                    return;
                }

                try {
                    await consultarJson(
                        `/api/bitacora/${encodeURIComponent(entrada.idEntrada)}`
                            + `/adjuntos/${encodeURIComponent(adjunto.idAdjunto)}`,
                        { method: "DELETE" }
                    );
                    await cargarEntradas();
                    mostrarMensaje(
                        "Adjunto eliminado correctamente.",
                        "mensaje-formulario mensaje-exito"
                    );
                } catch (error) {
                    mostrarMensaje(
                        error.message,
                        "mensaje-formulario mensaje-error"
                    );
                }
            });

            fila.append(enlace, nombre, botonEliminar);
            bloque.appendChild(fila);
        });

        return bloque;
    }

    function formatearTamano(bytes) {
        if (!Number.isFinite(bytes) || bytes < 0) {
            return "tamaño desconocido";
        }
        if (bytes < 1024 * 1024) {
            return Math.max(1, Math.round(bytes / 1024)) + " KB";
        }
        return (bytes / (1024 * 1024)).toFixed(1) + " MB";
    }

    function crearTarjeta(entrada) {
        const tarjeta = document.createElement("article");
        tarjeta.className = "perfil-card";
        const adjuntos = Array.isArray(entrada.adjuntos)
            ? entrada.adjuntos
            : [];

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

        const inputAdjuntar = document.createElement("input");
        inputAdjuntar.type = "file";
        inputAdjuntar.multiple = true;
        inputAdjuntar.accept = "image/jpeg,image/png,image/webp,application/pdf";
        inputAdjuntar.hidden = true;

        const botonAdjuntar = document.createElement("button");
        botonAdjuntar.type = "button";
        botonAdjuntar.className = "btn btn-secundario btn-pequeno";
        botonAdjuntar.textContent = "Adjuntar archivo";
        botonAdjuntar.disabled = adjuntos.length >= 5;
        botonAdjuntar.addEventListener("click", function () {
            inputAdjuntar.click();
        });

        inputAdjuntar.addEventListener("change", async function () {
            if (inputAdjuntar.files.length === 0) {
                return;
            }

            botonAdjuntar.disabled = true;
            inputAdjuntar.disabled = true;
            mostrarMensaje("Subiendo adjunto...", "estado-vista");

            try {
                await subirAdjuntos(entrada.idEntrada, inputAdjuntar.files);
                await cargarEntradas();
                mostrarMensaje(
                    "Adjunto guardado correctamente.",
                    "mensaje-formulario mensaje-exito"
                );
            } catch (error) {
                mostrarMensaje(
                    error.message,
                    "mensaje-formulario mensaje-error"
                );
                inputAdjuntar.disabled = false;
                botonAdjuntar.disabled = adjuntos.length >= 5;
            } finally {
                inputAdjuntar.value = "";
            }
        });

        acciones.append(botonEditar, botonEliminar, botonAdjuntar);
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

        const listaAdjuntos = crearListaAdjuntos(entrada);
        if (listaAdjuntos) {
            tarjeta.appendChild(listaAdjuntos);
        }

        tarjeta.appendChild(inputAdjuntar);
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

        const archivosNuevos = Array.from(archivosEntrada.files);

        try {
            const entradaCreada = await consultarJson("/api/bitacora", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    contenido: texto,
                    idActividad: actividad.value || null
                })
            });

            formulario.reset();
            actividad.value = "";

            let mensajeGuardado = "Entrada guardada correctamente.";
            let claseGuardado = "mensaje-formulario mensaje-exito";

            if (archivosNuevos.length > 0) {
                try {
                    await subirAdjuntos(entradaCreada.idEntrada, archivosNuevos);
                    mensajeGuardado += " Adjuntos guardados.";
                } catch (errorAdjunto) {
                    mensajeGuardado = "La entrada se guardó, pero no se pudieron subir todos los adjuntos: "
                        + errorAdjunto.message;
                    claseGuardado = "mensaje-formulario mensaje-error";
                }
            }

            mensajeNueva.textContent = mensajeGuardado;
            mensajeNueva.className = claseGuardado;
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
            seccionExportacion.hidden = true;
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

        await Promise.all([cargarActividades(), cargarEntradas()]);
    } catch (error) {
        if (!hayEntradas) {
            estadoExportacion.textContent = "Las descargas estarán disponibles cuando se cargue tu bitácora.";
        }
        mostrarMensaje(
            error.message,
            "mensaje-formulario mensaje-error"
        );
    }
});
