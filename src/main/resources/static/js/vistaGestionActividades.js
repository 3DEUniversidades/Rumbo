document.addEventListener('DOMContentLoaded', async () => {
    const acceso = document.getElementById('acceso-actividades');
    const contenido = document.getElementById('gestion-actividades');
    const lista = document.getElementById('catalogo-actividades');
    const mensaje = document.getElementById('mensaje-actividades');
    const dialogo = document.getElementById('editor-actividad');
    const formulario = document.getElementById('formulario-actividad');
    const errorFormulario = document.getElementById('error-actividad');
    const titulo = document.getElementById('titulo-editor-actividad');
    const nueva = document.getElementById('nueva-actividad');
    const actualizar = document.getElementById('actualizar-actividades');
    const guardar = document.getElementById('guardar-actividad');
    const cancelar = document.getElementById('cancelar-actividad');
    const campos = document.getElementById('campos-actividad');
    const etiquetas = {
        fecha: 'Fecha', descripcion: 'Descripción', contexto: 'Contexto',
        instrucciones: 'Instrucciones', accionesARealizar: 'Acciones a realizar',
        metodoRecomendado: 'Método recomendado', herramientasDisponibles: 'Herramientas disponibles',
        informacionAGuardar: 'Información que debe guardar', tipoActividad: 'Tipo'
    };
    let idEditando = null;
    let ocupado = false;
    let hayCambios = false;

    async function api(ruta, opciones = {}) {
        const respuesta = await fetch(ruta, {
            credentials: 'same-origin', cache: 'no-store', ...opciones
        });
        if (respuesta.status === 401) {
            window.location.replace('/login.html');
            throw new Error('Tu sesión terminó. Inicia sesión nuevamente.');
        }
        if (!respuesta.ok) {
            const datos = await respuesta.json().catch(() => ({}));
            const detalle = datos.campos
                ? Object.entries(datos.campos).map(([campo, texto]) =>
                    `${etiquetas[campo] ?? campo}: ${texto}`).join('\n')
                : '';
            throw new Error(detalle || datos.error || `No se pudo realizar la operación (${respuesta.status}).`);
        }
        return respuesta.status === 204 ? null : respuesta.json();
    }

    function bloquear(valor) {
        ocupado = valor;
        campos.disabled = valor;
        guardar.disabled = valor;
        cancelar.disabled = valor;
        nueva.disabled = valor;
        actualizar.disabled = valor;
        lista.querySelectorAll('button').forEach(boton => { boton.disabled = valor; });
        contenido.setAttribute('aria-busy', String(valor));
    }

    async function cargarLista() {
        const actividades = await api('/api/actividades');
        lista.replaceChildren(...actividades.map(crearTarjeta));
        return actividades.length;
    }

    async function refrescar() {
        if (ocupado) return;
        bloquear(true);
        mensaje.textContent = 'Consultando actividades...';
        try {
            const cantidad = await cargarLista();
            mensaje.textContent = cantidad ? `${cantidad} actividades disponibles.` : 'No hay actividades disponibles. Crea la primera.';
        } catch (error) {
            mensaje.textContent = error.message + ' Usa «Actualizar lista» para reintentar.';
        } finally { bloquear(false); }
    }

    function crearTarjeta(actividad) {
        const tarjeta = document.createElement('article');
        tarjeta.className = 'tarjeta-gestion-actividad';
        const nombre = document.createElement('h3');
        nombre.textContent = actividad.descripcion;
        const fecha = document.createElement('p');
        fecha.textContent = `Fecha: ${actividad.fecha} · Tipo: ${actividad.tipoActividad}`;
        const detalles = document.createElement('details');
        const abrir = document.createElement('summary');
        abrir.textContent = 'Ver todos los datos';
        const datos = document.createElement('dl');
        Object.entries(etiquetas).forEach(([campo, etiqueta]) => {
            const dt = document.createElement('dt'); dt.textContent = etiqueta;
            const dd = document.createElement('dd'); dd.textContent = actividad[campo] ?? 'Sin información';
            datos.append(dt, dd);
        });
        detalles.append(abrir, datos);
        const acciones = document.createElement('div'); acciones.className = 'acciones-actividades';
        const editar = document.createElement('button'); editar.type = 'button'; editar.className = 'btn'; editar.textContent = 'Editar';
        const eliminar = document.createElement('button'); eliminar.type = 'button'; eliminar.className = 'btn btn-secundario'; eliminar.textContent = 'Eliminar';
        editar.addEventListener('click', () => abrirEditor(actividad.idActividad));
        eliminar.addEventListener('click', () => retirar(actividad));
        acciones.append(editar, eliminar);
        tarjeta.append(nombre, fecha, detalles, acciones);
        return tarjeta;
    }

    async function abrirEditor(id = null) {
        if (ocupado) return;
        bloquear(true);
        try {
            const actividad = id ? await api('/api/actividades/' + encodeURIComponent(id)) : null;
            formulario.reset();
            errorFormulario.textContent = '';
            idEditando = id;
            hayCambios = false;
            Object.keys(etiquetas).forEach(campo => {
                formulario.elements.namedItem(campo).value = actividad?.[campo] ?? '';
            });
            titulo.textContent = id ? 'Editar actividad' : 'Crear actividad';
            dialogo.showModal();
        } catch (error) { mensaje.textContent = error.message; }
        finally { bloquear(false); }
    }

    function cerrarEditor() {
        if (ocupado) return;
        if (hayCambios && !window.confirm('¿Descartar los cambios que no has guardado?')) return;
        dialogo.close();
    }
    dialogo.addEventListener('cancel', evento => { evento.preventDefault(); cerrarEditor(); });
    cancelar.addEventListener('click', cerrarEditor);
    formulario.addEventListener('input', evento => {
        hayCambios = true;
        if (typeof evento.target.setCustomValidity === 'function') evento.target.setCustomValidity('');
    });
    window.addEventListener('beforeunload', evento => {
        if (dialogo.open && hayCambios) { evento.preventDefault(); evento.returnValue = ''; }
    });

    formulario.addEventListener('submit', async evento => {
        evento.preventDefault();
        if (ocupado) return;
        const solicitud = {};
        for (const campo of Object.keys(etiquetas)) {
            const entrada = formulario.elements.namedItem(campo);
            entrada.setCustomValidity('');
            solicitud[campo] = entrada.value.trim();
            if (!solicitud[campo]) {
                entrada.setCustomValidity('Este campo es obligatorio.');
                entrada.reportValidity();
                return;
            }
        }
        bloquear(true);
        errorFormulario.textContent = '';
        let guardado = false;
        try {
            const ruta = '/api/actividades' + (idEditando ? '/' + encodeURIComponent(idEditando) : '');
            await api(ruta, {
                method: idEditando ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(solicitud)
            });
            guardado = true;
            hayCambios = false;
            dialogo.close();
            mensaje.textContent = 'Actividad guardada. Los estudiantes verán el cambio al actualizar su línea de tiempo.';
            await cargarLista();
        } catch (error) {
            if (guardado) mensaje.textContent = 'Se guardó la actividad, pero no se pudo actualizar la lista. Pulsa «Actualizar lista».';
            else errorFormulario.textContent = error.message;
        } finally { bloquear(false); }
    });

    async function retirar(actividad) {
        if (ocupado) return;
        if (!window.confirm(`¿Eliminar «${actividad.descripcion}» de la línea de tiempo? Los avances y resultados ya registrados se conservarán.`)) return;
        bloquear(true);
        let eliminado = false;
        try {
            await api('/api/actividades/' + encodeURIComponent(actividad.idActividad), { method: 'DELETE' });
            eliminado = true;
            mensaje.textContent = 'Actividad eliminada de la línea de tiempo.';
            await cargarLista();
        } catch (error) {
            mensaje.textContent = eliminado
                ? 'La actividad se eliminó, pero no se pudo actualizar la lista. Pulsa «Actualizar lista».'
                : error.message;
        } finally { bloquear(false); }
    }

    nueva.addEventListener('click', () => abrirEditor());
    actualizar.addEventListener('click', refrescar);
    try {
        const sesion = await obtenerSesion();
        if (!sesion) return;
        if (sesion.rol !== 'ENCARGADO_DE_CARRERAS') {
            acceso.textContent = 'Esta página está disponible únicamente para el encargado de carreras.';
            return;
        }
        acceso.hidden = true;
        contenido.hidden = false;
        await refrescar();
    } catch (error) { acceso.textContent = error.message; }
});
