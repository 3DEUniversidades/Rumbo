document.addEventListener("DOMContentLoaded", () => {
    const obtener = id => document.getElementById(id);
    const principal = obtener("gestion-carreras");
    const modal = obtener("modal-edicion");
    const formulario = obtener("form-edicion");
    const guardar = obtener("guardar-edicion");
    const error = obtener("error-edicion");

    let carreras = [];
    let carreraActual = null;
    let guardando = false;
    let revisionHistorial = 0;

    const CAMPOS = [
        ["f-nombre", "nombre", "texto"],
        ["f-facultad", "facultad", "texto"],
        ["f-descripcion", "descripcion", "texto"],
        ["f-duracion", "duracion", "numero"],
        ["f-costo", "costo", "numero"],
        ["f-costo-periodo", "costoPeriodo", "texto"],
        ["f-costo-anio", "costoAnio", "numero"],
        ["f-modalidad", "modalidad", "texto"],
        ["f-creditos", "cargaCreditos", "numero"],
        ["f-areas", "areasFormacion", "lista"],
        ["f-competencias", "competenciasDesarrolladas", "lista"],
        ["f-oportunidades", "oportunidadesLaborales", "lista"],
        ["f-salario", "salarioEstimado", "numero"],
        ["f-empleabilidad", "empleabilidad", "numero"],
        ["f-contexto-laboral", "contextoLaboral", "texto"],
        ["f-pdf", "pdfOficialUrl", "texto"],
        ["f-url", "urlCarreraOficial", "texto"]
    ];

    const ETIQUETAS = {
        nombre: "Nombre",
        facultad: "Facultad",
        descripcion: "Descripción",
        duracion: "Duración",
        costo: "Costo",
        costoPeriodo: "Periodo del costo",
        costoAnio: "Año del tarifario",
        modalidad: "Modalidad",
        cargaCreditos: "Créditos",
        areasFormacion: "Áreas de formación",
        competenciasDesarrolladas: "Competencias",
        oportunidadesLaborales: "Oportunidades laborales",
        salarioEstimado: "Salario estimado",
        empleabilidad: "Empleabilidad",
        contextoLaboral: "Contexto laboral",
        pdfOficialUrl: "PDF oficial",
        urlCarreraOficial: "Página oficial"
    };

    obtener("cerrar-modal-edicion").addEventListener("click", cerrar);
    obtener("cancelar-edicion").addEventListener("click", cerrar);

    modal.addEventListener("cancel", evento => {
        if (guardando) evento.preventDefault();
    });

    formulario.addEventListener("submit", guardarCambios);
    obtener("filtro-carrera").addEventListener("change", cargarHistorial);

    iniciar();

    async function iniciar() {
        try {
            const sesion = await obtenerSesion();

            if (!sesion) return;

            if (sesion.rol !== "ENCARGADO_DE_CARRERAS") {
                window.location.replace("/inicio.html");
                return;
            }

            principal.hidden = false;
            await cargarCarreras();
            await cargarHistorial();

        } catch (e) {
            obtener("estado-acceso").textContent = e.message;
        }
    }

    async function api(url, opciones = {}) {
        const respuesta = await fetch(url, {
            ...opciones,
            credentials: "same-origin",
            cache: "no-store",
            headers: {
                "Content-Type": "application/json"
            }
        });

        const datos = await respuesta.json().catch(() => ({}));

        if (respuesta.status === 401 || respuesta.status === 403) {
            principal.hidden = true;
            modal.close();

            window.location.replace(
                respuesta.status === 401
                    ? "/login.html"
                    : "/inicio.html"
            );
        }

        if (!respuesta.ok) {
            throw new Error(
                datos.error || "No se pudo completar la operación."
            );
        }

        return datos;
    }

    async function cargarCarreras() {
        const estado = obtener("estado-gestion");

        estado.hidden = false;
        estado.textContent = "Cargando carreras...";
        obtener("tabla-carreras").hidden = true;

        try {
            carreras = await api("/api/carreras");

            const cuerpo = obtener("cuerpo-carreras");
            cuerpo.replaceChildren();

            carreras.forEach(c => {
                const fila = document.createElement("tr");

                [
                    c.nombre,
                    dato(c.facultad),
                    `${c.duracion} años`,
                    moneda(c.costo),
                    dato(c.modalidad),
                    dato(c.cargaCreditos),
                    moneda(c.salarioEstimado),
                    c.empleabilidad == null
                        ? "No disponible"
                        : `${c.empleabilidad} %`
                ].forEach(valor => fila.appendChild(celda(valor)));

                const enlaces = document.createElement("td");
                enlaces.className = "celda-enlaces";

                [
                    ["PDF", c.pdfOficialUrl],
                    ["Sitio", c.urlCarreraOficial]
                ].forEach(([nombre, direccion]) => {
                    try {
                        const url = new URL(direccion);

                        if (url.protocol !== "https:") {
                            throw new Error();
                        }

                        const a = document.createElement("a");
                        a.href = url.href;
                        a.target = "_blank";
                        a.rel = "noopener noreferrer";
                        a.textContent = nombre;
                        enlaces.appendChild(a);

                    } catch {
                        const texto = document.createElement("span");
                        texto.textContent = `${nombre}: no disponible`;
                        enlaces.appendChild(texto);
                    }
                });

                fila.appendChild(enlaces);

                const accion = document.createElement("td");
                const boton = document.createElement("button");

                boton.type = "button";
                boton.className = "btn btn-pequeno";
                boton.textContent = "Editar";
                boton.addEventListener("click", () => abrirEdicion(c));

                accion.appendChild(boton);
                fila.appendChild(accion);
                cuerpo.appendChild(fila);
            });

            const filtro = obtener("filtro-carrera");
            const anterior = filtro.value;

            filtro.replaceChildren(new Option("Todas", ""));

            carreras.forEach(c => {
                filtro.appendChild(new Option(c.nombre, c.id));
            });

            filtro.value = carreras.some(c => String(c.id) === anterior)
                ? anterior
                : "";

            estado.textContent = carreras.length
                ? ""
                : "No hay carreras registradas.";

            estado.hidden = carreras.length > 0;
            obtener("tabla-carreras").hidden = carreras.length === 0;

        } catch (e) {
            estado.textContent = e.message;
        }
    }

    async function cargarHistorial() {
        const revision = ++revisionHistorial;
        const estado = obtener("estado-historial");

        estado.hidden = false;
        estado.textContent = "Cargando historial...";
        obtener("tabla-historial").hidden = true;

        const id = obtener("filtro-carrera").value;

        try {
            const registros = await api(
                id
                    ? `/api/carreras/${id}/historial`
                    : "/api/carreras/historial"
            );

            if (revision !== revisionHistorial) return;

            const cuerpo = obtener("cuerpo-historial");
            cuerpo.replaceChildren();

            registros.forEach(r => {
                const fila = document.createElement("tr");

                [
                    new Date(r.fecha).toLocaleString("es-GT"),
                    r.carreraNombre,
                    ETIQUETAS[r.campo] ?? r.campo,
                    dato(r.valorAnterior),
                    dato(r.valorNuevo),
                    r.responsable
                ].forEach(valor => fila.appendChild(celda(valor)));

                cuerpo.appendChild(fila);
            });

            estado.hidden = registros.length > 0;
            estado.textContent = registros.length
                ? ""
                : "Todavía no hay actualizaciones.";

            obtener("tabla-historial").hidden = registros.length === 0;

        } catch (e) {
            if (revision === revisionHistorial) {
                estado.textContent = e.message;
            }
        }
    }

    function abrirEdicion(carrera) {
        carreraActual = carrera;

        obtener("edicion-titulo").textContent =
            "Editar: " + carrera.nombre;

        error.style.display = "none";

        CAMPOS.forEach(([id, clave, tipo]) => {
            const valor = carrera[clave];

            obtener(id).value = tipo === "lista"
                ? (valor ?? []).join("\n")
                : valor ?? "";
        });

        modal.showModal();
    }

    function cerrar() {
        if (!guardando) modal.close();
    }

    function leerCampo(id, tipo) {
        const valor = obtener(id).value.trim();

        if (tipo === "lista") {
            return [...new Set(
                valor.split("\n")
                    .map(v => v.trim())
                    .filter(Boolean)
            )];
        }

        if (!valor) return null;

        return tipo === "numero" ? Number(valor) : valor;
    }

    async function guardarCambios(evento) {
        evento.preventDefault();

        if (guardando
            || !carreraActual
            || !formulario.reportValidity()) {
            return;
        }

        error.style.display = "none";

        const cambios = {};

        CAMPOS.forEach(([id, clave, tipo]) => {
            const nuevo = leerCampo(id, tipo);

            const anterior = tipo === "lista"
                ? carreraActual[clave] ?? []
                : carreraActual[clave] ?? null;

            if (JSON.stringify(nuevo) !== JSON.stringify(anterior)) {
                cambios[clave] = nuevo;
            }
        });

        if (!Object.keys(cambios).length) {
            error.textContent = "No hay cambios que guardar.";
            error.style.display = "block";
            return;
        }

        guardando = true;
        guardar.disabled = true;
        guardar.textContent = "Guardando...";

        try {
            const actualizada = await api(
                `/api/carreras/${carreraActual.id}`,
                {
                    method: "PUT",
                    body: JSON.stringify(cambios)
                }
            );

            modal.close();

            const mensaje = obtener("mensaje-gestion");
            mensaje.textContent =
                "Carrera actualizada: " + actualizada.nombre;
            mensaje.hidden = false;

            await cargarCarreras();
            await cargarHistorial();

        } catch (e) {
            error.textContent = e.message;
            error.style.display = "block";

        } finally {
            guardando = false;
            guardar.disabled = false;
            guardar.textContent = "Guardar cambios";
        }
    }

    function celda(texto) {
        const td = document.createElement("td");
        td.textContent = texto;
        return td;
    }

    function dato(valor) {
        return valor == null || valor === ""
            ? "No disponible"
            : valor;
    }

    function moneda(valor) {
        return valor == null
            ? "No disponible"
            : new Intl.NumberFormat("es-GT", {
                style: "currency",
                currency: "GTQ"
            }).format(valor);
    }
});