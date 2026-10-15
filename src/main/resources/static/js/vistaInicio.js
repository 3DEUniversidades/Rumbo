document.addEventListener('DOMContentLoaded', function () {
    const sesion = JSON.parse(localStorage.getItem('sesionRumbo'));

    if (!sesion) {
        window.location.replace('/login.html');
        return;
    }

    document.getElementById('saludo').textContent = 'Hola, ' + sesion.nombreCompleto;
    if (sesion.rol === 'ENCARGADO_CARRERAS') {
        document.getElementById('enlace-gestion-carreras').hidden = false;
    }
});