const CACHE_NAME = "rumbo-v14-multiuniversidad";
const ASSETS = [
    "/style.css",
    "/css/lineaTiempo.css",
    "/css/ilustraciones.css",
    "/css/identidadAreas.css",
    "/css/carrerasUniversidades.css",
    "/icons/ilustraciones/carreras.svg",
    "/icons/ilustraciones/comparador.svg",
    "/icons/ilustraciones/bitacora.svg",
    "/icons/ilustraciones/pruebas.svg",
    "/icons/ilustraciones/foro.svg",
    "/icons/ilustraciones/recorrido.svg",
    "/icons/ilustraciones/perfil.svg",
    "/icons/ilustraciones/actividades.svg",
    "/icons/ilustraciones/usuarios.svg",
    "/icons/ilustraciones/registros.svg",
    "/manifest.json",
    "/js/rumbo.js",
    "/icons/fonts/PublicSans-Regular.woff2",
    "/icons/fonts/PublicSans-Medium.woff2",
    "/icons/fonts/PublicSans-SemiBold.woff2",
    "/icons/fonts/PublicSans-Bold.woff2"
];

self.addEventListener("install", event => {
    event.waitUntil((async () => {
        const cache = await caches.open(CACHE_NAME);
        await cache.addAll(ASSETS);
        await self.skipWaiting();
    })());
});

self.addEventListener("activate", event => {
    event.waitUntil((async () => {
        const nombres = await caches.keys();

        await Promise.all(
            nombres
                .filter(nombre =>
                    nombre.startsWith("rumbo-")
                    && nombre !== CACHE_NAME
                )
                .map(nombre => caches.delete(nombre))
        );

        await self.clients.claim();
    })());
});

self.addEventListener("fetch", event => {
    const url = new URL(event.request.url);

    if (event.request.method !== "GET"
        || url.origin !== self.location.origin
        || !ASSETS.includes(url.pathname)) {
        return;
    }

    event.respondWith((async () => {
        const cache = await caches.open(CACHE_NAME);

        try {
            const respuesta = await fetch(event.request);

            if (respuesta.ok) {
                await cache.put(
                    event.request,
                    respuesta.clone()
                );
            }

            return respuesta;

        } catch (error) {
            const guardada = await cache.match(event.request);

            if (guardada) return guardada;

            throw error;
        }
    })());
});
