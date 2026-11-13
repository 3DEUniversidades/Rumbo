document.addEventListener("DOMContentLoaded", () => {
    const buscar = document.getElementById("buscar-comparador");
    const facultad = document.getElementById("facultad-comparador");
    const universidad = document.getElementById("universidad-comparador");
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
    const panel = document.getElementById("panel-seleccion-comparador");
    const vistaSeleccion = document.getElementById("vista-seleccion-comparador");
    const paginacion = document.getElementById("paginacion-comparador");
    const paginaAnterior = document.getElementById("pagina-anterior-comparador");
    const paginaSiguiente = document.getElementById("pagina-siguiente-comparador");
    const estadoPagina = document.getElementById("pagina-comparador");
    const barraMovil = document.querySelector(".barra-superior-movil");

    function ajustarPanel() {
        const offset = barraMovil?.getBoundingClientRect().height || 0;
        panel.style.setProperty("--offset-comparador", `${offset}px`);
        resultado.style.scrollMarginTop = `${offset + 16}px`;
    }
    window.addEventListener("resize", ajustarPanel);
    if (window.ResizeObserver) {
        const observador = new ResizeObserver(ajustarPanel);
        observador.observe(panel);
        if (barraMovil) observador.observe(barraMovil);
    }
    ajustarPanel();

    let carreras = [];
    const seleccionadas = new Map();

    let objetivo = 2;
    let consultando = false;
    let revision = 0;
    const POR_PAGINA = 10;
    let paginaActual = 1;
    let scrollSeleccion = 0;

    [buscar, universidad, facultad, duracion, costo].forEach(control => {
        control.addEventListener("input", () => {
            paginaActual = 1;
            mostrarCatalogo();
        });
    });

    function cambiarPagina(cambio) {
        paginaActual += cambio;
        mostrarCatalogo();
        const primerBoton = catalogo.querySelector("button");
        if (primerBoton) {
            primerBoton.scrollIntoView({ block: "center" });
            primerBoton.focus({ preventScroll: true });
        }
    }
    paginaAnterior.addEventListener("click", () => cambiarPagina(-1));
    paginaSiguiente.addEventListener("click", () => cambiarPagina(1));
    document.getElementById("editar-seleccion-comparador").addEventListener("click", () => {
        revision++;
        resultado.hidden = true;
        vistaSeleccion.hidden = false;
        ajustarPanel();
        window.scrollTo(0, scrollSeleccion);
        botonComparar.focus({ preventScroll: true });
    });
    document.getElementById("nueva-comparacion").addEventListener("click", () => {
        seleccionadas.clear();
        [buscar, universidad, facultad, duracion, costo].forEach(control => { control.value = ""; });
        objetivo = 2;
        cantidad.value = "2";
        paginaActual = 1;
        invalidarResultado();
        vistaSeleccion.hidden = false;
        actualizarSeleccion();
        mostrarCatalogo();
        window.scrollTo(0, 0);
        buscar.focus();
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

    obtenerSesion().then(sesion => {
        if (sesion) return cargarCatalogo();
    }).catch(() => {
        estado.textContent = "No se pudo cargar la sesión. Recarga la página para intentarlo de nuevo.";
    });

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
            cargarOpciones(universidad,
                [...new Set(carreras.map(c => c.universidad).filter(Boolean))]
                    .sort((a, b) => a.localeCompare(b, "es")), "Todas");

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
            paginacion.hidden = true;
            estado.textContent = "Introduce un costo máximo válido.";
            return;
        }

        const consulta = normalizar(buscar.value);
        const maximo = costo.value === "" ? null : Number(costo.value);

        const visibles = carreras.filter(c => {
            const texto = normalizar([
                c.nombre,
                c.universidad,
                ...(c.areasFormacion ?? []),
                ...(c.oportunidadesLaborales ?? [])
            ].join(" "));

            return texto.includes(consulta)
                && (!universidad.value || c.universidad === universidad.value)
                && (!facultad.value || c.facultad === facultad.value)
                && (!duracion.value
                    || String(c.duracion) === duracion.value)
                && (maximo === null
                    || (c.costo != null && Number(c.costo) <= maximo));
        });

        const paginas = Math.max(1, Math.ceil(visibles.length / POR_PAGINA));
        paginaActual = Math.min(Math.max(1, paginaActual), paginas);
        const inicio = (paginaActual - 1) * POR_PAGINA;
        const dePagina = visibles.slice(inicio, inicio + POR_PAGINA);
        paginacion.hidden = visibles.length === 0;
        paginaAnterior.disabled = paginaActual === 1;
        paginaSiguiente.disabled = paginaActual === paginas;
        estadoPagina.textContent = `Página ${paginaActual} de ${paginas}`;

        estado.textContent = carreras.length < 2
            ? "Se necesitan al menos dos carreras registradas para comparar."
            : visibles.length === 0
                ? "No hay carreras que coincidan con los filtros."
                : `Mostrando ${inicio + 1}–${inicio + dePagina.length} de ${visibles.length} carreras.`;

        dePagina.forEach(c => {
            const tarjeta = crear("li", "", "fila-carrera-comparador");
            const informacion = crear("div");

            informacion.append(
                crear("h3", c.nombre),
                crear("p", c.universidad ?? "No disponible", "carrera-universidad")
            );
            tarjeta.appendChild(informacion);

            const agregada = seleccionadas.has(c.id);

            const boton = crear(
                "button",
                agregada ? "Quitar" : "Agregar",
                "btn btn-pequeno"
            );

            boton.type = "button";
            boton.setAttribute("aria-pressed", String(agregada));
            boton.setAttribute("aria-label", `${agregada ? "Quitar" : "Agregar"} ${c.nombre} · ${c.universidad ?? "No disponible"}`);

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
                `${c.nombre} · ${c.universidad ?? "No disponible"} ×`,
                "btn btn-secundario btn-pequeno"
            );

            boton.type = "button";
            boton.setAttribute("aria-label", `Quitar ${c.nombre} de ${c.universidad ?? "universidad no disponible"}`);

            boton.addEventListener("click", () => {
                seleccionadas.delete(c.id);
                invalidarResultado();
                actualizarSeleccion();
                mostrarCatalogo();
            });

            seleccion.appendChild(boton);
        });

        actualizarBoton();
        ajustarPanel();
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
        document.getElementById("tabla-general-comparador").replaceChildren();
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

            if (respuesta.status === 401) {
                window.location.replace("/login.html");
                return;
            }

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
        const sonDos = datos.carreras.length === 2;
        const general = document.getElementById("vista-general-comparador");
        general.hidden = sonDos;
        document.getElementById("detalle-comparador").open = sonDos;
        document.getElementById("abrir-detalle-comparador").hidden = sonDos;

        if (!sonDos) {
            const campos = ["universidad", "facultad", "duracion", "costo", "costoPeriodo",
                "costoAnio", "modalidad", "cargaCreditos"];
            llenarTabla(datos, document.getElementById("tabla-general-comparador"),
                datos.filas.filter(fila => campos.includes(fila.campo)));
        }
        llenarTabla(datos, tabla, datos.filas);
        scrollSeleccion = window.scrollY;
        vistaSeleccion.hidden = true;
        resultado.hidden = false;
        ajustarPanel();
        document.getElementById("titulo-resultado-comparador").focus({ preventScroll: true });
        resultado.scrollIntoView({ behavior: "instant", block: "start" });
    }

    function llenarTabla(datos, tabla, filas) {
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

            const th = crear("th", `${prefijo}${c.nombre} · ${c.universidad ?? "No disponible"}`);
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

        filas.forEach(fila => {
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
