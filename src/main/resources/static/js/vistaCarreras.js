document.addEventListener("DOMContentLoaded", () => {
    const estado = document.getElementById("estado-carreras");
    const lista = document.getElementById("lista-carreras");
    const modal = document.getElementById("modal-carrera");
    const botonCerrar = document.getElementById("cerrar-modal-carrera");

    const modalNombre = document.getElementById("modal-carrera-nombre");
    const modalDuracion = document.getElementById("modal-carrera-duracion");
    const modalCosto = document.getElementById("modal-carrera-costo");
    const modalModalidad = document.getElementById("modal-carrera-modalidad");
    const modalCreditos = document.getElementById("modal-carrera-creditos");
    const modalHabilidades = document.getElementById("modal-carrera-habilidades");
    const modalOportunidades = document.getElementById("modal-carrera-oportunidades");
    const modalSalario = document.getElementById("modal-carrera-salario");
    const modalEmpleabilidad = document.getElementById("modal-carrera-empleabilidad");
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

            const carreras = await respuesta.json();
            mostrarCarreras(carreras);
        } catch (error) {
            estado.textContent = "No se pudieron cargar las carreras.";
        }
    }

    function mostrarCarreras(carreras) {
        lista.replaceChildren();

        if (carreras.length === 0) {
            estado.textContent = "No hay carreras disponibles aun.";
            return;
        }

        estado.textContent = "";

        carreras.forEach((carrera) => {
            lista.appendChild(crearTarjetaCarrera(carrera));
        });
    }

    function crearTarjetaCarrera(carrera) {
        const tarjeta = document.createElement("article");
        tarjeta.className = "carrera-card";

        const nombre = document.createElement("h3");
        nombre.textContent = carrera.nombre;

        const duracion = document.createElement("p");
        duracion.className = "carrera-area";
        duracion.textContent = `${carrera.duracion} años`;

        const costo = document.createElement("p");
        costo.className = "carrera-uni";
        costo.textContent = formatearCosto(carrera.costo);

        const botonDetalle = document.createElement("button");
        botonDetalle.className = "btn";
        botonDetalle.type = "button";
        botonDetalle.textContent = "Ver detalle";
        botonDetalle.addEventListener("click", () => abrirDetalle(carrera));

        tarjeta.append(nombre, duracion, costo, botonDetalle);
        return tarjeta;
    }

    function abrirDetalle(carrera) {
        modalNombre.textContent = carrera.nombre;
        modalDuracion.textContent = `${carrera.duracion} años`;
        modalCosto.textContent = formatearCosto(carrera.costo);
        modalModalidad.textContent = carrera.modalidad ?? "Pendiente de información oficial";
        modalCreditos.textContent = carrera.cargaCreditos ?? "Pendiente de información oficial";
        modalHabilidades.textContent =
            carrera.habilidadesRequeridas ?? "Pendiente de información oficial";
        modalOportunidades.textContent =
            carrera.oportunidadesLaborales ?? "Pendiente de información oficial";

        modalSalario.textContent = carrera.salarioEstimado == null
            ? "Pendiente de información oficial"
            : formatearCosto(carrera.salarioEstimado);

        modalEmpleabilidad.textContent = carrera.empleabilidad == null
            ? "Pendiente de información oficial"
            : `${carrera.empleabilidad} %`;

        mostrarEnlaceOficial(bloquePdf, enlacePdf, carrera.pdfOficialUrl);
        mostrarEnlaceOficial(
            bloqueCarreraOficial, enlaceCarreraOficial, carrera.urlCarreraOficial
        );
        modal.showModal();
    }

    function mostrarEnlaceOficial(bloque, enlace, direccion) {
        let url;

        try {
            url = new URL(direccion);
        } catch {
            bloque.hidden = true;
            enlace.removeAttribute("href");
            return;
        }

        if (url.protocol !== "https:") {
            bloque.hidden = true;
            enlace.removeAttribute("href");
            return;
        }

        enlace.href = url.href;
        bloque.hidden = false;
    }

    function formatearCosto(costo) {
        return new Intl.NumberFormat("es-GT", {
            style: "currency",
            currency: "GTQ",
            maximumFractionDigits: 2
        }).format(costo);
    }
});
