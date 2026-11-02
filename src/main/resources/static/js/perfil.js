document.addEventListener("DOMContentLoaded", async function () {
    const contenido = document.getElementById("contenido-perfil");
    const error = document.getElementById("error-perfil");
    const mensaje = document.getElementById("mensaje-perfil");
    const formulario = document.getElementById("formulario-perfil");
    const botonGuardar = document.getElementById("guardar-perfil");
    const carrera = document.getElementById("carrera");
    const areaLaboral = document.getElementById("area-laboral");
    const etiquetaArea = document.getElementById("etiqueta-area-laboral");
    const etiquetaCarrera = document.getElementById("etiqueta-carrera");
    const seccionEdicion = document.getElementById("seccion-edicion-perfil");
    const formularioSolicitud = document.getElementById("formulario-solicitud-perfil");
    const estadoSolicitud = document.getElementById("estado-solicitud-perfil");
    const botonSolicitud = document.getElementById("solicitar-cambio-perfil");
    const botonEditarCuenta = document.getElementById("editar-datos-cuenta");
    const cancelarEdicionCuenta = document.getElementById("cancelar-edicion-cuenta");
    const resumenCuenta = document.getElementById("resumen-datos-cuenta");
    const campoNombreUsuario = document.getElementById("solicitud-nombre-usuario");
    const campoNombreCompleto = document.getElementById("solicitud-nombre-completo");
    const seccionFotoPerfil = document.getElementById("seccion-foto-perfil");
    const fotoPerfil = document.getElementById("foto-perfil");
    const fotoIniciales = document.getElementById("foto-perfil-iniciales");
    const archivoFotoPerfil = document.getElementById("archivo-foto-perfil");
    const seleccionarFotoPerfil = document.getElementById("seleccionar-foto-perfil");
    const subirFotoPerfil = document.getElementById("subir-foto-perfil");
    const eliminarFotoPerfil = document.getElementById("eliminar-foto-perfil");
    const mensajeFotoPerfil = document.getElementById("mensaje-foto-perfil");
    const cancelarFotoPerfil = document.getElementById("cancelar-foto-perfil");
    const MAX_FOTO_BYTES = 5 * 1024 * 1024;
    let solicitudPendiente = false;
    let perfilActual = null;
    let archivoFotoSeleccionado = null;
    let urlVistaPrevia = null;

    const cargarSolicitudPendiente = async function () {
        const respuesta = await fetch("/api/perfil/solicitudes/pendiente");
        if (respuesta.status === 204) {
            return;
        }
        if (!respuesta.ok) {
            throw new Error("No se pudo consultar la solicitud pendiente.");
        }
        const texto = await respuesta.text();
        if (!texto.trim()) {
            return;
        }
        const solicitud = JSON.parse(texto);
        if (!solicitud) {
            return;
        }
        solicitudPendiente = true;
        campoNombreUsuario.value =
            solicitud.nombreUsuarioSolicitado;
        campoNombreCompleto.value =
            solicitud.nombreCompletoSolicitado;
        estadoSolicitud.textContent =
            "Ya tienes una solicitud pendiente de revisión.";
        campoNombreUsuario.disabled = true;
        campoNombreCompleto.disabled = true;
        botonSolicitud.hidden = true;
        botonSolicitud.disabled = true;
        botonEditarCuenta.hidden = false;
        botonEditarCuenta.disabled = true;
        botonEditarCuenta.textContent = "Solicitud pendiente";
        formularioSolicitud.hidden = true;
        resumenCuenta.hidden = false;
    };

    seleccionarFotoPerfil.addEventListener("click", function () {
        archivoFotoPerfil.click();
    });

    archivoFotoPerfil.addEventListener("change", function () {
        const archivo = archivoFotoPerfil.files[0];
        if (!archivo) return;
        mensajeFotoPerfil.textContent = "";
        mensajeFotoPerfil.className = "mensaje-formulario";
        archivoFotoSeleccionado = null;
        subirFotoPerfil.disabled = true;
        mostrarFotoPerfil(perfilActual);

        const tiposPermitidos = ["image/jpeg", "image/png", "image/webp"];
        if (archivo.type && !tiposPermitidos.includes(archivo.type)) {
            mensajeFotoPerfil.textContent = "Usa una imagen JPG, PNG o WebP.";
            mensajeFotoPerfil.className = "mensaje-formulario mensaje-error";
            archivoFotoPerfil.value = "";
            return;
        }

        if (archivo.size > MAX_FOTO_BYTES) {
            mensajeFotoPerfil.textContent = "La foto no puede superar los 5 MB.";
            mensajeFotoPerfil.className = "mensaje-formulario mensaje-error";
            archivoFotoPerfil.value = "";
            return;
        }

        if (urlVistaPrevia) {
            URL.revokeObjectURL(urlVistaPrevia);
        }

        urlVistaPrevia = URL.createObjectURL(archivo);
        fotoPerfil.src = urlVistaPrevia;
        fotoPerfil.hidden = false;
        fotoIniciales.hidden = true;
        archivoFotoSeleccionado = archivo;
        subirFotoPerfil.disabled = false;
        subirFotoPerfil.hidden = false;
        cancelarFotoPerfil.hidden = false;
        eliminarFotoPerfil.hidden = true;
        mensajeFotoPerfil.textContent = "Vista previa. Guarda la foto para aplicar el cambio.";
    });

    cancelarFotoPerfil.addEventListener("click", function () {
        archivoFotoSeleccionado = null;
        archivoFotoPerfil.value = "";
        mostrarFotoPerfil(perfilActual);
        mensajeFotoPerfil.textContent = "";
        seleccionarFotoPerfil.focus();
    });

    function bloquearFoto(ocupado) {
        seleccionarFotoPerfil.disabled = ocupado;
        archivoFotoPerfil.disabled = ocupado;
        cancelarFotoPerfil.disabled = ocupado;
        eliminarFotoPerfil.disabled = ocupado;
        subirFotoPerfil.disabled = ocupado || !archivoFotoSeleccionado;
    }

    subirFotoPerfil.addEventListener("click", async function () {
        if (!archivoFotoSeleccionado) {
            return;
        }

        bloquearFoto(true);
        mensajeFotoPerfil.textContent = "Subiendo foto...";
        mensajeFotoPerfil.className = "mensaje-formulario";

        const formularioFoto = new FormData();
        formularioFoto.append("archivo", archivoFotoSeleccionado);

        try {
            const respuesta = await fetch("/api/perfil/foto", {
                method: "POST",
                body: formularioFoto
            });

            if (respuesta.status === 401) {
                window.location.replace("/login.html");
                return;
            }

            const cuerpo = await respuesta.json().catch(() => ({}));
            if (!respuesta.ok) {
                throw new Error(cuerpo.error || "No se pudo subir la foto.");
            }

            perfilActual = { ...perfilActual, ...cuerpo };
            archivoFotoSeleccionado = null;
            archivoFotoPerfil.value = "";
            mostrarFotoPerfil(perfilActual);
            mensajeFotoPerfil.textContent = "Foto actualizada correctamente.";
            mensajeFotoPerfil.className = "mensaje-formulario mensaje-exito";
        } catch (exception) {
            mensajeFotoPerfil.textContent = exception.message;
            mensajeFotoPerfil.className = "mensaje-formulario mensaje-error";
        } finally {
            bloquearFoto(false);
            if (!archivoFotoSeleccionado) seleccionarFotoPerfil.focus();
        }
    });

    eliminarFotoPerfil.addEventListener("click", async function () {
        if (!window.confirm("¿Eliminar tu foto de perfil?")) {
            return;
        }

        bloquearFoto(true);
        mensajeFotoPerfil.textContent = "Eliminando foto...";
        mensajeFotoPerfil.className = "mensaje-formulario";

        try {
            const respuesta = await fetch("/api/perfil/foto", {
                method: "DELETE"
            });

            if (respuesta.status === 401) {
                window.location.replace("/login.html");
                return;
            }

            const cuerpo = await respuesta.json().catch(() => ({}));
            if (!respuesta.ok) {
                throw new Error(cuerpo.error || "No se pudo eliminar la foto.");
            }

            perfilActual = { ...perfilActual, ...cuerpo };
            archivoFotoSeleccionado = null;
            archivoFotoPerfil.value = "";
            subirFotoPerfil.disabled = true;
            mostrarFotoPerfil(perfilActual);
            mensajeFotoPerfil.textContent = "Foto eliminada correctamente.";
            mensajeFotoPerfil.className = "mensaje-formulario mensaje-exito";
        } catch (exception) {
            mensajeFotoPerfil.textContent = exception.message;
            mensajeFotoPerfil.className = "mensaje-formulario mensaje-error";
        } finally {
            bloquearFoto(false);
            seleccionarFotoPerfil.focus();
        }
    });

    try {
        const sesion = await obtenerSesion();
        if (!sesion) {
            return;
        }
        const perfil = await consultarPerfil();
        if (!perfil) return;
        perfilActual = perfil;
        mostrarCuenta(perfil);
        mostrarFotoPerfil(perfil);
        const rol = String(sesion.rol || "").toUpperCase();
        const puedeEditar = rol === "ESTUDIANTE"
            || rol === "COLABORADOR";

        seccionFotoPerfil.hidden = !puedeEditar;

        if (!puedeEditar) {
            seccionEdicion.hidden = true;
        } else {
            botonEditarCuenta.hidden = false;
            try {
                await cargarSolicitudPendiente();
            } catch (exception) {
                estadoSolicitud.textContent = exception.message;
                estadoSolicitud.className = "mensaje-formulario mensaje-error";
            }
            mostrarFormulario(perfil, [], rol);
        }

        contenido.hidden = false;
        document.getElementById("cargando-perfil").hidden = true;

        if (puedeEditar) {
            try {
                const carreras = await consultarCarreras();
                mostrarCarreras(carreras, perfil);
            } catch (exception) {
                mostrarMensaje(
                    exception.message || "No se pudo cargar el catálogo de carreras."
                );
            }
        }
    } catch (exception) {
        mostrarMensaje(exception.message || "No se pudo cargar el perfil.");
    } finally {
        document.getElementById("cargando-perfil").hidden = true;
    }

    formulario.addEventListener("submit", async function (evento) {
        evento.preventDefault();
        botonGuardar.disabled = true;
        mensaje.textContent = "Guardando...";
        mensaje.className = "mensaje-formulario";

        const datos = {
            nivelEstudios: document.getElementById("nivel-estudios").value,
            carreraId: carrera.value ? Number(carrera.value) : null,
            areaLaboral: areaLaboral.value,
            descripcionPersonal: document.getElementById("descripcion-personal").value
        };

        try {
            const respuesta = await fetch("/api/perfil", {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(datos)
            });

            if (respuesta.status === 401) {
                window.location.replace("/login.html");
                return;
            }
            const cuerpo = await respuesta.json().catch(() => ({}));
            if (!respuesta.ok) {
                throw new Error(cuerpo.error || "No se pudo guardar el perfil.");
            }
            perfilActual = { ...perfilActual, ...cuerpo };
            mensaje.textContent = "Perfil guardado correctamente.";
            mensaje.className = "mensaje-formulario mensaje-exito";
        } catch (exception) {
            mensaje.textContent = exception.message;
            mensaje.className = "mensaje-formulario mensaje-error";
        } finally {
            botonGuardar.disabled = false;
        }
    });

    botonEditarCuenta.addEventListener("click", function () {
        if (solicitudPendiente) {
            estadoSolicitud.textContent =
                "Ya tienes una solicitud pendiente de revisión.";
            estadoSolicitud.className = "mensaje-formulario";
            return;
        }
        campoNombreUsuario.disabled = false;
        campoNombreCompleto.disabled = false;
        formularioSolicitud.hidden = false;
        resumenCuenta.hidden = true;
        botonEditarCuenta.setAttribute("aria-expanded", "true");
        botonSolicitud.hidden = false;
        botonSolicitud.disabled = false;
        botonEditarCuenta.hidden = true;
        botonEditarCuenta.textContent = "Editando";
        estadoSolicitud.textContent =
            "Modifica tus datos y después guarda los cambios.";
        estadoSolicitud.className = "mensaje-formulario";
        campoNombreUsuario.focus();
    });

    cancelarEdicionCuenta.addEventListener("click", function () {
        campoNombreUsuario.value = perfilActual.nombreUsuario || "";
        campoNombreCompleto.value = perfilActual.nombreCompleto || "";
        cerrarEdicionCuenta();
        estadoSolicitud.textContent = "";
        botonEditarCuenta.focus();
    });

    function cerrarEdicionCuenta() {
        formularioSolicitud.hidden = true;
        resumenCuenta.hidden = false;
        campoNombreUsuario.disabled = true;
        campoNombreCompleto.disabled = true;
        botonEditarCuenta.hidden = false;
        botonEditarCuenta.setAttribute("aria-expanded", "false");
        botonEditarCuenta.textContent = solicitudPendiente ? "Solicitud pendiente" : "Editar datos";
        botonEditarCuenta.disabled = solicitudPendiente;
    }

    async function consultarPerfil() {
        const respuesta = await fetch("/api/perfil");
        if (respuesta.status === 401) {
            window.location.replace("/login.html");
            return null;
        }
        if (!respuesta.ok) {
            throw new Error("No se pudo consultar el perfil.");
        }
        return respuesta.json();
    }

    async function consultarCarreras() {
        const respuesta = await fetch("/api/carreras");
        if (!respuesta.ok) {
            throw new Error("No se pudo cargar el catálogo de carreras.");
        }
        return respuesta.json();
    }

    formularioSolicitud.addEventListener("submit", async function (evento) {
        evento.preventDefault();
        botonSolicitud.disabled = true;
        cancelarEdicionCuenta.disabled = true;
        estadoSolicitud.textContent = "Enviando solicitud...";
        estadoSolicitud.className = "mensaje-formulario";

        try {
            const respuesta = await fetch("/api/perfil/solicitudes", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    nombreUsuario: campoNombreUsuario.value.trim(),
                    nombreCompleto: campoNombreCompleto.value.trim()
                })
            });
            const cuerpo = await respuesta.json().catch(() => ({}));
            if (respuesta.status === 401) {
                window.location.replace("/login.html");
                return;
            }
            if (!respuesta.ok) {
                throw new Error(cuerpo.error || "No se pudo crear la solicitud.");
            }
            estadoSolicitud.textContent =
                "Solicitud enviada y pendiente de revisión.";
            estadoSolicitud.className = "mensaje-formulario mensaje-exito";
            solicitudPendiente = true;
            campoNombreUsuario.disabled = true;
            campoNombreCompleto.disabled = true;
            botonSolicitud.hidden = true;
            botonSolicitud.disabled = true;
            botonEditarCuenta.hidden = false;
            botonEditarCuenta.disabled = true;
            botonEditarCuenta.textContent = "Solicitud pendiente";
            cerrarEdicionCuenta();
            estadoSolicitud.setAttribute("tabindex", "-1");
            estadoSolicitud.focus();
        } catch (exception) {
            estadoSolicitud.textContent = exception.message;
            estadoSolicitud.className = "mensaje-formulario mensaje-error";
            botonSolicitud.disabled = false;
        } finally {
            cancelarEdicionCuenta.disabled = false;
        }
    });

    function mostrarCuenta(perfil) {
        campoNombreUsuario.value = perfil.nombreUsuario || "";
        campoNombreCompleto.value = perfil.nombreCompleto || "";
        document.getElementById("perfil-correo").textContent = perfil.correo;
        const roles = {
            ESTUDIANTE: "Estudiante", COLABORADOR: "Colaborador", ADMIN: "Administrador",
            ENCARGADO_DE_CARRERAS: "Encargado de carreras",
            SUPERVISOR_DE_MENSAJES: "Supervisor de mensajes",
            SUPERVISOR_DE_USUARIOS: "Supervisor de usuarios"
        };
        document.getElementById("perfil-rol").textContent = roles[perfil.rol] || perfil.rol;
        document.getElementById("titulo-perfil").textContent = perfil.nombreCompleto;
        document.getElementById("perfil-usuario-cabecera").textContent = "@" + perfil.nombreUsuario;
        document.getElementById("perfil-nombre-completo").textContent = perfil.nombreCompleto;
        document.getElementById("perfil-nombre-usuario").textContent = perfil.nombreUsuario;
    }

    function mostrarFotoPerfil(perfil) {
        subirFotoPerfil.hidden = true;
        subirFotoPerfil.disabled = true;
        cancelarFotoPerfil.hidden = true;
        if (urlVistaPrevia) {
            URL.revokeObjectURL(urlVistaPrevia);
            urlVistaPrevia = null;
        }

        const fotoUrl = perfil && perfil.fotoUrl;
        seleccionarFotoPerfil.textContent = fotoUrl ? "Cambiar foto" : "Añadir foto";
        if (fotoUrl) {
            fotoPerfil.src = fotoUrl;
            fotoPerfil.hidden = false;
            fotoIniciales.hidden = true;
            eliminarFotoPerfil.hidden = false;
            eliminarFotoPerfil.disabled = false;
        } else {
            fotoPerfil.removeAttribute("src");
            fotoPerfil.hidden = true;
            fotoIniciales.hidden = false;
            fotoIniciales.textContent = calcularIniciales(perfil && perfil.nombreCompleto);
            eliminarFotoPerfil.hidden = true;
        }
    }

    function calcularIniciales(nombreCompleto) {
        if (!nombreCompleto) {
            return "RU";
        }
        const partes = nombreCompleto.trim().split(/\s+/);
        if (partes.length === 1) {
            return partes[0].substring(0, 2).toUpperCase();
        }
        return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
    }

    function mostrarFormulario(perfil, carreras, rol) {
        document.getElementById("nivel-estudios").value = perfil.nivelEstudios || "";
        document.getElementById("descripcion-personal").value = perfil.descripcionPersonal || "";
        areaLaboral.value = perfil.areaLaboral || "";
        const esColaborador = rol === "COLABORADOR";
        etiquetaCarrera.textContent = esColaborador ? "Carrera" : "Carrera de interés";
        areaLaboral.required = esColaborador;
        areaLaboral.hidden = !esColaborador;
        etiquetaArea.hidden = !esColaborador;
        document.getElementById("descripcion-personal").previousElementSibling.textContent =
            "Descripción personal";

        carreras.forEach(function (opcion) {
            agregarCarrera(opcion, perfil);
        });
    }

    function mostrarCarreras(carreras, perfil) {
        carreras.forEach(function (opcion) {
            agregarCarrera(opcion, perfil);
        });
    }

    function agregarCarrera(opcion, perfil) {
        const elemento = document.createElement("option");
        elemento.value = opcion.id;
        elemento.textContent = opcion.nombre;
        elemento.selected = opcion.id === perfil.carreraId;
        carrera.appendChild(elemento);
    }

    function mostrarMensaje(texto) {
        error.textContent = texto;
        error.hidden = false;
    }
});
