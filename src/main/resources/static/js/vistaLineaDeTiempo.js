document.addEventListener('DOMContentLoaded', function () {
    const contenedor = document.getElementById('lista-actividades');

    function renderizarActividad(actividad) {
        const tarjeta = document.createElement('div');
        tarjeta.className = 'actividad-card' + (actividad.completada ? ' actividad-completada' : '');
        tarjeta.dataset.id = actividad.idActividad;

        tarjeta.innerHTML = `
            <div class="actividad-header">
                <input type="checkbox" class="actividad-checkbox"
                    ${actividad.completada ? 'checked' : ''}
                    data-id="${actividad.idActividad}">
                <span class="actividad-fecha">${actividad.fecha}</span>
                <span class="actividad-tipo">${actividad.tipoActividad}</span>
            </div>
            <h3>${actividad.descripcion}</h3>
            <p class="actividad-contexto">${actividad.contexto}</p>
        `;

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
                } else {
                    checkbox.checked = !checkbox.checked;
                }
            } catch (error) {
                checkbox.checked = !checkbox.checked;
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

            contenedor.innerHTML = '';
            if (actividades.length === 0) {
                contenedor.innerHTML = '<p>No hay actividades disponibles aun.</p>';
                return;
            }
            actividades.forEach(function (actividad) {
                contenedor.appendChild(renderizarActividad(actividad));
            });
        } catch (error) {
            contenedor.innerHTML = '<p>No se pudo cargar la linea de tiempo.</p>';
        }
    }

    cargarActividades();
});
