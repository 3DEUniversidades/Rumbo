/* Preferencia visual local, independiente de los datos y permisos de la cuenta. */
(() => {
    "use strict";

    const CLAVE = "rumbo-apariencia";
    const raiz = document.documentElement;
    const validar = valor => valor === "claro" ? "claro" : "oscuro";
    let modo = "oscuro";

    try {
        modo = validar(localStorage.getItem(CLAVE));
    } catch {
        // La interfaz sigue disponible si el navegador bloquea el almacenamiento.
    }

    function aplicar(valor) {
        modo = validar(valor);
        const claro = modo === "claro";
        raiz.dataset.apariencia = modo;
        raiz.style.colorScheme = claro ? "light" : "dark";
        document.querySelector('meta[name="color-scheme"]')
            ?.setAttribute("content", claro ? "light" : "dark");
        document.querySelector('meta[name="theme-color"]')
            ?.setAttribute("content", claro ? "#E8EDF3" : "#0C1420");

        document.querySelectorAll("[data-modo-apariencia]").forEach(boton => {
            boton.setAttribute("aria-pressed", String(boton.dataset.modoApariencia === modo));
        });
        const estado = document.getElementById("estado-apariencia");
        if (estado) estado.textContent = `Modo ${modo} activado.`;
    }

    // Se ejecuta en el encabezado para aplicar la elección antes de mostrar la página.
    aplicar(modo);

    document.addEventListener("DOMContentLoaded", () => {
        aplicar(modo);
        document.querySelectorAll("[data-modo-apariencia]").forEach(boton => {
            boton.addEventListener("click", () => {
                aplicar(boton.dataset.modoApariencia);
                try {
                    localStorage.setItem(CLAVE, modo);
                } catch {
                    const estado = document.getElementById("estado-apariencia");
                    if (estado) estado.textContent = `Modo ${modo} activado. No se pudo guardar la preferencia en este navegador.`;
                }
            });
        });
    });

    window.addEventListener("storage", evento => {
        if (evento.key === CLAVE || evento.key === null) aplicar(evento.newValue);
    });
})();
