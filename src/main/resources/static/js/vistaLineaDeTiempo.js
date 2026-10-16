document.addEventListener('DOMContentLoaded', function () {
    const contenedor = document.getElementById('lista-actividades');
    const estado = document.getElementById('estado-actividades');
    function avisar(texto) { if (estado) estado.textContent = texto; }

    function renderizarActividad(actividad) {
        const tarjeta = document.createElement('article');
        tarjeta.className = 'actividad-card' + (actividad.completada ? ' actividad-completada' : '');
        tarjeta.dataset.id = actividad.idActividad;

        const encabezado = document.createElement('div');
        encabezado.className = 'actividad-header';
        const control = document.createElement('input');
        control.type = 'checkbox';
        control.className = 'actividad-checkbox';
        control.checked = Boolean(actividad.completada);
        control.dataset.id = actividad.idActividad;
        control.setAttribute('aria-label', 'Completada: ' + (actividad.descripcion ?? 'actividad'));
        const fecha = document.createElement('time');
        fecha.className = 'actividad-fecha';
        // Fecha local sin conversión UTC: conserva el día y el orden del servidor.
        const fechaOriginal = String(actividad.fecha ?? '');
        fecha.textContent = fechaOriginal || 'No disponible';
        if (/^\d{4}-\d{2}-\d{2}$/.test(fechaOriginal)) {
            fecha.dateTime = fechaOriginal;
            const [anio, mes, dia] = fechaOriginal.split('-').map(Number);
            fecha.textContent = new Date(anio, mes - 1, dia).toLocaleDateString('es-GT', {
                day: 'numeric', month: 'long', year: 'numeric'
            });
        }
        const tipo = document.createElement('span');
        tipo.className = 'actividad-tipo';
        tipo.textContent = actividad.tipoActividad ?? 'No disponible';
        const estadoActividad = document.createElement('span');
        estadoActividad.className = 'actividad-estado';
        estadoActividad.textContent = actividad.completada ? 'Completada' : 'Pendiente';
        const titulo = document.createElement('h3');
        titulo.textContent = actividad.descripcion ?? 'No disponible';
        const contexto = document.createElement('p');
        contexto.className = 'actividad-contexto';
        contexto.textContent = actividad.contexto ?? 'No disponible';
        encabezado.append(control, fecha, tipo, estadoActividad);
        tarjeta.append(encabezado, titulo, contexto);

        const checkbox = tarjeta.querySelector('.actividad-checkbox');
        checkbox.addEventListener('change', async function () {
            checkbox.disabled = true;
            try {
                const respuesta = await fetch('/api/actividades/' + actividad.idActividad + '/toggle', {
                    method: 'PATCH'
                });
                if (respuesta.ok) {
                    const actualizada = await respuesta.json();
                    tarjeta.classList.toggle('actividad-completada', actualizada.completada);
                    checkbox.checked = Boolean(actualizada.completada);
                    estadoActividad.textContent = actualizada.completada ? 'Completada' : 'Pendiente';
                    avisar('Actividad actualizada.');
                } else {
                    checkbox.checked = !checkbox.checked;
                    avisar('No se pudo guardar el cambio. Inténtalo de nuevo.');
                }
            } catch (error) {
                checkbox.checked = !checkbox.checked;
                    avisar('No se pudo guardar el cambio. Inténtalo de nuevo.');
            } finally {
                checkbox.disabled = false;
            }
        });

        return tarjeta;
    }

    async function cargarActividades() {
        try {
            const respuesta = await fetch('/api/actividades');
            if (!respuesta.ok) throw new Error();
            const actividades = await respuesta.json();

            contenedor.replaceChildren();
            avisar('');
            if (actividades.length === 0) {
                avisar('No hay actividades disponibles aún.');
                return;
            }
            actividades.forEach(function (actividad) {
                contenedor.appendChild(renderizarActividad(actividad));
            });
        } catch (error) {
            avisar('No se pudo cargar la línea de tiempo.');
        }
    }

    obtenerSesion().then(function (sesion) {
        if (sesion && sesion.rol === 'ESTUDIANTE') {
            cargarActividades();
        } else if (sesion) {
            avisar('No tienes permisos para ver esta línea de tiempo.');
        }
    }).catch(function () {
        avisar('No se pudo cargar la sesión.');
    });
});
