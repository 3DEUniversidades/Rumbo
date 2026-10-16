document.addEventListener('DOMContentLoaded', async function () {
    const contenedor = document.getElementById('contenido-rol');

    try {
        const sesion = await obtenerSesion();
        document.getElementById('nombre-usuario').textContent = sesion.nombreCompleto;
        document.getElementById('rol-usuario').textContent = sesion.rol;
        contenedor.hidden = false;
    } catch (error) {
        contenedor.textContent = 'No se pudo cargar la sesion.';
        contenedor.hidden = false;
    }
});
