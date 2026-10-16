document.addEventListener("DOMContentLoaded", () => {
    const buscar = document.getElementById("buscar-comparador");
    const facultad = document.getElementById("facultad-comparador");
    const duracion = document.getElementById("duracion-comparador");
    const costo = document.getElementById("costo-comparador");
    const cantidad = document.getElementById("cantidad-comparador");

    const estado = document.getElementById("estado-comparador");
    const catalogo = document.getElementById("catalogo-comparador");
    const seleccion = document.getElementById("seleccion-comparador");
    const contador = document.getElementById("contador-comparador");

    const botonComparar = document.getElementById("comparar-carreras");
    const resultado = document.getElementById("resultado-comparador");
    const tabla = document.getElementById("tabla-comparador");

    let carreras = [];
    const seleccionadas = new Map();

    let objetivo = 2;
    let consultando = false;
    let revision = 0;

    [buscar, facultad, duracion, costo].forEach(control => {
        control.addEventListener("input", mostrarCatalogo);
    });

    cantidad.addEventListener("change", () => {
        const nuevaCantidad = Number(cantidad.value);

        if (seleccionadas.size > nuevaCantidad) {
            cantidad.value = String(objetivo);
            estado.textContent =
                "Quita algunas carreras antes de reducir la cantidad.";
            return;
        }

        objetivo = nuevaCantidad;
        invalidarResultado();
        actualizarSeleccion();
        mostrarCatalogo();
    });

    botonComparar.addEventListener("click", comparar);

    cargarCatalogo();

    async function cargarCatalogo() {
        estado.textContent = "Cargando carreras...";

        try {
            const respuesta = await fetch("/api/carreras", {
                cache: "no-store"
            });

            if (!respuesta.ok) {
                throw new Error("No se pudo cargar el catálogo.");
            }

            carreras = await respuesta.json();

            cargarOpciones(
                facultad,
                [...new Set(carreras.map(c => c.facultad).filter(Boolean))]
                    .sort((a, b) => a.localeCompare(b)),
                "Todas"
            );

            cargarOpciones(
                duracion,
                [...new Set(carreras.map(c => c.duracion)
                    .filter(v => v != null))]
                    .sort((a, b) => a - b),
                "Todas"
            );

            cantidad.replaceChildren();

            for (let n = 2; n <= carreras.length; n++) {
                const opcion = crear("option", String(n));
                opcion.value = String(n);
                cantidad.appendChild(opcion);
            }

            cantidad.disabled = carreras.length < 2;

            actualizarSeleccion();
            mostrarCatalogo();

        } catch (error) {
            estado.textContent = error.message;
        }
    }

    function cargarOpciones(control, valores, primeraEtiqueta) {
        control.replaceChildren();

        const primera = crear("option", primeraEtiqueta);
        primera.value = "";
        control.appendChild(primera);

        valores.forEach(valor => {
            const opcion = crear("option", String(valor));
            opcion.value = String(valor);
            control.appendChild(opcion);
        });
    }

    function normalizar(texto) {
        return String(texto ?? "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim()
            .replace(/\s+/g, " ");
    }

    function mostrarCatalogo() {
        catalogo.replaceChildren();

        if (!costo.checkValidity()) {
            estado.textContent = "Introduce un costo máximo válido.";
            return;
        }

        const consulta = normalizar(buscar.value);
        const maximo = costo.value === "" ? null : Number(costo.value);

        const visibles = carreras.filter(c => {
            const texto = normalizar([
                c.nombre,
                ...(c.areasFormacion ?? []),
                ...(c.oportunidadesLaborales ?? [])
            ].join(" "));

            return texto.includes(consulta)
                && (!facultad.value || c.facultad === facultad.value)
                && (!duracion.value
                    || String(c.duracion) === duracion.value)
                && (maximo === null
                    || (c.costo != null && Number(c.costo) <= maximo));
        });

        estado.textContent = carreras.length < 2
            ? "Se necesitan al menos dos carreras registradas para comparar."
            : visibles.length === 0
                ? "No hay carreras que coincidan con los filtros."
                : `${visibles.length} carrera(s) encontradas.`;

        visibles.forEach(c => {
            const tarjeta = crear("article", "", "carrera-card");

            tarjeta.append(
                crear("h3", c.nombre),
                crear("p", c.facultad ?? "Facultad no disponible", "carrera-area"),
                crear(
                    "p",
                    `${c.duracion == null ? "No disponible" : c.duracion + " años"} · ${moneda(c.costo)}${c.costoPeriodo ? " · " + c.costoPeriodo : (c.costo == null ? "" : " · Período no disponible")}`,
                    "carrera-uni"
                )
            );

            const agregada = seleccionadas.has(c.id);

            const boton = crear(
                "button",
                agregada ? "Quitar" : "Agregar",
                "btn btn-pequeno"
            );

            boton.type = "button";
            boton.setAttribute("aria-pressed", String(agregada));

            boton.disabled = !agregada
                && seleccionadas.size >= objetivo;

            boton.addEventListener("click", () => {
                if (seleccionadas.has(c.id)) {
                    seleccionadas.delete(c.id);
                } else if (seleccionadas.size < objetivo) {
                    seleccionadas.set(c.id, c);
                }

                invalidarResultado();
                actualizarSeleccion();
                mostrarCatalogo();
            });

            tarjeta.appendChild(boton);
            catalogo.appendChild(tarjeta);
        });
    }

    function actualizarSeleccion() {
        seleccion.replaceChildren();
        contador.textContent =
            `${seleccionadas.size} de ${objetivo} carreras seleccionadas.`;

        seleccionadas.forEach(c => {
            const boton = crear(
                "button",
                `${c.nombre} ×`,
                "btn btn-secundario btn-pequeno"
            );

            boton.type = "button";
            boton.setAttribute("aria-label", `Quitar ${c.nombre}`);

            boton.addEventListener("click", () => {
                seleccionadas.delete(c.id);
                invalidarResultado();
                actualizarSeleccion();
                mostrarCatalogo();
            });

            seleccion.appendChild(boton);
        });

        actualizarBoton();
    }

    function actualizarBoton() {
        botonComparar.disabled = consultando
            || seleccionadas.size !== objetivo
            || seleccionadas.size < 2;

        botonComparar.textContent = consultando
            ? "Comparando..."
            : "Comparar";
    }

    function invalidarResultado() {
        revision++;
        resultado.hidden = true;
        tabla.replaceChildren();
    }

    async function comparar() {
        if (botonComparar.disabled) {
            return;
        }

        const revisionSolicitada = revision;
        const ids = [...seleccionadas.keys()].join(",");
        const parametros = new URLSearchParams({ ids });

        consultando = true;
        actualizarBoton();
        resultado.hidden = true;
        estado.textContent = "Consultando los datos actuales...";

        try {
            const respuesta = await fetch(
                "/api/comparador?" + parametros,
                { cache: "no-store" }
            );

            const datos = await respuesta.json().catch(() => ({}));

            if (!respuesta.ok) {
                throw new Error(
                    datos.error || "No se pudo realizar la comparación."
                );
            }

            // Si cambió la selección durante la petición,
            // no se muestra un resultado correspondiente a la anterior.
            if (revisionSolicitada !== revision) {
                return;
            }

            mostrarResultado(datos);
            estado.textContent = "Comparación generada.";

        } catch (error) {
            if (revisionSolicitada === revision) {
                estado.textContent = error.message;
            }
        } finally {
            consultando = false;
            actualizarBoton();
        }
    }

    function mostrarResultado(datos) {
        tabla.replaceChildren();

        const sonDos = datos.carreras.length === 2;
        const cabecera = crear("thead");
        const filaCabecera = crear("tr");

        const criterio = crear("th", "Criterio");
        criterio.scope = "col";
        filaCabecera.appendChild(criterio);

        datos.carreras.forEach((c, indice) => {
            const prefijo = sonDos
                ? `${indice === 0 ? "A" : "B"} — `
                : "";

            const th = crear("th", prefijo + c.nombre);
            th.scope = "col";
            filaCabecera.appendChild(th);
        });

        if (sonDos) {
            const th = crear("th", "Diferencias");
            th.scope = "col";
            filaCabecera.appendChild(th);
        }

        cabecera.appendChild(filaCabecera);

        const cuerpo = crear("tbody");

        datos.filas.forEach(fila => {
            const tr = crear("tr");
            const titulo = crear("th", fila.criterio);
            titulo.scope = "row";
            tr.appendChild(titulo);

            fila.valores.forEach((valor, indice) => {
                const td = crear("td");

                if (["pdfOficialUrl", "urlCarreraOficial"]
                    .includes(fila.campo)) {
                    td.appendChild(crearEnlace(valor));
                } else {
                    td.appendChild(crear("span", valor, "valor-comparador"));
                }

                const indicador = fila.indicadores[indice];

                if (indicador) {
                    const tipo = indicador.startsWith("↑")
                        ? "mayor"
                        : indicador.startsWith("↓") ? "menor" : "igual";

                    td.appendChild(crear(
                        "small",
                        indicador,
                        `marca-comparador marca-${tipo}`
                    ));
                }

                tr.appendChild(td);
            });

            if (sonDos) {
                tr.appendChild(
                    crear("td", fila.diferencia, "valor-comparador")
                );
            }

            cuerpo.appendChild(tr);
        });

        tabla.append(cabecera, cuerpo);
        resultado.hidden = false;
    }

    function crearEnlace(valor) {
        try {
            const url = new URL(valor);

            if (url.protocol === "https:") {
                const enlace = crear("a", "Abrir enlace");
                enlace.href = url.href;
                enlace.target = "_blank";
                enlace.rel = "noopener noreferrer";
                return enlace;
            }
        } catch {
            // Un dato vacío o no disponible se presenta como texto.
        }

        return crear("span", "No disponible");
    }

    function moneda(valor) {
        if (valor == null) {
            return "Costo no disponible";
        }

        return new Intl.NumberFormat("es-GT", {
            style: "currency",
            currency: "GTQ"
        }).format(valor);
    }

    function crear(etiqueta, texto = "", clase = "") {
        const elemento = document.createElement(etiqueta);
        elemento.textContent = texto;

        if (clase) {
            elemento.className = clase;
        }

        return elemento;
    }
});