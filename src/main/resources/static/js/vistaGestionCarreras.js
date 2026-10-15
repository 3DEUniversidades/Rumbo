document.addEventListener("DOMContentLoaded", () => {
    // Rol provisional: debe coincidir con el definido por el modulo de sesion/permisos.
    const ROL_ENCARGADO_CARRERAS = "ENCARGADO_CARRERAS";

    // Guardia visual. La validacion real de permisos la hace el backend.
    const sesion = JSON.parse(localStorage.getItem("sesionRumbo"));
    if (!sesion) {
        window.location.replace("/login.html");
        return;
    }
    if (sesion.rol !== ROL_ENCARGADO_CARRERAS) {
        window.location.replace("/inicio.html");
        return;
    }

    const estadoGestion = document.getElementById("estado-gestion");
    const tablaCarreras = document.getElementById("tabla-carreras");
    const cuerpoCarreras = document.getElementById("cuerpo-carreras");
    const mensajeGestion = document.getElementById("mensaje-gestion");

    const estadoHistorial = document.getElementById("estado-historial");
    const tablaHistorial = document.getElementById("tabla-historial");
    const cuerpoHistorial = document.getElementById("cuerpo-historial");
    const filtroCarrera = document.getElementById("filtro-carrera");

    const modal = document.getElementById("modal-edicion");
    const formulario = document.getElementById("form-edicion");
    const tituloEdicion = document.getElementById("edicion-titulo");
    const errorEdicion = document.getElementById("error-edicion");
    const botonGuardar = document.getElementById("guardar-edicion");

    // Campos del formulario: id del input, clave del JSON y tipo de dato.
    // Tipos: texto, entero, decimal y lista (un elemento por linea).
    const CAMPOS = [
        { id: "f-nombre", clave: "nombre", tipo: "texto" },
        { id: "f-facultad", clave: "facultad", tipo: "texto" },
        { id: "f-descripcion", clave: "descripcion", tipo: "texto" },
        { id: "f-duracion", clave: "duracion", tipo: "entero" },
        { id: "f-costo", clave: "costo", tipo: "decimal" },
        { id: "f-modalidad", clave: "modalidad", tipo: "texto" },
        { id: "f-costo", clave: "costo", tipo: "decimal" },
        { id: "f-costo-anio", clave: "costoAnio", tipo: "entero" },
        { id: "f-costo-periodo", clave: "costoPeriodo", tipo: "texto" },
        { id: "f-creditos", clave: "cargaCreditos", tipo: "entero" },
        { id: "f-salario", clave: "salarioEstimado", tipo: "decimal" },
        { id: "f-empleabilidad", clave: "empleabilidad", tipo: "decimal" },
        { id: "f-contexto-laboral", clave: "contextoLaboral", tipo: "texto" },
        { id: "f-pdf", clave: "pdfOficialUrl", tipo: "texto" },
        { id: "f-url", clave: "urlCarreraOficial", tipo: "texto" }
    ];

    // Nombres legibles para el historial.
    const ETIQUETAS = {
        nombre: "Nombre",
        facultad: "Facultad",
        descripcion: "Descripci�n",
        duracion: "Duraci�n",
        costo: "Costo",
        costoPeriodo: "Periodo del costo",
        costoAnio: "A�o del costo",
        modalidad: "Modalidad",
        cargaCreditos: "Carga de cr�ditos",
        areasFormacion: "�reas de formaci�n",
        competenciasDesarrolladas: "Competencias",
        oportunidadesLaborales: "Oportunidades laborales",
        salarioEstimado: "Salario estimado",
        empleabilidad: "Empleabilidad",
        contextoLaboral: "Contexto laboral",
        pdfOficialUrl: "PDF oficial",
        urlCarreraOficial: "Enlace oficial"
    };

    let carreras = [];
    let carreraEnEdicion = null;

    document.getElementById("cerrar-modal-edicion").addEventListener("click", () => modal.close());
    document.getElementById("cancelar-edicion").addEventListener("click", () => modal.close());
    modal.addEventListener("click", (evento) => {
        if (evento.target === modal) {
            modal.close();
        }
    });
    formulario.addEventListener("submit", guardarCambios);
    filtroCarrera.addEventListener("change", cargarHistorial);

    iniciar();

    async function iniciar() {
        await cargarCarreras();
        await cargarHistorial();
    }

    // ---------- Comunicacion con el backend ----------

    function cabeceras() {
        // Temporal: la identidad viaja en un header con el id del usuario.
        // Si el modulo de sesion cambia a token, solo se modifica esta funcion.
        return {
            "Content-Type": "application/json",
            "X-Usuario-Id": sesion.idUsuario
        };
    }

    async function leerError(respuesta) {
        if (respuesta.status === 401) {
        }
        if (respuesta.status === 403) {
            return "No tienes permiso para gestionar carreras.";
        }
        try {
            const cuerpo = await respuesta.json();
        } catch (e) {
        }
    }

    async function cargarCarreras() {
        estadoGestion.textContent = "Cargando carreras...";
        estadoGestion.hidden = false;
        try {
            const respuesta = await fetch("/api/carreras");
            if (!respuesta.ok) {
                throw new Error();
            }
            carreras = await respuesta.json();
            mostrarCarreras();
            llenarFiltro();
        } catch (e) {
            estadoGestion.textContent = "No se pudieron cargar las carreras.";
            tablaCarreras.hidden = true;
        }
    }

    async function cargarHistorial() {
        estadoHistorial.textContent = "Cargando historial...";
        estadoHistorial.hidden = false;
        tablaHistorial.hidden = true;

        const id = filtroCarrera.value;
        const url = id ? "/api/carreras/" + id + "/historial" : "/api/carreras/historial";

        try {
            const respuesta = await fetch(url, { headers: cabeceras() });
            if (!respuesta.ok) {
                estadoHistorial.textContent = await leerError(respuesta);
                return;
            }
            mostrarHistorial(await respuesta.json());
        } catch (e) {
            estadoHistorial.textContent = "No se pudo cargar el historial.";
        }
    }

    // ---------- Tablero de carreras ----------

    function mostrarCarreras() {
        cuerpoCarreras.replaceChildren();

        if (carreras.length === 0) {
            estadoGestion.textContent = "No hay carreras registradas.";
            tablaCarreras.hidden = true;
            return;
        }

        estadoGestion.hidden = true;
        tablaCarreras.hidden = false;

        carreras.forEach((c) => {
            const fila = document.createElement("tr");

            fila.append(
                celda(c.nombre),
                celda(valorOGuion(c.modalidad)),
                celda(valorOGuion(c.cargaCreditos)),
                celda(c.salarioEstimado == null ? "-" : formatearMoneda(c.salarioEstimado)),
                celda(c.empleabilidad == null ? "-" : c.empleabilidad + "%"),
                celdaEnlaces(c),
                celdaBoton(c)
            );
            cuerpoCarreras.appendChild(fila);
        });
    }

    function celda(texto) {
        const td = document.createElement("td");
        td.textContent = texto;
        return td;
    }

    function celdaEnlaces(c) {
        const td = document.createElement("td");
        td.className = "celda-enlaces";

        [["PDF", c.pdfOficialUrl], ["Sitio", c.urlCarreraOficial]].forEach(([etiqueta, url]) => {
            if (url) {
                const a = document.createElement("a");
                a.href = url;
                a.target = "_blank";
                a.rel = "noopener noreferrer";
                a.textContent = etiqueta;
                td.appendChild(a);
            } else {
                const span = document.createElement("span");
                span.className = "enlace-faltante";
                span.textContent = etiqueta + ": falta";
                td.appendChild(span);
            }
        });
        return td;
    }

    function celdaBoton(c) {
        const td = document.createElement("td");
        const boton = document.createElement("button");
        boton.type = "button";
        boton.className = "btn btn-pequeno";
        boton.textContent = "Editar";
        boton.addEventListener("click", () => abrirEdicion(c));
        td.appendChild(boton);
        return td;
    }

    function llenarFiltro() {
        const seleccionada = filtroCarrera.value;
        filtroCarrera.replaceChildren();

        const todas = document.createElement("option");
        todas.value = "";
        todas.textContent = "Todas";
        filtroCarrera.appendChild(todas);

        carreras.forEach((c) => {
            const opcion = document.createElement("option");
            opcion.value = c.id;
            opcion.textContent = c.nombre;
            filtroCarrera.appendChild(opcion);
        });
        filtroCarrera.value = seleccionada;
    }

    // ---------- Edicion ----------

    function abrirEdicion(carrera) {
        carreraEnEdicion = carrera;
        tituloEdicion.textContent = "Editar: " + carrera.nombre;
        ocultarError();

        CAMPOS.forEach((campo) => {
            const valor = carrera[campo.clave];
        });

        modal.showModal();
    }

    // Un campo vacio se envia como null: queda como "No disponible", nunca como cero.
    function leerCampo(campo) {
        const bruto = document.getElementById(campo.id).value.trim();
        }
        if (bruto === "") {
            return null;
        }
    }

    // Arma solo los campos que cambiaron respecto a la carrera original.
    function calcularCambios() {
        const cambios = {};

        CAMPOS.forEach((campo) => {
            const nuevo = leerCampo(campo);
            const original = carreraEnEdicion[campo.clave];

                cambios[campo.clave] = nuevo;
            }
        });
        return cambios;
    }

    async function guardarCambios(evento) {
        evento.preventDefault();
        ocultarError();

        const cambios = calcularCambios();
        if (Object.keys(cambios).length === 0) {
            mostrarError("No hay cambios que guardar.");
            return;
        }

        botonGuardar.disabled = true;
        botonGuardar.textContent = "Guardando...";

        try {
            const respuesta = await fetch("/api/carreras/" + carreraEnEdicion.id, {
                method: "PUT",
                headers: cabeceras(),
                body: JSON.stringify(cambios)
            });

            if (!respuesta.ok) {
                mostrarError(await leerError(respuesta));
                return;
            }

            const nombre = carreraEnEdicion.nombre;
            modal.close();
            mostrarMensaje("Carrera actualizada: " + nombre);
            await cargarCarreras();
            await cargarHistorial();
        } catch (e) {
            mostrarError("No se pudo conectar con el servidor.");
        } finally {
            botonGuardar.disabled = false;
            botonGuardar.textContent = "Guardar cambios";
        }
    }

    // ---------- Historial ----------

    function mostrarHistorial(registros) {
        cuerpoHistorial.replaceChildren();

        if (registros.length === 0) {
            estadoHistorial.hidden = false;
            tablaHistorial.hidden = true;
            return;
        }

        estadoHistorial.hidden = true;
        tablaHistorial.hidden = false;

        registros.forEach((r) => {
            const fila = document.createElement("tr");
            fila.append(
                celda(formatearFecha(r.fecha)),
                celda(r.carreraNombre),
                celda(valorOGuion(r.valorAnterior)),
                celda(valorOGuion(r.valorNuevo)),
                celda(r.responsable)
            );
            cuerpoHistorial.appendChild(fila);
        });
    }

    // ---------- Utilidades ----------

    function mostrarMensaje(texto) {
        mensajeGestion.textContent = texto;
        mensajeGestion.hidden = false;
        setTimeout(() => { mensajeGestion.hidden = true; }, 4000);
    }

    function mostrarError(texto) {
        errorEdicion.textContent = texto;
        errorEdicion.style.display = "block";
    }

    function ocultarError() {
        errorEdicion.textContent = "";
        errorEdicion.style.display = "none";
    }

    function valorOGuion(valor) {
        return valor == null || valor === "" ? "-" : valor;
    }

    function formatearMoneda(valor) {
        return new Intl.NumberFormat("es-GT", {
            style: "currency",
            currency: "GTQ",
            maximumFractionDigits: 2
        }).format(valor);
    }

    function formatearFecha(iso) {
        return new Date(iso).toLocaleString("es-GT", {
            dateStyle: "short",
            timeStyle: "short"
        });
    }
});