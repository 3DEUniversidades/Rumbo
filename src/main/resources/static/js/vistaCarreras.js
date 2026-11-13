document.addEventListener("DOMContentLoaded", () => {
    const NO_DISPONIBLE = "No disponible";

    const estado = document.getElementById("estado-carreras");
    const lista = document.getElementById("lista-carreras");
    const universidad = document.getElementById("universidad-catalogo");
    let carreras = [];
    universidad.addEventListener("change", () => mostrarCarreras(carreras));
    const modal = document.getElementById("modal-carrera");
    const botonCerrar = document.getElementById("cerrar-modal-carrera");

    const modalNombre = document.getElementById("modal-carrera-nombre");
    const modalFacultad = document.getElementById("modal-carrera-facultad");
    const modalDescripcion = document.getElementById("modal-carrera-descripcion");
    const modalDuracion = document.getElementById("modal-carrera-duracion");
    const modalCosto = document.getElementById("modal-carrera-costo");
    const modalCostoNota = document.getElementById("modal-carrera-costo-nota");
    const modalModalidad = document.getElementById("modal-carrera-modalidad");
    const modalCreditos = document.getElementById("modal-carrera-creditos");
    const modalAreas = document.getElementById("modal-carrera-areas");
    const modalCompetencias = document.getElementById("modal-carrera-competencias");
    const modalOportunidades = document.getElementById("modal-carrera-oportunidades");
    const modalSalario = document.getElementById("modal-carrera-salario");
    const modalEmpleabilidad = document.getElementById("modal-carrera-empleabilidad");
    const bloqueContextoLaboral = document.getElementById("bloque-contexto-laboral");
    const modalContextoLaboral = document.getElementById("modal-carrera-contexto-laboral");
    const bloquePdf = document.getElementById("bloque-pdf-oficial");
    const enlacePdf = document.getElementById("modal-carrera-pdf");
    const bloqueCarreraOficial = document.getElementById("bloque-carrera-oficial");
    const enlaceCarreraOficial = document.getElementById("modal-carrera-enlace-oficial");

    botonCerrar.addEventListener("click", () => modal.close());

    modal.addEventListener("click", (evento) => {
        if (evento.target === modal) {
            modal.close();
        }
    });

    obtenerSesion().then(function (sesion) {
        if (sesion) {
            cargarCarreras();
        }
    }).catch(function () {
        estado.textContent = "No se pudo cargar la sesion.";
    });

    async function cargarCarreras() {
        estado.textContent = "Cargando carreras...";
        lista.replaceChildren();

        try {
            const respuesta = await fetch("/api/carreras");

            if (!respuesta.ok) {
                throw new Error("No se pudo consultar el catalogo de carreras.");
            }

            carreras = await respuesta.json();
            universidad.replaceChildren(new Option("Todas las universidades", ""),
                ...[...new Set(carreras.map(c => c.universidad).filter(Boolean))]
                    .sort((a, b) => a.localeCompare(b, "es")).map(u => new Option(u, u)));
            universidad.disabled = false;
            mostrarCarreras(carreras);
        } catch (error) {
            estado.textContent = "No se pudieron cargar las carreras.";
        }
    }

    function mostrarCarreras(carreras) {
        lista.replaceChildren();
        const visibles = carreras.filter(c => !universidad.value || c.universidad === universidad.value);

        if (visibles.length === 0) {
            estado.textContent = "No hay carreras disponibles para esta universidad.";
            return;
        }

        estado.textContent = `${visibles.length} carrera(s) disponibles.`;

        visibles.forEach((carrera) => {
            lista.appendChild(crearTarjetaCarrera(carrera));
        });
    }

    function crearTarjetaCarrera(carrera) {
        const tarjeta = document.createElement("article");
        tarjeta.className = "carrera-card";

        const nombre = document.createElement("h3");
        nombre.textContent = carrera.nombre;
        const institucion = document.createElement("p");
        institucion.className = "carrera-universidad";
        institucion.textContent = carrera.universidad ?? NO_DISPONIBLE;

        const facultad = document.createElement("p");
        facultad.className = "carrera-area";
        facultad.textContent = "Facultad: " + (carrera.facultad ?? NO_DISPONIBLE);

        const resumen = document.createElement("p");
        resumen.className = "carrera-uni";
        const duracion = carrera.duracion == null
            ? "Duración no disponible" : `${carrera.duracion} años`;
        resumen.textContent = carrera.costo == null
            ? `${duracion} · Costo no disponible`
            : `${duracion} · ${formatearMonto(carrera.costo)} el primer semestre`;

        const botonDetalle = document.createElement("button");
        botonDetalle.className = "btn";
        botonDetalle.type = "button";
        botonDetalle.textContent = "Ver detalle";
        botonDetalle.addEventListener("click", () => abrirDetalle(carrera));

        tarjeta.append(nombre, institucion, facultad, resumen, botonDetalle);
        return tarjeta;
    }

    function abrirDetalle(carrera) {
        modalNombre.textContent = carrera.nombre;
        document.getElementById("modal-carrera-universidad").textContent = carrera.universidad ?? NO_DISPONIBLE;
        modalFacultad.textContent = "Facultad: " + (carrera.facultad ?? NO_DISPONIBLE);
        modalDescripcion.textContent = carrera.descripcion ?? NO_DISPONIBLE;

        modalDuracion.textContent = carrera.duracion == null
            ? NO_DISPONIBLE : `${carrera.duracion} años`;
        modalCosto.textContent = formatearMonto(carrera.costo);
        modalCostoNota.textContent = construirNotaCosto(carrera);
        modalModalidad.textContent = carrera.modalidad ?? NO_DISPONIBLE;
        modalCreditos.textContent = carrera.cargaCreditos ?? NO_DISPONIBLE;

        mostrarLista(modalAreas, carrera.areasFormacion);
        mostrarLista(modalCompetencias, carrera.competenciasDesarrolladas);
        mostrarLista(modalOportunidades, carrera.oportunidadesLaborales);

        modalSalario.textContent = formatearMonto(carrera.salarioEstimado);
        modalEmpleabilidad.textContent = carrera.empleabilidad == null
            ? NO_DISPONIBLE
            : `${carrera.empleabilidad} %`;

        modalContextoLaboral.textContent = carrera.contextoLaboral ?? NO_DISPONIBLE;
        bloqueContextoLaboral.hidden = false;

        mostrarEnlaceOficial(bloquePdf, enlacePdf, carrera.pdfOficialUrl);
        mostrarEnlaceOficial(
            bloqueCarreraOficial, enlaceCarreraOficial, carrera.urlCarreraOficial
        );
        modal.showModal();
    }

    function construirNotaCosto(carrera) {
        if (carrera.costo == null) {
            return "";
        }

        let nota = carrera.costoPeriodo ?? "";
        if (carrera.costoAnio) {
            nota += nota ? ` (tarifario ${carrera.costoAnio})` : `Tarifario ${carrera.costoAnio}`;
        }

        const aviso = "Es una referencia: no es el costo total de la carrera ni incluye becas o descuentos.";
        return nota ? `${nota}. ${aviso}` : aviso;
    }

    function mostrarLista(contenedor, elementos) {
        contenedor.replaceChildren();

        if (!Array.isArray(elementos) || elementos.length === 0) {
            const vacio = document.createElement("li");
            vacio.className = "dato-faltante";
            vacio.textContent = NO_DISPONIBLE;
            contenedor.appendChild(vacio);
            return;
        }

        elementos.forEach((texto) => {
            const item = document.createElement("li");
            item.textContent = texto;
            contenedor.appendChild(item);
        });
    }

    function mostrarEnlaceOficial(bloque, enlace, direccion) {
        let url;

        try {
            url = new URL(direccion);
        } catch {
            bloque.hidden = false;
            enlace.removeAttribute("href");
            enlace.textContent = NO_DISPONIBLE;
            return;
        }

        if (url.protocol !== "https:") {
            bloque.hidden = false;
            enlace.removeAttribute("href");
            enlace.textContent = NO_DISPONIBLE;
            return;
        }

        enlace.href = url.href;
        enlace.textContent = bloque === bloquePdf ? "Abrir PDF oficial" : "Visitar sitio oficial";
        bloque.hidden = false;
    }

    // Un valor desconocido es null; nunca se muestra como cero.
    function formatearMonto(valor) {
        if (valor == null) {
            return NO_DISPONIBLE;
        }

        return new Intl.NumberFormat("es-GT", {
            style: "currency",
            currency: "GTQ",
            maximumFractionDigits: 2
        }).format(valor);
    }
});
