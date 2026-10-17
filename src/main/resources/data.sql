-- Carreras reales de la Universidad del Valle de Guatemala
-- costo = total calculado del primer semestre de primer ingreso (cursos + laboratorios + recursos
-- digitales + inscripcion ordinaria). No es costo anual ni de toda la carrera.
-- NULL = dato no confirmado todavia; nunca equivale a cero.
-- Las listas (areas, competencias, oportunidades) se guardan como arreglos JSON en texto.

-- La finalizacion ahora pertenece al registro de cada estudiante, no a la actividad.
ALTER TABLE actividad DROP COLUMN IF EXISTS completada;

-- Retira las carreras de ejemplo anteriores (enlaces example.org) si no tienen historial de cambios.
DELETE FROM carreras
WHERE pdf_oficial_url LIKE 'https://example.org/%'
  AND NOT EXISTS (
      SELECT 1
      FROM registro_actualizacion_carrera AS registro
      WHERE registro.carrera_id = carreras.id
  );

INSERT INTO carreras (
    nombre,
    facultad,
    descripcion,
    duracion,
    costo,
    costo_periodo,
    costo_anio,
    modalidad,
    carga_creditos,
    areas_formacion,
    competencias_desarrolladas,
    oportunidades_laborales,
    salario_estimado,
    empleabilidad,
    contexto_laboral,
    pdf_oficial_url,
    url_carrera_oficial
)

SELECT datos.*

