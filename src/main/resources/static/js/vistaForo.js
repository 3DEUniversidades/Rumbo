document.addEventListener("DOMContentLoaded", async () => {
    const obtener = id => document.getElementById(id);

    const mensaje = obtener("mensaje-foro");
    const contenido = obtener("contenido-foro");
    const login = obtener("login-foro");

    const formulario = obtener("form-publicacion");
    const tipoPublicacion = obtener("tipo-publicacion");
    const opcionesCarreras = obtener("opciones-carreras-foro");
    const botonPublicar = obtener("publicar-foro");

    const filtros = obtener("filtros-foro");
    const filtroTipo = obtener("filtro-tipo-foro");
    const filtroCarrera = obtener("filtro-carrera-foro");
    const estadoListado = obtener("estado-listado-foro");
    const listado = obtener("lista-publicaciones-foro");

    const modal = obtener("detalle-foro");
    const estadoDetalle = obtener("estado-detalle-foro");
    const contenidoDetalle = obtener("contenido-detalle-foro");
    const respuestas = obtener("respuestas-foro");
    const formularioRespuesta = obtener("form-respuesta-foro");
    const botonResponder = obtener("responder-foro");

    const tipos = {
        DUDA: "Duda",
        EXPERIENCIA: "Experiencia",
        PROYECTO: "Proyecto",
        ACTIVIDAD: "Actividad",
        OPORTUNIDAD: "Oportunidad"
    };

    const autorFiltro = new URLSearchParams(location.search).get("autor");

    let sesion = null;
    let publicacionAbierta = null;
    let revisionDetalle = 0;
    let revisionListado = 0;
    let publicando = false;
    let respondiendo = false;

    try {
        sesion = await obtenerSesion();

        if (!sesion) return;

    } catch (error) {
        mensaje.textContent = error.message;
        login.hidden = false;
        return;
    }

    if (!sesion?.idUsuario) {
        mensaje.textContent = "Inicia sesión para entrar al foro.";
        login.hidden = false;
        return;
    }

    obtener("cerrar-detalle-foro").addEventListener(
        "click", () => modal.close()
    );

    modal.addEventListener("close", () => {
        revisionDetalle++;
        publicacionAbierta = null;
    });

    filtros.addEventListener("submit", evento => {
        evento.preventDefault();
        cargarListado();
    });

    opcionesCarreras.addEventListener("change", actualizarCarreras);
    formulario.addEventListener("submit", publicar);
    formularioRespuesta.addEventListener("submit", responder);

    iniciar();

    async function iniciar() {
        try {
            const acceso = await solicitar("/api/foro/acceso");

            Object.entries(tipos).forEach(([valor, etiqueta]) => {
                agregarOpcion(filtroTipo, valor, etiqueta);

                if (acceso.rol === "COLABORADOR" || valor === "DUDA") {
                    agregarOpcion(tipoPublicacion, valor, etiqueta);
                }
            });

            contenido.hidden = false;
            mensaje.textContent = "";

            obtener("filtro-autor-foro").hidden = !autorFiltro;

            await Promise.all([
                cargarListado(),
                cargarCarreras()
            ]);

        } catch (error) {
            mensaje.textContent = error.message;
            login.hidden = false;
        }
    }

    async function solicitar(url, opciones = {}) {
        const respuesta = await fetch(url, {
            ...opciones,
            credentials: "same-origin",
            cache: "no-store",
            headers: {
                "Content-Type": "application/json",
                ...(opciones.headers ?? {})
            }
        });

        const datos = await respuesta.json().catch(() => ({}));

        if (!respuesta.ok) {
            throw new Error(
                datos.error || "No se pudo completar la operación."
            );
        }

        return datos;
    }

    async function cargarCarreras() {
        const estadoCarreras = obtener("estado-carreras-foro");

        try {
            const carreras = await solicitar("/api/carreras");

            carreras.forEach(carrera => {
                agregarOpcion(
                    filtroCarrera, carrera.id, carrera.nombre
                );

                const etiqueta = crear("label");
                const casilla = crear("input");
                casilla.type = "checkbox";
                casilla.value = String(carrera.id);

                etiqueta.append(
                    casilla,
                    crear("span", carrera.nombre)
                );

                opcionesCarreras.appendChild(etiqueta);
            });

            estadoCarreras.textContent = carreras.length === 0
                ? "No hay carreras cargadas. Puedes publicar sin relacionarlas."
                : "";

        } catch {
            estadoCarreras.textContent =
                "No se cargó el catálogo. Puedes hacer una publicación general.";
        }
    }

    function actualizarCarreras() {
        const casillas = [...opcionesCarreras.querySelectorAll("input")];
        const seleccionadas = casillas.filter(c => c.checked).length;

        casillas.forEach(casilla => {
            casilla.disabled = seleccionadas >= 3 && !casilla.checked;
        });
    }

    async function cargarListado() {
        const revision = ++revisionListado;
        estadoListado.textContent = "Cargando publicaciones...";

        const parametros = new URLSearchParams({
            q: obtener("buscar-foro").value.trim(),
            tipo: filtroTipo.value
        });

        if (filtroCarrera.value) {
            parametros.set("carreraId", filtroCarrera.value);
        }

        if (autorFiltro) {
            parametros.set("autor", autorFiltro);
        }

        try {
            const publicaciones = await solicitar(
                "/api/foro/publicaciones?" + parametros
            );

            if (revision !== revisionListado) {
                return;
            }

            listado.replaceChildren();

            estadoListado.textContent = publicaciones.length === 0
                ? "No hay publicaciones para estos filtros."
                : "";

            publicaciones.forEach(p => {
                const tarjeta = crear("article", "", "carrera-card");

                tarjeta.append(
                    crear("span", tipos[p.tipo] ?? p.tipo, "etiqueta-foro"),
                    crear("h3", p.titulo),
                    crearAutor(p.autor, p.fecha),
                    crear(
                        "p",
                        p.contenido.length > 220
                            ? p.contenido.slice(0, 220) + "…"
                            : p.contenido,
                        "texto-publicacion-foro"
                    ),
                    crear("p", nombresCarreras(p), "carrera-area")
                );

                if (p.estado === "VALIDADA") {
                    tarjeta.appendChild(
                        crear("small", "Publicación validada", "carrera-area")
                    );
                }

                const abrir = crear(
                    "button", "Abrir publicación", "btn btn-pequeno"
                );
                abrir.type = "button";
                abrir.addEventListener(
                    "click", () => abrirPublicacion(p.id)
                );

                tarjeta.appendChild(abrir);
                listado.appendChild(tarjeta);
            });

        } catch (error) {
            if (revision === revisionListado) {
                listado.replaceChildren();
                estadoListado.textContent = error.message;
            }
        }
    }

    async function publicar(evento) {
        evento.preventDefault();

        if (publicando) {
            return;
        }

        const carrerasIds = [
            ...opcionesCarreras.querySelectorAll("input:checked")
        ].map(c => Number(c.value));

        const solicitud = {
            titulo: obtener("titulo-publicacion").value.trim(),
            contenido: obtener("contenido-publicacion").value.trim(),
            tipo: tipoPublicacion.value,
            carrerasIds
        };

        publicando = true;
        botonPublicar.disabled = true;
        botonPublicar.textContent = "Publicando...";
        mensaje.textContent = "";

        try {
            const publicacion = await solicitar("/api/foro/publicaciones", {
                method: "POST",
                body: JSON.stringify(solicitud)
            });

            formulario.reset();
            actualizarCarreras();

            mensaje.textContent = "Publicación creada.";
            await cargarListado();
            await abrirPublicacion(publicacion.id);

        } catch (error) {
            mensaje.textContent = error.message;
        } finally {
            publicando = false;
            botonPublicar.disabled = false;
            botonPublicar.textContent = "Publicar";
        }
    }

    async function abrirPublicacion(id) {
        const revision = ++revisionDetalle;
        publicacionAbierta = null;

        contenidoDetalle.hidden = true;
        estadoDetalle.textContent = "Cargando publicación...";

        if (!modal.open) {
            modal.showModal();
        }

        try {
            const [publicacion, listaRespuestas] = await Promise.all([
                solicitar(`/api/foro/publicaciones/${id}`),
                solicitar(`/api/foro/publicaciones/${id}/respuestas`)
            ]);

            if (revision !== revisionDetalle || !modal.open) {
                return;
            }

            publicacionAbierta = publicacion;

            obtener("titulo-detalle-foro").textContent =
                publicacion.titulo;

            obtener("autor-detalle-foro").replaceChildren(
                crearAutor(publicacion.autor, publicacion.fecha)
            );

            obtener("carreras-detalle-foro").textContent =
                nombresCarreras(publicacion);

            obtener("texto-detalle-foro").textContent =
                publicacion.contenido;

            respuestas.replaceChildren();

            if (listaRespuestas.length === 0) {
                respuestas.appendChild(crear(
                    "p",
                    "Todavía no hay respuestas.",
                    "texto-secundario sin-respuestas-foro"
                ));
            } else {
                listaRespuestas.forEach(agregarRespuesta);
            }

            formularioRespuesta.reset();
            estadoDetalle.textContent = "";
            contenidoDetalle.hidden = false;

        } catch (error) {
            if (revision === revisionDetalle && modal.open) {
                estadoDetalle.textContent = error.message;
            }
        }
    }

    async function responder(evento) {
        evento.preventDefault();

        if (!publicacionAbierta || respondiendo) {
            return;
        }

        const id = publicacionAbierta.id;
        const revision = revisionDetalle;

        respondiendo = true;
        botonResponder.disabled = true;
        botonResponder.textContent = "Enviando...";
        estadoDetalle.textContent = "";

        try {
            const respuesta = await solicitar(
                `/api/foro/publicaciones/${id}/respuestas`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        contenido: obtener("texto-respuesta-foro")
                            .value.trim()
                    })
                }
            );

            if (revision !== revisionDetalle || !modal.open) {
                return;
            }

            respuestas.querySelector(".sin-respuestas-foro")?.remove();
            agregarRespuesta(respuesta);
            formularioRespuesta.reset();
            estadoDetalle.textContent = "Respuesta publicada.";

        } catch (error) {
            if (revision === revisionDetalle && modal.open) {
                estadoDetalle.textContent = error.message;
            }
        } finally {
            respondiendo = false;
            botonResponder.disabled = false;
            botonResponder.textContent = "Responder";
        }
    }

    function agregarRespuesta(respuesta) {
        const tarjeta = crear("article", "", "respuesta-foro");

        tarjeta.append(
            crearAutor(respuesta.autor, respuesta.fecha),
            crear("p", respuesta.contenido, "texto-publicacion-foro")
        );

        respuestas.appendChild(tarjeta);
    }

    function crearAutor(autor, fecha) {
        const contenedor = crear("p", "", "autor-foro");
        const enlace = crear("a", "@" + autor.nombre);

        enlace.href = "/foro.html?autor="
            + encodeURIComponent(autor.idUsuario);
        enlace.title = "Ver publicaciones de este autor";

        const fechaFormateada = new Date(fecha).toLocaleString("es-GT", {
            dateStyle: "medium",
            timeStyle: "short"
        });

        contenedor.append(
            enlace,
            crear("span", " · " + fechaFormateada)
        );

        return contenedor;
    }

    function nombresCarreras(publicacion) {
        return publicacion.carreras.length === 0
            ? "Publicación general"
            : publicacion.carreras.map(c => c.nombre).join(" · ");
    }

    function agregarOpcion(select, valor, etiqueta) {
        const opcion = crear("option", etiqueta);
        opcion.value = String(valor);
        select.appendChild(opcion);
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