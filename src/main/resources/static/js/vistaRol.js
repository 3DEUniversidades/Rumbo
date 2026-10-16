document.addEventListener('DOMContentLoaded', async function () {
    const contenedor = document.getElementById('contenido-rol');

    try {
        const sesion = await obtenerSesion();
        if (!sesion) {
            return;
        }
        document.getElementById('nombre-usuario').textContent = sesion.nombreCompleto;
        document.getElementById('rol-usuario').textContent = sesion.rol;
        document.getElementById('cerrar-sesion').addEventListener('click', async function () {
            this.disabled = true;
            try {
                const respuesta = await fetch('/api/auth/logout', { method: 'POST' });
                if (!respuesta.ok) {
                    throw new Error('No se pudo cerrar la sesion');
                }
                window.location.replace('/login.html');
            } catch (error) {
                this.disabled = false;
                this.textContent = 'No se pudo cerrar sesion';
            }
        });
        contenedor.hidden = false;
    } catch (error) {
        contenedor.textContent = 'No se pudo cargar la sesion.';
        contenedor.hidden = false;
    }
});