FROM (
    VALUES
    (
        'Ingeniería en Ciencia de la Computación y Tecnologías de la Información',
        'Facultad de Ingeniería',
        'Estudia el diseño de algoritmos y sistemas de software y el análisis de información para resolver problemas de distintos sectores mediante ciencia, ingeniería y matemáticas.',
        5,
        38826.00,
        'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
        2026,
        CAST(NULL AS VARCHAR(80)),
        CAST(NULL AS INTEGER),
        '["Algoritmos y teoría computacional", "Ingeniería de software", "Tecnologías de la información", "Inteligencia artificial y aprendizaje automático", "Ciencia e ingeniería de datos", "Seguridad informática", "Desarrollo de videojuegos", "Experiencia de usuario"]',
        '["Diseñar y desarrollar sistemas de software.", "Analizar información y resolver problemas mediante algoritmos.", "Aplicar herramientas computacionales a problemas científicos y tecnológicos."]',
        '["Desarrollo de software web y móvil", "Ingeniería y análisis de datos", "Seguridad informática", "Administración de tecnología e infraestructura en la nube", "Soluciones para banca, industria, agricultura, educación y salud"]',
        CAST(NULL AS NUMERIC(12,2)),
        CAST(NULL AS NUMERIC(5,2)),
        'Referencia institucional, sin metodología verificada: la universidad afirma que los sueldos de egresados en áreas de computación superan Q360,000 anuales, que hay entre 15 y 20 ofertas de empleo mensuales para estudiantes y que, en promedio, comienzan a trabajar desde tercer año. No equivale a un salario promedio ni a una tasa de empleabilidad.',
        'https://res.cloudinary.com/webuvg/image/upload/v1774622678/WEB/Academico/Carreras/Ingenieria/Computacion%20y%20tecnologias%20de%20la%20informacion/folleto-ingenieria-computacion-2026.pdf',
        'https://www.uvg.edu.gt/carreras/computacion-y-tecnologias-de-la-informacion/'
    ),
    (
        'Ingeniería Civil',
        'Facultad de Ingeniería',
        'Forma profesionales para analizar, diseñar, construir y gestionar infraestructura y obras hidráulicas, incorporando seguridad, sostenibilidad y planificación territorial.',
        5,
        32107.00,
        'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
        2026,
        CAST(NULL AS VARCHAR(80)),
        CAST(NULL AS INTEGER),
        '["Ingeniería estructural", "Ingeniería geotécnica", "Ingeniería hidráulica", "Sistemas de información geográfica y diseño de infraestructura", "Modelado de información de edificaciones (BIM)", "Gestión de proyectos y ordenamiento territorial"]',
        '["Analizar y diseñar soluciones estructurales, geotécnicas e hidráulicas.", "Utilizar herramientas informáticas para representar y simular sistemas.", "Gestionar proyectos de construcción considerando calidad, seguridad e impacto ambiental.", "Comunicar soluciones y colaborar en equipos de trabajo."]',
        '["Diseño estructural y supervisión de obras", "Gestión de proyectos de infraestructura", "Planificación y ordenamiento territorial", "Asesoría geotécnica e hidráulica", "Geoinformática y análisis de datos geoespaciales", "Administración de empresas de construcción"]',
        CAST(NULL AS NUMERIC(12,2)),
        CAST(NULL AS NUMERIC(5,2)),
        NULL,
        'https://res.cloudinary.com/webuvg/image/upload/v1764880393/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-civil.pdf',
        'https://www.uvg.edu.gt/carreras/civil/'
    ),
    (
        'Ingeniería Mecatrónica',
        'Facultad de Ingeniería',
        'Integra mecánica, electrónica e informática para diseñar y optimizar máquinas y sistemas, con orientación a robótica y automatización.',
        5,
        39456.00,
        'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
        2026,
        CAST(NULL AS VARCHAR(80)),
        CAST(NULL AS INTEGER),
        '["Instrumentación y automatización industrial", "Diseño electrónico", "Diseño mecatrónico", "Robótica", "Sistemas de control", "Diseño y manufactura CAD/CAM"]',
        '["Analizar, diseñar e integrar sistemas mecánicos y electrónicos.", "Implementar soluciones de automatización y control.", "Desarrollar y simular sistemas que optimicen procesos.", "Aplicar herramientas de diseño y manufactura a soluciones tecnológicas."]',
        '["Automatización industrial", "Diseño mecánico y electrónico", "Sistemas de control y maquinaria inteligente", "Integración de sistemas", "Mantenimiento industrial", "Investigación, docencia y ejercicio independiente"]',
        CAST(NULL AS NUMERIC(12,2)),
        CAST(NULL AS NUMERIC(5,2)),
        NULL,
        'https://res.cloudinary.com/webuvg/image/upload/v1781555300/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/ing-mecatronica-2026.pdf',
        'https://www.uvg.edu.gt/carreras/mecatronica/'
    ),
    (
        'Licenciatura en Biología',
        'Facultad de Ciencias y Humanidades',
        'Estudia los seres vivos, su evolución y sus relaciones con el ambiente, utilizando investigación científica y herramientas cuantitativas para comprender y conservar la biodiversidad.',
        5,
        33457.00,
        'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
        2026,
        CAST(NULL AS VARCHAR(80)),
        CAST(NULL AS INTEGER),
        '["Ecología y conservación", "Zoología y botánica", "Biología celular y molecular", "Evolución y genética", "Biometría y bioinformática", "SIG y sensores remotos", "Educación y divulgación científica", "Gestión ambiental"]',
        '["Investigar sistemas biológicos y analizar datos.", "Evaluar procesos ecológicos y necesidades de conservación.", "Aplicar herramientas científicas y cuantitativas al estudio de la biodiversidad.", "Comunicar conocimiento científico y proponer iniciativas sostenibles."]',
        '["Investigación biológica", "Gestión ambiental y de recursos naturales", "Conservación de biodiversidad", "Educación y divulgación científica", "Análisis de información biológica y geoespacial", "Innovación y emprendimiento en ciencias biológicas"]',
        CAST(NULL AS NUMERIC(12,2)),
        CAST(NULL AS NUMERIC(5,2)),
        NULL,
        'https://res.cloudinary.com/webuvg/image/upload/v1778602439/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/2024/biologia-2026.pdf',
        'https://www.uvg.edu.gt/carreras/biologia/'
    ),
    (
        'Licenciatura en Química',
        'Facultad de Ciencias y Humanidades',
        'Estudia la estructura, propiedades y transformación de la materia para analizar, sintetizar y desarrollar productos, impulsando la investigación y la innovación con responsabilidad ambiental.',
        4,
        38826.00,
        'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
        2026,
        CAST(NULL AS VARCHAR(80)),
        CAST(NULL AS INTEGER),
        '["Investigación química", "Análisis químico e instrumental", "Síntesis química", "Tecnología para análisis químico", "Control y aseguramiento de calidad", "Gestión de laboratorios"]',
        '["Sintetizar, caracterizar y cuantificar sustancias y productos.", "Evaluar sustancias y desarrollar o modificar métodos analíticos.", "Investigar desde materias primas hasta productos finales.", "Participar en la elaboración de productos y el aseguramiento de su calidad."]',
        '["Investigación básica y aplicada en industria y agroindustria", "Análisis químico y control de calidad", "Desarrollo de productos y métodos analíticos", "Gestión de laboratorios y procesos químicos", "Emprendimiento e innovación química"]',
        CAST(NULL AS NUMERIC(12,2)),
        CAST(NULL AS NUMERIC(5,2)),
        NULL,
        'https://res.cloudinary.com/webuvg/image/upload/v1765423018/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/2024/folleto-lic-quimica.pdf',
        'https://www.uvg.edu.gt/carreras/licenciatura-quimica/'
    ),
    (
        'Licenciatura en Química Farmacéutica',
        'Facultad de Ciencias y Humanidades',
        'Forma profesionales para investigar, formular, producir y controlar medicamentos y cosméticos, y participar en servicios y sistemas relacionados con su uso y regulación.',
        5,
        38826.00,
        'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
        2026,
        CAST(NULL AS VARCHAR(80)),
        CAST(NULL AS INTEGER),
        '["Investigación y desarrollo farmacéutico", "Formulación y producción de medicamentos y cosméticos", "Análisis y aseguramiento de calidad", "Farmacología y farmacovigilancia", "Atención y gestión farmacéutica", "Regulación y distribución de productos farmacéuticos"]',
        '["Participar en el diseño, formulación, producción y evaluación de productos farmacéuticos.", "Aplicar procesos de control y aseguramiento de calidad.", "Contribuir al uso racional de medicamentos y a la farmacovigilancia.", "Investigar y aplicar tecnología en el ámbito farmacéutico y cosmético."]',
        '["Laboratorios de investigación y desarrollo", "Fabricación y control de medicamentos y cosméticos", "Hospitales y farmacias comunitarias", "Regulación y distribución farmacéutica", "Gestión de establecimientos farmacéuticos", "Docencia e investigación científica"]',
        CAST(NULL AS NUMERIC(12,2)),
        CAST(NULL AS NUMERIC(5,2)),
        NULL,
        'https://res.cloudinary.com/webuvg/image/upload/v1778618536/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/2024/folleto-quimica-farmaceutica-2026.pdf',
        'https://www.uvg.edu.gt/carreras/quimica-farmaceutica/'
    )
)

AS datos (
    nombre,
    facultad,
    descripcion,
    duracion,
    costo,
    costo_periodo,
    costo_anio,
    modalidad,
    carga_creditos,
    areas_formacion,
    competencias_desarrolladas,
    oportunidades_laborales,
    salario_estimado,
    empleabilidad,
    contexto_laboral,
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
