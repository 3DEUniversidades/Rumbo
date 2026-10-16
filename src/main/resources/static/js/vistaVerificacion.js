document.addEventListener('DOMContentLoaded', async function () {
    const contenedor = document.getElementById('verificacion-estudiante');
    const mensaje = document.getElementById('mensaje-verificacion');
    const boton = document.getElementById('solicitar-verificacion');

    try {
        const sesion = await obtenerSesion();
        if (!sesion || sesion.rol !== 'ESTUDIANTE') {
            return;
        }

        contenedor.hidden = false;
        await cargarEstado();
    } catch (error) {
        mensaje.textContent = 'No se pudo consultar el estado de verificación.';
        contenedor.hidden = false;
    }

    async function cargarEstado() {
        const respuesta = await fetch('/api/verificacion/estado');
        if (respuesta.status === 401) {
            window.location.replace('/login.html');
            return;
        }
        if (!respuesta.ok) {
            throw new Error('No se pudo consultar el estado de verificación');
        }

        const estado = await respuesta.json();
        if (estado.estadoVerificacion === 'verificado') {
            contenedor.hidden = true;
            return;
        }

        mensaje.textContent = estado.mensaje;
        boton.hidden = estado.solicitudPendiente;
        boton.disabled = false;
    }

    boton.addEventListener('click', async function () {
        boton.disabled = true;
        try {
            const respuesta = await fetch('/api/verificacion/solicitudes', {
                method: 'POST'
            });
            if (respuesta.status === 401) {
                window.location.replace('/login.html');
                return;
            }
            if (!respuesta.ok) {
                const cuerpo = await respuesta.json().catch(function () {
                    return {};
                });
                throw new Error(cuerpo.error || 'No se pudo enviar la solicitud.');
            }

            await cargarEstado();
        } catch (error) {
            mensaje.textContent = error.message;
            boton.hidden = false;
            boton.disabled = false;
        }
    });
});
