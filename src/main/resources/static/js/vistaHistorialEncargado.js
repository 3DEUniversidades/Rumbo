document.addEventListener("DOMContentLoaded", async () => {
    const obtener = id => document.getElementById(id);
    const seccion = obtener("seccion-historial-encargado");
    const estado = obtener("estado-historial-encargado");
    const tabla = obtener("tabla-historial-encargado");
    const cuerpo = obtener("cuerpo-historial-encargado");
    const actualizar = obtener("actualizar-historial-encargado");

    const etiquetas = {
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

    actualizar.addEventListener("click", cargar);

    try {
        const sesion = await obtenerSesion();
        if (!sesion || sesion.rol !== "ENCARGADO_DE_CARRERAS") return;
        seccion.hidden = false;
        await cargar();
    } catch (error) {
        estado.textContent = error.message;
    }

    async function cargar() {
        if (actualizar.disabled) return;
        actualizar.disabled = true;
        tabla.hidden = true;
        cuerpo.replaceChildren();
        estado.textContent = "Consultando tus actualizaciones...";

        try {
            const respuesta = await fetch("/api/carreras/mis-actualizaciones", {
                credentials: "same-origin",
                cache: "no-store"
            });
            if (respuesta.status === 401) {
                window.location.replace("/login.html");
                return;
            }
            if (respuesta.status === 403) seccion.hidden = true;
            const datos = await respuesta.json().catch(() => ({}));
            if (!respuesta.ok) {
                throw new Error(datos.error || "No se pudo consultar tu historial.");
            }

            datos.forEach(registro => {
                const fila = document.createElement("tr");
                const fecha = new Date(registro.fecha);
                [
                    Number.isNaN(fecha.getTime())
                        ? registro.fecha : fecha.toLocaleString("es-GT"),
                    registro.carreraNombre,
                    etiquetas[registro.campo] ?? registro.campo,
                    registro.valorAnterior,
                    registro.valorNuevo
                ].forEach(valor => {
                    const celda = document.createElement("td");
                    celda.className = "valor-comparador";
                    celda.textContent = valor == null || valor === ""
                        ? "No disponible" : valor;
                    fila.appendChild(celda);
                });
                cuerpo.appendChild(fila);
            });

            tabla.hidden = datos.length === 0;
            estado.textContent = datos.length === 0
                ? "Todavía no has realizado actualizaciones de carreras."
                : `${datos.length} actualizaciones registradas.`;
        } catch (error) {
            cuerpo.replaceChildren();
            tabla.hidden = true;
            estado.textContent = error.message;
        } finally {
            actualizar.disabled = false;
        }
    }
});
