INSERT INTO carreras (nombre, duracion, pensum, costo)
SELECT 'Ingenieria en Ciencias de la Computacion', 5,
       'Programacion, estructuras de datos, bases de datos, redes, ingenieria de software e inteligencia artificial.',
       57500.00
WHERE NOT EXISTS (
    SELECT 1 FROM carreras WHERE nombre = 'Ingenieria en Ciencias de la Computacion'
);

INSERT INTO carreras (nombre, duracion, pensum, costo)
SELECT 'Ingenieria Industrial', 5,
       'Matematica, estadistica, administracion de operaciones, logistica, calidad, finanzas y gestion de proyectos.',
       54800.00
WHERE NOT EXISTS (
    SELECT 1 FROM carreras WHERE nombre = 'Ingenieria Industrial'
);

INSERT INTO carreras (nombre, duracion, pensum, costo)
SELECT 'Administracion de Empresas', 4,
       'Contabilidad, economia, mercadeo, finanzas, gestion del talento humano, emprendimiento y estrategia empresarial.',
       46200.00
WHERE NOT EXISTS (
    SELECT 1 FROM carreras WHERE nombre = 'Administracion de Empresas'
);

INSERT INTO carreras (nombre, duracion, pensum, costo)
SELECT 'Psicologia Clinica', 5,
       'Bases biologicas de la conducta, evaluacion psicologica, psicoterapia, investigacion, etica y practica supervisada.',
       48900.00
WHERE NOT EXISTS (
    SELECT 1 FROM carreras WHERE nombre = 'Psicologia Clinica'
);

INSERT INTO carreras (nombre, duracion, pensum, costo)
SELECT 'Arquitectura', 5,
       'Diseno arquitectonico, dibujo tecnico, urbanismo, estructuras, construccion, historia de la arquitectura y sostenibilidad.',
       61000.00
WHERE NOT EXISTS (
    SELECT 1 FROM carreras WHERE nombre = 'Arquitectura'
);
