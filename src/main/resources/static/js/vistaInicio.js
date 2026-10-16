document.addEventListener('DOMContentLoaded', function () {
    obtenerSesion()
        .then(function (sesion) {
            if (sesion) {
                document.getElementById('saludo').textContent = 'Hola, ' + sesion.nombreCompleto;
            }
        })
        .catch(function () {
            document.getElementById('saludo').textContent = 'No se pudo cargar la sesion';
        });
});