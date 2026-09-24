document.addEventListener("DOMContentLoaded", () => {
    const estado = document.getElementById("estado-carreras");
    const lista = document.getElementById("lista-carreras");
    const modal = document.getElementById("modal-carrera");
    const botonCerrar = document.getElementById("cerrar-modal-carrera");

    const modalNombre = document.getElementById("modal-carrera-nombre");
    const modalDuracion = document.getElementById("modal-carrera-duracion");
    const modalCosto = document.getElementById("modal-carrera-costo");
    const modalPensum = document.getElementById("modal-carrera-pensum");

    botonCerrar.addEventListener("click", () => modal.close());

    modal.addEventListener("click", (evento) => {
        if (evento.target === modal) {
            modal.close();
        }
    });

    cargarCarreras();

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

        const pensum = document.createElement("p");
        pensum.textContent = carrera.pensum;

        const botonDetalle = document.createElement("button");
        botonDetalle.className = "btn";
        botonDetalle.type = "button";
        botonDetalle.textContent = "Ver detalle";
        botonDetalle.addEventListener("click", () => abrirDetalle(carrera));

        tarjeta.append(nombre, duracion, costo, pensum, botonDetalle);
        return tarjeta;
    }

    function abrirDetalle(carrera) {
        modalNombre.textContent = carrera.nombre;
        modalDuracion.textContent = `${carrera.duracion} años`;
        modalCosto.textContent = formatearCosto(carrera.costo);
        modalPensum.textContent = carrera.pensum;
        modal.showModal();
    }

    function formatearCosto(costo) {
        return new Intl.NumberFormat("es-GT", {
            style: "currency",
            currency: "GTQ",
            maximumFractionDigits: 2
        }).format(costo);
    }
});
