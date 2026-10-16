INSERT INTO carreras (
    nombre,
    duracion,
    costo,
    modalidad,
    carga_creditos,
    habilidades_requeridas,
    oportunidades_laborales,
    salario_estimado,
    empleabilidad,
    pdf_oficial_url,
    url_carrera_oficial
)

SELECT datos.*

FROM (
    VALUES
    (
        'Ingenieria en Ciencias de la Computacion',
        5,
        57500.00,
        'Presencial',
        220,
        'Razonamiento lógico, matemáticas y resolución de problemas.',
        'Desarrollo de software, análisis de datos y soporte tecnológico.',
        8500.00,
        82.00,
        'https://example.org/pensum/computacion.pdf',
        'https://example.org/carreras/computacion'
    ),
    (
        'Ingenieria Industrial',
        5,
        54800.00,
        'Presencial',
        215,
        'Análisis de procesos, matemáticas y organización.',
        'Producción, logística, calidad y gestión de proyectos.',
        7800.00,
        79.00,
        'https://example.org/pensum/industrial.pdf',
        'https://example.org/carreras/industrial'
    ),
    (
        'Administracion de Empresas',
        4,
        46200.00,
        'Híbrida',
        190,
        'Comunicación, organización y análisis financiero.',
        'Administración, mercadeo, finanzas y emprendimiento.',
        6500.00,
        76.00,
        'https://example.org/pensum/administracion.pdf',
        'https://example.org/carreras/administracion'
    ),
    (
        'Psicologia Clinica',
        5,
        48900.00,
        'Presencial',
        205,
        'Escucha activa, investigación y comunicación.',
        'Atención psicológica, investigación y programas de bienestar.',
        6200.00,
        71.00,
        'https://example.org/pensum/psicologia.pdf',
        'https://example.org/carreras/psicologia'
    ),
    (
        'Arquitectura',
        5,
        61000.00,
        'Presencial',
        225,
        'Diseño, creatividad, dibujo técnico y análisis espacial.',
        'Diseño arquitectónico, urbanismo y supervisión de obras.',
        7200.00,
        74.00,
        'https://example.org/pensum/arquitectura.pdf',
        'https://example.org/carreras/arquitectura'
    ),
    (
        'Medicina Veterinaria',
        5,
        59000.00,
        'Presencial',
        230,
        'Biología, observación, trabajo práctico y cuidado animal.',
        'Clínicas veterinarias, salud pública e investigación.',
        6800.00,
        73.00,
        'https://example.org/pensum/veterinaria.pdf',
        'https://example.org/carreras/veterinaria'
    )
) 

AS datos (
    nombre,
    duracion,
    costo,
    modalidad,
    carga_creditos,
    habilidades_requeridas,
    oportunidades_laborales,
    salario_estimado,
    empleabilidad,
    pdf_oficial_url,
    url_carrera_oficial
)

WHERE NOT EXISTS (
    SELECT 1
    FROM carreras AS existente
    WHERE existente.nombre = datos.nombre
);





INSERT INTO usuario (id_usuario, nombre_usuario, nombre_completo, correo, contrasenia, rol, estado_verificacion)
VALUES ('usr-0001', 'admin', 'Administrador General', 'admin@rumbo.com',
        '$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji',
        'ADMIN', 'VERIFICADO')
ON CONFLICT (id_usuario) DO UPDATE SET
    contrasenia = EXCLUDED.contrasenia,
    correo = EXCLUDED.correo;
     
INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones, acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-0001', '2026-01-15', 'Autoevaluación de intereses vocacionales', 'Primer contacto del estudiante con el proceso de orientación', 'Completa el test de intereses vocacionales de forma honesta y sin apuros', 'Realizar el test externo y anotar tus 3 áreas de interés principales', 'Prueba estandarizada de intereses (tipo Holland)', 'Enlace externo a prueba vocacional, papel y lápiz', 'Guarda tus 3 áreas de interés principales en tu bitácora', 'Autoevaluación')
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones, acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-0002', '2026-02-10', 'Exploración de carreras afines', 'Después de conocer tus intereses, busca carreras que coincidan', 'Investiga al menos 5 carreras relacionadas con tus áreas de interés', 'Consultar guías de carreras y anotar pros y contras de cada una', 'Investigación documental y comparativa', 'Guías universitarias, internet, revistas de orientación', 'Lista de 5 carreras con sus principales fortalezas', 'Exploración')
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones, acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-0003', '2026-03-05', 'Prueba de aptitudes académicas', 'Evaluación de fortalezas cognitivas para orientar la decisión', 'Realiza la prueba de aptitudes en un ambiente tranquilo y sin distracciones', 'Completar el cuestionario de aptitudes y calificar según las instrucciones', 'Prueba psicométrica de aptitudes', 'Cuestionario impreso, lápiz, cronómetro', 'Tus puntajes en cada dimensión de aptitud', 'Evaluación')
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones, acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-0004', '2026-04-20', 'Toma de decisión y planificación', 'Momento de elegir la carrera y diseñar el plan de estudio', 'Reúne toda tu información y toma una decisión fundamentada', 'Revisar bitácora, comparar opciones y elegir la carrera que mejor se alinee', 'Matriz de decisión ponderada', 'Bitácora, hoja de cálculo o cuaderno', 'Carrera elegida y razones de tu decisión', 'Decisión')
ON CONFLICT (id_actividad) DO NOTHING;
