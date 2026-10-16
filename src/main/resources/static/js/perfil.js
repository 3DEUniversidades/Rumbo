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

    try {
        const sesion = await obtenerSesion();
        if (!sesion) {
            return;
        }
        const perfil = await consultarPerfil();
        mostrarCuenta(perfil);
        const puedeEditar = sesion.rol === "ESTUDIANTE"
            || sesion.rol === "COLABORADOR";

        if (!puedeEditar) {
            seccionEdicion.hidden = true;
        } else {
            mostrarFormulario(perfil, [], sesion.rol);
        }

        contenido.hidden = false;

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
            mensaje.textContent = "Perfil guardado correctamente.";
            mensaje.className = "mensaje-formulario mensaje-exito";
        } catch (exception) {
            mensaje.textContent = exception.message;
            mensaje.className = "mensaje-formulario mensaje-error";
        } finally {
            botonGuardar.disabled = false;
        }
    });

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

    function mostrarCuenta(perfil) {
        document.getElementById("perfil-nombre-usuario").textContent = perfil.nombreUsuario;
        document.getElementById("perfil-nombre-completo").textContent = perfil.nombreCompleto;
        document.getElementById("perfil-correo").textContent = perfil.correo;
        document.getElementById("perfil-rol").textContent = perfil.rol;
        document.getElementById("titulo-perfil").textContent = "Perfil de " + perfil.nombreCompleto;
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
