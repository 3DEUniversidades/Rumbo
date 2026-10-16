document.addEventListener('DOMContentLoaded', async function () {
    try {
        const sesion = await obtenerSesion();
        if (sesion) {
            document.getElementById('saludo').textContent = 'Hola, ' + sesion.nombreCompleto;
        }
    } catch (error) {
        document.getElementById('saludo').textContent = 'No se pudo cargar la sesion';
    }
});