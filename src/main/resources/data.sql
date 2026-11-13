-- Carreras reales de la Universidad del Valle de Guatemala
-- costo = total calculado del primer semestre de primer ingreso (cursos + laboratorios + recursos
-- digitales + inscripcion ordinaria). No es costo anual ni de toda la carrera.
-- NULL = dato no confirmado todavia; nunca equivale a cero.
-- Las listas (areas, competencias, oportunidades) se guardan como arreglos JSON en texto.

-- Las carreras anteriores pertenecen a UVG. Conservar ids y claves foraneas.
ALTER TABLE carreras ADD COLUMN IF NOT EXISTS universidad VARCHAR(160) DEFAULT 'UVG';
UPDATE carreras SET universidad = 'UVG' WHERE universidad IS NULL OR BTRIM(universidad) = '';
ALTER TABLE carreras ALTER COLUMN universidad SET NOT NULL;

-- La finalizacion ahora pertenece al registro de cada estudiante, no a la actividad.
ALTER TABLE actividad DROP COLUMN IF EXISTS completada;

-- Retira las carreras de ejemplo anteriores (enlaces example.org) si no tienen historial de cambios.
DELETE FROM carreras
WHERE pdf_oficial_url LIKE 'https://example.org/%'
  AND universidad = 'UVG'
  AND NOT EXISTS (
      SELECT 1
      FROM registro_actualizacion_carrera AS registro
      WHERE registro.carrera_id = carreras.id
  );

-- T-08 E5: 48 programas UVG del archivo adjunto (consulta 2026-10-28).
-- Se conservan sus 17 valores, los identificadores existentes y las relaciones.
-- DEFINICIONES Y ALCANCE
-- duracion: años de la etapa descrita. NUMERIC permite 2.5 y 1.5; no redondear.
-- costo: GTQ del PRIMER SEMESTRE, primer ingreso, con inscripción ordinaria.
-- costo_anio: año del tarifario (2026), NO costo anual.
-- No se extrapolan costos al resto de semestres ni a toda la carrera.
-- Tarifas antes de becas/descuentos y sin gastos personales, pruebas ni extras.
-- listas: arreglos JSON en TEXTO, conforme al esquema facilitado.
-- carga_creditos: total del plan principal únicamente cuando pudo verificarse.
-- NULL: dato no confirmado; no significa cero ni inexistencia.
-- Descripciones, áreas, competencias y salidas: síntesis de perfiles y planes UVG;
-- son ámbitos formativos y de actuación, no vacantes ni promesas de contratación.
-- No se reproduce el pénsum completo: el esquema solicita áreas, no asignaturas.
--
-- FUENTES COMUNES
-- [T] Tarifario UVG 2026, páginas 1-7 (la octava es promocional):
-- https://res.cloudinary.com/webuvg/image/upload/v1758575076/WEB/Admisiones/home/tarifario-carreras-2026.pdf
-- Contrastado con el adjunto tarifario-2025 (1).pdf: su contenido dice 2026.
-- Inscripción general Q2,810; Educación Q1,078; Primaria virtual Q690.
-- En Educación no se agregan laboratorios: el tarifario señala que no se utilizan.
-- Algunas fichas de profesorado publican Q1,080: se registra la discrepancia y
-- se mantiene [T] como base homogénea de los costos, sin afirmar cotización vigente.
-- [M] Modalidad general de pregrado, aplicada a programas no educativos:
-- https://www.uvg.edu.gt/admisiones/preguntas-frecuentes/
-- Es una declaración institucional general; para Educación prevalece el folleto
-- específico. No se asigna modalidad a Mecánica y Aeronáutica sin confirmación.
-- [E] Estructura de continuidad de profesorados y licenciaturas de Educación:
-- https://www.uvg.edu.gt/facultades/educacion/
-- Las cinco licenciaturas de Educación describen la etapa de tres semestres
-- posterior a profesorado/técnico. La ruta UVG 2.5 + 1.5 suma cuatro años.
-- [C] Reporte UVG del CEPS, complemento de duración, no fuente de tarifas actuales:
-- https://www.ceps.edu.gt/publico/download/Universidad%20Del%20Valle%20de%20Guatemala?version_id=34160
-- El contenido pertinente se pudo consultar en resultados indexados; la descarga
-- directa falló. No se incorporan sus cargas de créditos de planes anteriores.
--
-- TRAZABILIDAD POR REGISTRO
-- url_carrera_oficial: fuente del perfil; pdf_oficial_url: plan/folleto entregado.
-- Salvo excepciones documentadas, duración derivada de años/ciclos del plan.
-- En cada bloque se documentan cálculo del costo y fuentes complementarias.
-- Hay enlaces que redirigen a una página canónica; se conserva el destino verificado.
-- PDFs no recuperados: Ciencias de Alimentos y Ciencias de Alimentos Industrial.
-- Lectura parcial/ambigua: Composición Musical, Matemática Aplicada y tablas de
-- créditos de Biología. Se emplean fuentes alternativas o NULL, según el caso.
-- Mecánica y Aeronáutica: el PDF repetido de Mecánica no valida su plan propio.
--
-- DATOS NO RESUELTOS
-- No se encontró evidencia suficiente de salarios representativos ni tasas de
-- empleo por cada carrera/cohorte: salario_estimado y empleabilidad quedan NULL.
-- No se convierten testimonios, ofertas aisladas o porcentajes de posgrado en tasas.
-- Los créditos NULL no significan que la carrera carezca de créditos: faltó un
-- total inequívoco del plan correspondiente en la documentación consultada.
-- Duración NULL: Alimentos Industrial, Matemática Aplicada, Mecánica y Aeronáutica.
-- Tarifa y folleto propio NULL: Mecánica y Aeronáutica.
-- Química Industrial: folleto de seis años frente a registro CEPS de cinco.
-- Baccalaureatus: cuatro años del plan cerrado; no es licenciatura profesional.
--
BEGIN;

-- Compatibilidad con las carreras existentes: conservar decimales y datos desconocidos.
ALTER TABLE carreras ALTER COLUMN duracion TYPE NUMERIC(4,1) USING duracion::NUMERIC(4,1);
ALTER TABLE carreras ALTER COLUMN duracion DROP NOT NULL;
ALTER TABLE carreras ALTER COLUMN costo DROP NOT NULL;

CREATE TEMP TABLE uvg_datos_20261028 (
    nombre TEXT,
    facultad TEXT,
    descripcion TEXT,
    duracion NUMERIC(4,1),
    costo NUMERIC(12,2),
    costo_periodo TEXT,
    costo_anio INTEGER,
    modalidad TEXT,
    carga_creditos INTEGER,
    areas_formacion TEXT,
    competencias_desarrolladas TEXT,
    oportunidades_laborales TEXT,
    salario_estimado NUMERIC(12,2),
    empleabilidad NUMERIC(5,2),
    contexto_laboral TEXT,
    pdf_oficial_url TEXT,
    url_carrera_oficial TEXT
) ON COMMIT DROP;

-- 01/48 | Ingeniería Biomédica
-- Costo GTQ: cursos 32214 + laboratorios 4194 + recursos digitales 780 + inscripción 2810 = 39998.
-- Duración y modalidad: https://ceps.edu.gt/ceps/prog-view-one?programa_id=1850
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Biomédica',
    'Facultad de Ingeniería',
    'Integra ingeniería y ciencias de la salud para desarrollar tecnología de diagnóstico, monitoreo, tratamiento y rehabilitación.',
    5,
    39998,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Fisiología cuantitativa", "Instrumentación médica", "Biomecánica y biomateriales", "Robótica médica", "Procesamiento de señales e imágenes", "Ingeniería de tejidos"]',
    '["Diseñar soluciones tecnológicas para problemas de salud.", "Modelar procesos fisiológicos.", "Adquirir y analizar información de dispositivos médicos."]',
    '["Desarrollo de dispositivos médicos", "Robótica de rehabilitación y cirugía", "Investigación biomédica"]',
    NULL,
    NULL,
    'UVG comunica un 30% de egresados con posicionamiento internacional en trabajo o maestrías. No identifica cohorte ni metodología; la cifra combina empleo y estudios, por lo que no es una tasa de empleabilidad.',
    'https://res.cloudinary.com/webuvg/image/upload/v1764782900/WEB/Academico/Carreras/Ingenieria/Biom%C3%A9dica/folleto-ing-biomedica.pdf',
    'https://www.uvg.edu.gt/carreras/biomedica/'
);

-- 02/48 | Ingeniería en Biotecnología Industrial
-- Costo GTQ: cursos 32214 + laboratorios 3022 + recursos digitales 780 + inscripción 2810 = 38826.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería en Biotecnología Industrial',
    'Facultad de Ingeniería',
    'Aplica sistemas biológicos al diseño y operación de procesos industriales para obtener productos y aprovechar recursos de manera sostenible.',
    5,
    38826,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Microbiología industrial", "Ingeniería genética", "Operaciones unitarias", "Biorreactores", "Bioquímica", "Evaluación de procesos"]',
    '["Diseñar y optimizar bioprocesos.", "Integrar conocimientos biológicos y de ingeniería.", "Evaluar alternativas tecnológicas y productivas."]',
    '["Producción biotecnológica", "Industria alimentaria y farmacéutica", "Procesos ambientales", "Investigación y desarrollo"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1764798856/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-biotecnologia-industrial.pdf',
    'https://www.uvg.edu.gt/carreras/biotecnologia-industrial/'
);

-- 03/48 | Ingeniería en Ciencia de la Administración
-- Costo GTQ: cursos 32214 + laboratorios 1246 + recursos digitales 780 + inscripción 2810 = 37050.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería en Ciencia de la Administración',
    'Bridge Business School',
    'Combina herramientas de ingeniería, análisis de datos y administración para gestionar organizaciones y fundamentar decisiones de negocio.',
    5,
    37050,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Finanzas", "Analítica de negocios", "Operaciones y logística", "Estrategia", "Mercadeo", "Gestión de personas"]',
    '["Analizar información para decisiones empresariales.", "Evaluar proyectos y procesos.", "Gestionar recursos y liderar equipos."]',
    '["Gestión empresarial", "Análisis de negocios", "Operaciones y logística", "Finanzas", "Emprendimiento"]',
    NULL,
    NULL,
    'El tarifario y el folleto ubican el programa en Bridge Business School. El catálogo también lo incluye en Ingeniería; se utiliza la adscripción de las fuentes específicas.',
    'https://res.cloudinary.com/webuvg/image/upload/v1764864037/WEB/Academico/Carreras/Ingenieria/Administracion/folleto-ing-ciencia-administracion.pdf',
    'https://www.uvg.edu.gt/carreras/administracion/'
);

-- 04/48 | Ingeniería en Ciencias de Alimentos
-- Costo GTQ: cursos 32214 + laboratorios 3810 + recursos digitales 780 + inscripción 2810 = 39614.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería en Ciencias de Alimentos',
    'Facultad de Ingeniería',
    'Aplica ciencia e ingeniería a la elaboración de alimentos seguros y al desarrollo de productos y procesos alimentarios.',
    5,
    39614,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Ciencia y tecnología de alimentos", "Procesamiento", "Calidad e inocuidad", "Desarrollo de productos", "Legislación alimentaria"]',
    '["Diseñar productos alimentarios.", "Controlar procesos de producción.", "Evaluar calidad e inocuidad."]',
    '["Desarrollo de alimentos", "Control de calidad", "Producción alimentaria", "Gestión de negocios de alimentos"]',
    NULL,
    NULL,
    'Perfil sintetizado de la página departamental. El PDF suministrado no pudo recuperarse; se conserva su enlace sin atribuirle contenido. Duración de cinco años corroborada en el reporte de UVG del CEPS.',
    'https://res.cloudinary.com/webuvg/image/upload/v1764871350/WEB/Academico/Carreras/Ingenieria/Ciencias%20de%20Alimentos/plan-estudios/folleto-ing-ciencia-alimentos.pdf',
    'https://www.uvg.edu.gt/carreras/ciencia-de-alimentos/'
);

-- 05/48 | Ingeniería en Ciencias de Alimentos Industrial
-- Costo GTQ: cursos 32214 + laboratorios 3810 + recursos digitales 780 + inscripción 2810 = 39614.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería en Ciencias de Alimentos Industrial',
    'Facultad de Ingeniería',
    'Programa de ingeniería alimentaria identificado de forma independiente en el catálogo y en el tarifario UVG. Comparte página departamental con Ciencias de Alimentos.',
    NULL,
    39614,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Tecnología de alimentos", "Producción alimentaria", "Calidad e inocuidad"]',
    '["Analizar procesos alimentarios.", "Participar en el desarrollo y control de productos."]',
    '["Industria alimentaria", "Producción y calidad"]',
    NULL,
    NULL,
    'Áreas y oportunidades proceden del perfil común del departamento, no de un perfil exclusivo confirmado. El folleto específico no pudo recuperarse. Duración, créditos y diferenciación curricular quedan pendientes; no se copia la duración del otro programa.',
    'https://res.cloudinary.com/webuvg/image/upload/v1764874210/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-ciencias-alimentos-industrial.pdf',
    'https://www.uvg.edu.gt/carreras/ciencia-de-alimentos/'
);

-- 06/48 | Ingeniería Civil
-- Costo GTQ: cursos 26845 + laboratorios 1672 + recursos digitales 780 + inscripción 2810 = 32107.
-- Duración corroborada en el reporte UVG del CEPS (fuente complementaria global).
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Civil',
    'Facultad de Ingeniería',
    'Forma profesionales para diseñar, construir y gestionar infraestructura, considerando seguridad, sostenibilidad y características del territorio.',
    5,
    32107,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Estructuras", "Geotecnia", "Hidráulica", "Geoinformática", "BIM", "Gestión de construcción"]',
    '["Diseñar soluciones estructurales e hidráulicas.", "Evaluar condiciones geotécnicas.", "Gestionar proyectos de infraestructura."]',
    '["Diseño y supervisión de obras", "Consultoría de infraestructura", "Gestión de construcción", "Planificación territorial"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1764880393/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-civil.pdf',
    'https://www.uvg.edu.gt/carreras/civil/'
);

-- 07/48 | Ingeniería Civil Arquitectónica
-- Costo GTQ: cursos 26845 + laboratorios 1672 + recursos digitales 780 + inscripción 2810 = 32107.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Civil Arquitectónica',
    'Facultad de Ingeniería',
    'Integra ingeniería civil y diseño arquitectónico para desarrollar edificaciones y coordinar sus sistemas constructivos e instalaciones.',
    6,
    32107,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Diseño arquitectónico", "Estructuras", "Instalaciones hidrosanitarias y eléctricas", "BIM", "Construcción sostenible", "Gestión de proyectos"]',
    '["Integrar diseño y soluciones constructivas.", "Coordinar instalaciones de edificios.", "Evaluar seguridad, eficiencia y costos."]',
    '["Diseño de edificaciones", "Coordinación de obras", "Modelado BIM", "Gestión de construcción sostenible"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1764883268/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-civil-arquitectonica.pdf',
    'https://www.uvg.edu.gt/carreras/civil-arquitectonica/'
);

-- 08/48 | Ingeniería en Ciencia de la Computación y Tecnologías de la Información
-- Costo GTQ: cursos 32214 + laboratorios 3022 + recursos digitales 780 + inscripción 2810 = 38826.
-- Créditos calculados de C por ciclo, páginas 2-6: 31+28+31+27+31+25+30+30+31+30 = 294.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería en Ciencia de la Computación y Tecnologías de la Información',
    'Facultad de Ingeniería',
    'Estudia algoritmos, software y sistemas de información para resolver problemas mediante ciencias de la computación e ingeniería.',
    5,
    38826,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    294,
    '["Algoritmos", "Ingeniería de software", "Inteligencia artificial", "Ciencia de datos", "Ciberseguridad", "Sistemas y redes"]',
    '["Diseñar y desarrollar software.", "Analizar datos mediante herramientas computacionales.", "Resolver problemas con algoritmos y modelos."]',
    '["Desarrollo de software", "Ingeniería de datos", "Ciberseguridad", "Infraestructura tecnológica", "Investigación computacional"]',
    NULL,
    NULL,
    'UVG publica referencias salariales superiores a Q360,000 anuales y entre 15 y 20 ofertas mensuales, sin metodología suficiente para convertirlas en promedio salarial o tasa de empleo. Créditos: suma calculada de la columna C de los diez ciclos del plan principal; no incluye especialidades adicionales.',
    'https://res.cloudinary.com/webuvg/image/upload/v1774622678/WEB/Academico/Carreras/Ingenieria/Computacion%20y%20tecnologias%20de%20la%20informacion/folleto-ingenieria-computacion-2026.pdf',
    'https://www.uvg.edu.gt/carreras/computacion-y-tecnologias-de-la-informacion/'
);

-- 09/48 | Licenciatura en Sistemas de Información Computacional
-- Costo GTQ: cursos 32214 + laboratorios 1650 + recursos digitales 780 + inscripción 2810 = 37454.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Sistemas de Información Computacional',
    'Facultad de Ingeniería',
    'Integra tecnología y gestión organizacional para implementar sistemas de información que apoyen procesos y decisiones empresariales.',
    4,
    37454,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Bases de datos", "Sistemas de información", "Redes y seguridad", "Inteligencia de negocios", "Gestión de proyectos"]',
    '["Analizar necesidades de información.", "Implementar soluciones tecnológicas.", "Gestionar proyectos que conectan tecnología y negocios."]',
    '["Análisis de sistemas", "Administración de bases de datos", "Inteligencia de negocios", "Gestión de proyectos tecnológicos"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1764899403/WEB/Academico/Carreras/Ingenieria/Sistemas-informacion-computacional/folleto-lic-sistemas-informacion-computacional.pdf',
    'https://www.uvg.edu.gt/carreras/sistemas-de-informacion-computacional/'
);

-- 10/48 | Ingeniería Electrónica
-- Costo GTQ: cursos 32214 + laboratorios 3652 + recursos digitales 780 + inscripción 2810 = 39456.
-- Duración y modalidad: https://ceps.edu.gt/ceps/prog-view-one?programa_id=948
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Electrónica',
    'Facultad de Ingeniería',
    'Desarrolla sistemas electrónicos para procesar información, comunicar dispositivos y automatizar procesos.',
    5,
    39456,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Circuitos analógicos y digitales", "Microelectrónica", "Telecomunicaciones", "Control", "Sistemas embebidos"]',
    '["Diseñar y probar circuitos.", "Desarrollar hardware y firmware.", "Integrar instrumentos y sistemas de control."]',
    '["Diseño electrónico", "Telecomunicaciones", "Instrumentación industrial", "Sistemas embebidos"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1764907079/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-electronica.pdf',
    'https://www.uvg.edu.gt/carreras/electronica/'
);

-- 11/48 | Ingeniería Industrial
-- Costo GTQ: cursos 32214 + laboratorios 2723 + recursos digitales 780 + inscripción 2810 = 38527.
-- Créditos calculados de C por ciclo, páginas 2-6: 30+26+28+26+26+23+25+27+24+38 = 273.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Industrial',
    'Facultad de Ingeniería',
    'Diseña y mejora sistemas de producción y servicios que integran personas, materiales, información y equipos.',
    5,
    38527,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    273,
    '["Investigación de operaciones", "Logística", "Calidad", "Finanzas", "Análisis de datos", "Gestión de proyectos"]',
    '["Optimizar procesos y recursos.", "Evaluar proyectos económicamente.", "Analizar información para mejorar operaciones."]',
    '["Operaciones", "Cadena de suministro", "Calidad y mejora de procesos", "Consultoría", "Gestión de proyectos"]',
    NULL,
    NULL,
    'Se registra el programa regular solicitado. El tarifario presenta por separado Ingeniería Industrial vespertina, que no se incluye como carrera adicional. Créditos calculados del plan principal; no se vuelven a sumar las opciones de especialidad y secuencia matemática detalladas al final del folleto.',
    'https://res.cloudinary.com/webuvg/image/upload/v1743024183/WEB/Academico/Carreras/Ingenieria/Industrial/folleto-ingenieria-industrial-update.pdf',
    'https://www.uvg.edu.gt/carreras/industrial/'
);

-- 12/48 | Ingeniería Mecánica
-- Costo GTQ: cursos 32214 + laboratorios 1987 + recursos digitales 780 + inscripción 2810 = 37791.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Mecánica',
    'Facultad de Ingeniería',
    'Aplica mecánica, energía y materiales al diseño, fabricación y mantenimiento de máquinas y sistemas.',
    5,
    37791,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Diseño mecánico", "Materiales", "Termodinámica", "Mecánica de fluidos", "Manufactura CAD/CAM", "Mantenimiento"]',
    '["Diseñar componentes y mecanismos.", "Analizar sistemas térmicos y de fluidos.", "Seleccionar materiales y procesos de fabricación."]',
    '["Diseño y manufactura", "Mantenimiento industrial", "Sistemas energéticos", "Investigación y desarrollo mecánico"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1764947428/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-mecanica.pdf',
    'https://www.uvg.edu.gt/carreras/mecanica/'
);

-- 13/48 | Ingeniería Mecánica y Aeronáutica
-- Programa distinto: no se fusiona con Ingeniería Mecánica. Folleto propio y tarifa no confirmados.
-- Fuente de existencia: https://www.uvg.edu.gt/facultades/ingenieria/
-- Fuente del enfoque: https://noticias.uvg.edu.gt/congreso-espacial-centroamericano-2026-uvg/
-- Enlace PDF recibido pero no asignado a este programa: https://res.cloudinary.com/webuvg/image/upload/v1764947428/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-mecanica.pdf
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Mecánica y Aeronáutica',
    'Facultad de Ingeniería',
    'Programa listado por UVG que busca desarrollar capacidades para el diseño, construcción y mantenimiento de aeronaves.',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '["Diseño de aeronaves", "Construcción de aeronaves", "Mantenimiento de aeronaves"]',
    '["Desarrollar soluciones para el diseño, construcción y mantenimiento de aeronaves."]',
    NULL,
    NULL,
    NULL,
    'Existencia confirmada en el catálogo de Ingeniería y orientación confirmada en la nota oficial del Congreso Espacial Centroamericano 2026. El PDF facilitado corresponde a Ingeniería Mecánica, no a un plan específico de Mecánica y Aeronáutica. No se heredan tarifa, duración, modalidad ni créditos. Los demás campos requieren documentación específica.',
    NULL,
    'https://www.uvg.edu.gt/carreras/mecanica/'
);

-- 14/48 | Ingeniería Mecánica Industrial
-- Costo GTQ: cursos 32214 + laboratorios 1987 + recursos digitales 780 + inscripción 2810 = 37791.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Mecánica Industrial',
    'Facultad de Ingeniería',
    'Combina el diseño y operación de sistemas mecánicos con herramientas de gestión de procesos y recursos industriales.',
    6,
    37791,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Diseño mecánico", "Manufactura", "Mantenimiento", "Operaciones industriales", "Evaluación económica"]',
    '["Integrar soluciones mecánicas en sistemas productivos.", "Mejorar procesos de fabricación.", "Gestionar recursos y mantenimiento."]',
    '["Producción industrial", "Gestión de mantenimiento", "Manufactura", "Administración de operaciones"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1764949231/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-mecanica-industrial.pdf',
    'https://www.uvg.edu.gt/carreras/mecanica/'
);

-- 15/48 | Ingeniería Mecatrónica
-- Costo GTQ: cursos 32214 + laboratorios 3652 + recursos digitales 780 + inscripción 2810 = 39456.
-- Duración corroborada: https://noticias.uvg.edu.gt/estudiantes-ingenieria-mecatronica-uvg-proyectos-experiencia/
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Mecatrónica',
    'Facultad de Ingeniería',
    'Integra mecánica, electrónica e informática para diseñar máquinas y sistemas de automatización y robótica.',
    5,
    39456,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Robótica", "Control", "Diseño mecánico", "Electrónica", "Instrumentación", "Manufactura CAD/CAM"]',
    '["Integrar sistemas mecánicos y electrónicos.", "Diseñar soluciones de automatización.", "Simular y optimizar sistemas mecatrónicos."]',
    '["Automatización industrial", "Robótica", "Diseño de maquinaria", "Mantenimiento", "Investigación tecnológica"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1781555300/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/ing-mecatronica-2026.pdf',
    'https://www.uvg.edu.gt/carreras/mecatronica/'
);

-- 16/48 | Ingeniería Química
-- Costo GTQ: cursos 32214 + laboratorios 2076 + recursos digitales 780 + inscripción 2810 = 37880.
-- Duración: https://ceps.edu.gt/ceps/prog-view-one?programa_id=952
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Química',
    'Facultad de Ingeniería',
    'Aplica ciencias e ingeniería al diseño y operación de procesos que transforman materias primas y energía en productos útiles.',
    5,
    37880,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Operaciones unitarias", "Transferencia de calor y masa", "Reactores", "Diseño de plantas", "Control de procesos", "Producción limpia"]',
    '["Modelar y diseñar procesos químicos.", "Evaluar seguridad y desempeño de instalaciones.", "Proponer mejoras productivas y ambientales."]',
    '["Diseño y operación de plantas", "Control de procesos", "Investigación industrial", "Producción y calidad"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1765206332/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-quimica.pdf',
    'https://www.uvg.edu.gt/carreras/quimica/'
);

-- 17/48 | Ingeniería Química Industrial
-- Costo GTQ: cursos 32214 + laboratorios 2076 + recursos digitales 780 + inscripción 2810 = 37880.
-- Fuente discrepante: https://ceps.edu.gt/ceps/prog-view-one?programa_id=1696
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Ingeniería Química Industrial',
    'Facultad de Ingeniería',
    'Integra ingeniería de procesos químicos y gestión industrial para diseñar y mejorar sistemas de transformación y producción.',
    6,
    37880,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Procesos químicos", "Operaciones unitarias", "Gestión de producción", "Calidad", "Evaluación económica"]',
    '["Diseñar y evaluar procesos de transformación.", "Gestionar recursos industriales.", "Analizar viabilidad técnica y económica."]',
    '["Producción industrial", "Gestión de plantas", "Control de calidad", "Mejora de procesos"]',
    NULL,
    NULL,
    'El folleto suministrado presenta un sexto año y se adopta duración curricular de seis años. El registro CEPS consultado indica cinco; existe una discrepancia entre fuentes que debe confirmarse con UVG para la cohorte de ingreso.',
    'https://res.cloudinary.com/webuvg/image/upload/v1765208205/WEB/Academico/Plan%20de%20estudios/Ingenier%C3%ADa/folleto-ing-quimica-industrial.pdf',
    'https://www.uvg.edu.gt/carreras/quimica/'
);

-- 18/48 | Profesorado de Enseñanza Media Especializado en Educación Musical
-- Costo GTQ: cursos 5255 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 6863.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Profesorado de Enseñanza Media Especializado en Educación Musical',
    'Facultad de Educación',
    'Forma docentes para planificar y facilitar aprendizajes musicales mediante práctica artística y pedagogía.',
    2.5,
    6863,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Lenguaje musical", "Instrumentos y ensambles", "Didáctica musical", "Tecnología educativa"]',
    '["Planificar experiencias musicales.", "Evaluar aprendizajes artísticos.", "Integrar recursos musicales en la enseñanza."]',
    '["Docencia musical", "Talleres y programas artísticos"]',
    NULL,
    NULL,
    'Programa de cinco semestres (2.5 años). Existe diferencia entre inscripción del tarifario 2026 (Q1,078) y folletos consultados (Q1,080); costo calculado con el tarifario, no con la cifra del folleto.',
    'https://res.cloudinary.com/webuvg/image/upload/v1780070295/WEB/Academico/Plan%20de%20estudios/Educaci%C3%B3n/2026/4-prof-ensenanza-media-en-edu-musical.pdf',
    'https://www.uvg.edu.gt/carreras/educacion-educacion-musical/'
);

-- 19/48 | Profesorado de Enseñanza Media Especializado en English Language Teaching
-- Costo GTQ: cursos 5255 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 6863.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Profesorado de Enseñanza Media Especializado en English Language Teaching',
    'Facultad de Educación',
    'Prepara docentes de inglés para diseñar, desarrollar y evaluar experiencias de aprendizaje de una lengua adicional.',
    2.5,
    6863,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Adquisición de lenguas", "Gramática y escritura", "Comunicación oral", "Didáctica del inglés", "Evaluación"]',
    '["Planificar clases de inglés.", "Desarrollar habilidades lingüísticas.", "Evaluar desempeño y adaptar materiales."]',
    '["Docencia de inglés", "Programas de idiomas", "Diseño de recursos didácticos"]',
    NULL,
    NULL,
    'Programa de cinco semestres (2.5 años). Existe diferencia entre inscripción del tarifario 2026 (Q1,078) y folletos consultados (Q1,080); costo calculado con el tarifario, no con la cifra del folleto. El folleto señala prueba de ubicación de Q295; no se incluye en el costo académico comparable del primer semestre.',
    'https://res.cloudinary.com/webuvg/image/upload/v1780068300/WEB/Academico/Plan%20de%20estudios/Educaci%C3%B3n/2026/6-prof-ensenanza-media-en-english-ELT.pdf',
    'https://www.uvg.edu.gt/carreras/educacion-english-language-teaching/'
);

-- 20/48 | Profesorado Especializado en Educación Inclusiva
-- Costo GTQ: cursos 5255 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 6863.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Profesorado Especializado en Educación Inclusiva',
    'Facultad de Educación',
    'Forma docentes para atender diversidad de necesidades y promover participación y aprendizaje en entornos educativos inclusivos.',
    2.5,
    6863,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Desarrollo y neurodiversidad", "Discapacidad", "Diseño universal para el aprendizaje", "Evaluación educativa"]',
    '["Adaptar experiencias educativas.", "Identificar barreras al aprendizaje.", "Diseñar apoyos para la inclusión."]',
    '["Docencia inclusiva", "Apoyo educativo", "Programas de inclusión"]',
    NULL,
    NULL,
    'Programa de cinco semestres (2.5 años). Existe diferencia entre inscripción del tarifario 2026 (Q1,078) y folletos consultados (Q1,080); costo calculado con el tarifario, no con la cifra del folleto.',
    'https://res.cloudinary.com/webuvg/image/upload/v1780072004/WEB/Academico/Plan%20de%20estudios/Educaci%C3%B3n/2026/14-prof-profesorado-educacion-inclusiva.pdf',
    'https://www.uvg.edu.gt/carreras/educacion-inclusiva/'
);

-- 21/48 | Profesorado Especializado en Educación Primaria
-- Costo GTQ: cursos 5255 + laboratorios 0 + recursos digitales 530 + inscripción 690 = 6475.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Profesorado Especializado en Educación Primaria',
    'Facultad de Educación',
    'Prepara docentes de primaria mediante un programa virtual con formación pedagógica, disciplinar y práctica educativa.',
    2.5,
    6475,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Virtual',
    NULL,
    '["Lectoescritura", "Matemática escolar", "Ciencias naturales y sociales", "Didáctica", "Evaluación"]',
    '["Planificar enseñanza para primaria.", "Facilitar aprendizaje activo.", "Evaluar avances y adaptar recursos."]',
    '["Docencia de primaria", "Desarrollo de materiales", "Proyectos educativos"]',
    NULL,
    NULL,
    'Programa de cinco semestres (2.5 años).',
    'https://res.cloudinary.com/webuvg/image/upload/v1780071430/WEB/Academico/Plan%20de%20estudios/Educaci%C3%B3n/2026/5-prof-primaria-virtual.pdf',
    'https://www.uvg.edu.gt/carreras/educacion-primaria-virtual/'
);

-- 22/48 | Profesorado Especializado en Problemas del Aprendizaje
-- Costo GTQ: cursos 5255 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 6863.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Profesorado Especializado en Problemas del Aprendizaje',
    'Facultad de Educación',
    'Forma profesionales para identificar dificultades de aprendizaje y desarrollar apoyos educativos pertinentes.',
    2.5,
    6863,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Lectoescritura y cálculo", "Desarrollo humano", "Evaluación del aprendizaje", "Intervención educativa"]',
    '["Identificar dificultades del aprendizaje.", "Diseñar estrategias de apoyo.", "Acompañar procesos educativos."]',
    '["Apoyo al aprendizaje", "Docencia especializada", "Programas de intervención educativa"]',
    NULL,
    NULL,
    'Programa de cinco semestres (2.5 años). Existe diferencia entre inscripción del tarifario 2026 (Q1,078) y folletos consultados (Q1,080); costo calculado con el tarifario, no con la cifra del folleto.',
    'https://res.cloudinary.com/webuvg/image/upload/v1780071010/WEB/Academico/Plan%20de%20estudios/Educaci%C3%B3n/2026/13-prof-problemas-del-aprendizaje.pdf',
    'https://www.uvg.edu.gt/carreras/educacion-problemas-de-aprendizaje/'
);

-- 23/48 | Profesorado de Enseñanza Media Especializado en Matemática y Ciencias Físicas
-- Costo GTQ: cursos 5255 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 6863.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Profesorado de Enseñanza Media Especializado en Matemática y Ciencias Físicas',
    'Facultad de Educación',
    'Prepara docentes de matemática y física que combinan conocimiento disciplinar, experimentación y estrategias pedagógicas.',
    2.5,
    6863,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Álgebra y geometría", "Cálculo", "Física", "Experimentación", "Didáctica"]',
    '["Explicar conceptos matemáticos y físicos.", "Diseñar actividades y experimentos.", "Evaluar razonamiento y resolución de problemas."]',
    '["Docencia de matemática y física", "Materiales educativos", "Programas de educación científica"]',
    NULL,
    NULL,
    'Programa de cinco semestres (2.5 años). Existe diferencia entre inscripción del tarifario 2026 (Q1,078) y folletos consultados (Q1,080); costo calculado con el tarifario, no con la cifra del folleto.',
    'https://res.cloudinary.com/webuvg/image/upload/v1780068637/WEB/Academico/Plan%20de%20estudios/Educaci%C3%B3n/2026/12-prof-ensenanza-media-matematica-y-ciencias-fisicas.pdf',
    'https://www.uvg.edu.gt/carreras/educacion-matematica-y-ciencias-fisicas/'
);

-- 24/48 | Profesorado de Enseñanza Media Especializado en Ciencias Químicas y Biológicas
-- Costo GTQ: cursos 5255 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 6863.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Profesorado de Enseñanza Media Especializado en Ciencias Químicas y Biológicas',
    'Facultad de Educación',
    'Forma docentes para enseñar química y biología mediante actividades experimentales y aprendizaje activo.',
    2.5,
    6863,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Química", "Biología", "Bioquímica", "Laboratorio", "Didáctica de las ciencias"]',
    '["Diseñar experiencias de educación científica.", "Relacionar conceptos con fenómenos observables.", "Evaluar aprendizajes en ciencias."]',
    '["Docencia de química y biología", "Laboratorios educativos", "Divulgación y recursos científicos"]',
    NULL,
    NULL,
    'Programa de cinco semestres (2.5 años). Existe diferencia entre inscripción del tarifario 2026 (Q1,078) y folletos consultados (Q1,080); costo calculado con el tarifario, no con la cifra del folleto.',
    'https://res.cloudinary.com/webuvg/image/upload/v1780068053/WEB/Academico/Plan%20de%20estudios/Educaci%C3%B3n/2026/1-prof-ensenanza-media-ciencias-quimicas-y-biologicas.pdf',
    'https://www.uvg.edu.gt/carreras/educacion-ciencias-quimicas-y-bilogicas/'
);

-- 25/48 | Profesorado de Enseñanza Media Especializado en Ciencias Sociales
-- Costo GTQ: cursos 5255 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 6863.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Profesorado de Enseñanza Media Especializado en Ciencias Sociales',
    'Facultad de Educación',
    'Prepara docentes para comprender y enseñar procesos sociales, históricos y geográficos con perspectiva crítica.',
    2.5,
    6863,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Historia", "Geografía", "Antropología", "Ciudadanía", "Didáctica de las ciencias sociales"]',
    '["Analizar procesos históricos y sociales.", "Diseñar experiencias de aprendizaje contextualizadas.", "Evaluar interpretación y pensamiento crítico."]',
    '["Docencia de ciencias sociales", "Recursos educativos", "Proyectos de formación ciudadana"]',
    NULL,
    NULL,
    'Programa de cinco semestres (2.5 años). Existe diferencia entre inscripción del tarifario 2026 (Q1,078) y folletos consultados (Q1,080); costo calculado con el tarifario, no con la cifra del folleto.',
    'https://res.cloudinary.com/webuvg/image/upload/v1780069821/WEB/Academico/Plan%20de%20estudios/Educaci%C3%B3n/2026/2-prof-ensenanza-media-en-ciencias-sociales.pdf',
    'https://www.uvg.edu.gt/carreras/educacion-ciencias-sociales/'
);

-- 26/48 | Profesorado de Enseñanza Media Especializado en Comunicación y Lenguaje
-- Costo GTQ: cursos 5255 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 6863.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Profesorado de Enseñanza Media Especializado en Comunicación y Lenguaje',
    'Facultad de Educación',
    'Forma docentes para desarrollar capacidades de lectura, escritura y comunicación mediante enfoques pedagógicos activos.',
    2.5,
    6863,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Lingüística", "Literatura", "Lectura y escritura", "Comunicación", "Didáctica"]',
    '["Planificar enseñanza de lengua y literatura.", "Promover comprensión lectora.", "Evaluar producción oral y escrita."]',
    '["Docencia de comunicación y lenguaje", "Materiales didácticos", "Programas de lectura"]',
    NULL,
    NULL,
    'Programa de cinco semestres (2.5 años). Existe diferencia entre inscripción del tarifario 2026 (Q1,078) y folletos consultados (Q1,080); costo calculado con el tarifario, no con la cifra del folleto.',
    'https://res.cloudinary.com/webuvg/image/upload/v1780072272/WEB/Academico/Plan%20de%20estudios/Educaci%C3%B3n/2026/3-prof-ensenanza-media-comunicacion-y-lenguaje.pdf',
    'https://www.uvg.edu.gt/carreras/educacion-comunicacion-y-lenguaje/'
);

-- 27/48 | Licenciatura en Educación
-- Costo GTQ: cursos 7880 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 9488.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Educación',
    'Facultad de Educación',
    'Profundiza en investigación, currículo, gestión e innovación para mejorar procesos e instituciones educativas. Requiere formación previa de profesorado o técnico y admisión según el folleto.',
    1.5,
    9488,
    'Primer semestre de licenciatura posterior a profesorado o técnico, incluida inscripción',
    2026,
    'Virtual sincrónica',
    NULL,
    '["Investigación educativa", "Estadística", "Currículo", "Liderazgo", "Gestión de proyectos"]',
    '["Investigar problemas educativos.", "Diseñar y evaluar propuestas curriculares.", "Gestionar proyectos e instituciones."]',
    '["Gestión educativa", "Investigación", "Diseño curricular", "Formación docente"]',
    NULL,
    NULL,
    'Duración registrada: tres ciclos semestrales de licenciatura posteriores al título previo (1.5 años); no es una licenciatura de 1.5 años desde diversificado. La ruta con profesorado UVG de 2.5 años suma cuatro años. El costo corresponde al primer semestre de esta etapa.',
    'https://www.uvg.edu.gt/wp-content/uploads/licenciatura-educacion-2026.pdf',
    'https://www.uvg.edu.gt/carreras/licenciatura-educacion/'
);

-- 28/48 | Licenciatura en Educación Musical
-- Costo GTQ: cursos 7880 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 9488.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Educación Musical',
    'Facultad de Educación',
    'Desarrolla capacidades pedagógicas y artísticas para coordinar proyectos e instituciones de educación musical. Requiere formación previa de profesorado o técnico y admisión según el folleto.',
    1.5,
    9488,
    'Primer semestre de licenciatura posterior a profesorado o técnico, incluida inscripción',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Currículo", "Investigación educativa", "Gestión de proyectos musicales", "Tecnología", "Pedagogía musical"]',
    '["Diseñar programas musicales.", "Liderar proyectos educativos y culturales.", "Investigar prácticas de enseñanza."]',
    '["Coordinación de educación musical", "Gestión artística educativa", "Docencia"]',
    NULL,
    NULL,
    'Duración registrada: tres ciclos semestrales de licenciatura posteriores al título previo (1.5 años); no es una licenciatura de 1.5 años desde diversificado. La ruta con profesorado UVG de 2.5 años suma cuatro años. El costo corresponde al primer semestre de esta etapa.',
    'https://res.cloudinary.com/webuvg/image/upload/v1787162445/WEB/Academico/Carreras/Educaci%C3%B3n/M%C3%BAsica/licenciatura-educacion-musical.pdf',
    'https://www.uvg.edu.gt/carreras/licenciatura-musica/'
);

-- 29/48 | Licenciatura en Educación con Especialidad en English Language Teaching
-- Costo GTQ: cursos 7880 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 9488.
-- Modalidad b-learning corroborada en el programa integrado de la Facultad: https://www.uvg.edu.gt/facultades/educacion/
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Educación con Especialidad en English Language Teaching',
    'Facultad de Educación',
    'Profundiza en enseñanza del inglés, investigación, currículo y dirección de programas educativos. Requiere formación previa de profesorado o técnico y admisión según el folleto.',
    1.5,
    9488,
    'Primer semestre de licenciatura posterior a profesorado o técnico, incluida inscripción',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Currículo ELT", "Evaluación docente", "Investigación", "Gestión educativa", "Escritura académica"]',
    '["Evaluar materiales y programas de inglés.", "Investigar procesos de enseñanza.", "Coordinar proyectos académicos."]',
    '["Coordinación de idiomas", "Gestión académica", "Investigación educativa", "Docencia de inglés"]',
    NULL,
    NULL,
    'Duración registrada: tres ciclos semestrales de licenciatura posteriores al título previo (1.5 años); no es una licenciatura de 1.5 años desde diversificado. La ruta con profesorado UVG de 2.5 años suma cuatro años. El costo corresponde al primer semestre de esta etapa.',
    'https://res.cloudinary.com/webuvg/image/upload/v1786569630/WEB/Academico/Carreras/Educaci%C3%B3n/English%20Language%20Teaching/folleto-licenciatura-elt.pdf',
    'https://www.uvg.edu.gt/carreras/licenciatura-en-english-language-teaching/'
);

-- 30/48 | Licenciatura en Educación con Especialidad en Educación Inclusiva
-- Costo GTQ: cursos 7880 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 9488.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Educación con Especialidad en Educación Inclusiva',
    'Facultad de Educación',
    'Desarrolla competencias para diseñar y gestionar respuestas educativas inclusivas y proyectos de apoyo a la diversidad. Requiere formación previa de profesorado o técnico y admisión según el folleto.',
    1.5,
    9488,
    'Primer semestre de licenciatura posterior a profesorado o técnico, incluida inscripción',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Currículo inclusivo", "Investigación", "Orientación familiar", "Inclusión sociolaboral", "Gestión de proyectos"]',
    '["Diseñar propuestas de inclusión.", "Evaluar necesidades y apoyos educativos.", "Coordinar intervenciones con familias e instituciones."]',
    '["Coordinación de inclusión", "Asesoría educativa", "Proyectos de atención a la diversidad"]',
    NULL,
    NULL,
    'Duración registrada: tres ciclos semestrales de licenciatura posteriores al título previo (1.5 años); no es una licenciatura de 1.5 años desde diversificado. La ruta con profesorado UVG de 2.5 años suma cuatro años. El costo corresponde al primer semestre de esta etapa.',
    'https://res.cloudinary.com/webuvg/image/upload/v1787162422/WEB/Academico/Carreras/Educaci%C3%B3n/Educacion-Inclusiva/licenciatura-educacion-especialidad-educacion-inclusiva.pdf',
    'https://www.uvg.edu.gt/carreras/licenciatura-educacion-inclusiva/'
);

-- 31/48 | Licenciatura en Psicopedagogía
-- Costo GTQ: cursos 7880 + laboratorios 0 + recursos digitales 530 + inscripción 1078 = 9488.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Psicopedagogía',
    'Facultad de Educación',
    'Integra psicología y pedagogía para evaluar el aprendizaje y desarrollar intervenciones en ámbitos escolares, familiares y comunitarios. Requiere formación previa de profesorado o técnico y admisión según el folleto.',
    1.5,
    9488,
    'Primer semestre de licenciatura posterior a profesorado o técnico, incluida inscripción',
    2026,
    'Semipresencial (b-learning)',
    NULL,
    '["Evaluación psicopedagógica", "Intervención psicoeducativa", "Entrevista", "Orientación", "Investigación"]',
    '["Realizar evaluación psicopedagógica.", "Diseñar intervenciones para el aprendizaje.", "Orientar procesos de desarrollo y formación."]',
    '["Intervención psicopedagógica", "Orientación educativa", "Programas de apoyo", "Asesoría y capacitación"]',
    NULL,
    NULL,
    'Duración registrada: tres ciclos semestrales de licenciatura posteriores al título previo (1.5 años); no es una licenciatura de 1.5 años desde diversificado. La ruta con profesorado UVG de 2.5 años suma cuatro años. El costo corresponde al primer semestre de esta etapa.',
    'https://res.cloudinary.com/webuvg/image/upload/v1782136334/WEB/Academico/Carreras/Educaci%C3%B3n/Psicopedagogia/licenciatura-psicopedagogi%CC%81a.pdf',
    'https://www.uvg.edu.gt/carreras/licenciatura-psicopedagogia/'
);

-- 32/48 | Baccalaureatus en Artibus
-- Costo GTQ: cursos 32214 + laboratorios 820 + recursos digitales 780 + inscripción 2810 = 36624.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Baccalaureatus en Artibus',
    'Colegio Universitario',
    'Programa de formación universitaria general con recorrido en humanidades y ciencias sociales. Ofrece plan cerrado de cuatro años y plan abierto cuya duración depende del recorrido elegido.',
    4,
    36624,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Formación general", "Humanidades", "Ciencias sociales", "Investigación"]',
    '["Integrar conocimientos de distintas disciplinas.", "Desarrollar pensamiento crítico.", "Comunicar y analizar información."]',
    NULL,
    NULL,
    NULL,
    'No equivale a una licenciatura profesional ni habilita por sí mismo colegiación. Duración de cuatro años aplicable al plan cerrado; el abierto es variable. Las opciones posteriores dependen del plan y requisitos de la institución receptora. No se atribuyen puestos laborales específicos sin respaldo.',
    'https://res.cloudinary.com/webuvg/image/upload/v1709917954/WEB/Academico/Plan%20de%20estudios/Colegio%20Universitario/2024/baccalaureatus.pdf',
    'https://www.uvg.edu.gt/carreras/baccalaureatus-in-artibus/'
);

-- 33/48 | Baccalaureatus en Scientiis
-- Costo GTQ: cursos 32214 + laboratorios 820 + recursos digitales 780 + inscripción 2810 = 36624.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Baccalaureatus en Scientiis',
    'Colegio Universitario',
    'Programa de formación universitaria general con recorrido en ciencias. Ofrece plan cerrado de cuatro años y plan abierto cuya duración depende del recorrido elegido.',
    4,
    36624,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Formación general", "Ciencias naturales", "Matemática", "Investigación"]',
    '["Integrar conocimientos de distintas disciplinas.", "Desarrollar pensamiento crítico.", "Comunicar y analizar información."]',
    NULL,
    NULL,
    NULL,
    'No equivale a una licenciatura profesional ni habilita por sí mismo colegiación. Duración de cuatro años aplicable al plan cerrado; el abierto es variable. Las opciones posteriores dependen del plan y requisitos de la institución receptora. No se atribuyen puestos laborales específicos sin respaldo.',
    'https://res.cloudinary.com/webuvg/image/upload/v1709917954/WEB/Academico/Plan%20de%20estudios/Colegio%20Universitario/2024/baccalaureatus.pdf',
    'https://www.uvg.edu.gt/carreras/baccalaureatus-in-scientiis/'
);

-- 34/48 | Licenciatura en Arquitectura
-- Costo GTQ: cursos 32214 + laboratorios 1036 + recursos digitales 780 + inscripción 2810 = 36840.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Arquitectura',
    'Escuela de Arquitectura',
    'Forma profesionales para diseñar espacios y edificaciones articulando necesidades humanas, territorio, tecnología y sostenibilidad.',
    5,
    36840,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Diseño arquitectónico", "Construcción", "Estructuras", "Urbanismo", "BIM", "Sostenibilidad"]',
    '["Desarrollar proyectos arquitectónicos.", "Integrar soluciones técnicas y espaciales.", "Representar y coordinar diseños."]',
    '["Diseño arquitectónico", "Desarrollo de proyectos", "Consultoría de construcción", "Planeación de espacios"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1764775527/WEB/Academico/Carreras/Arquitectura/folleto-arquitectura.pdf',
    'https://www.uvg.edu.gt/carreras/arquitectura/'
);

-- 35/48 | Licenciatura en Biología
-- Costo GTQ: cursos 26845 + laboratorios 3022 + recursos digitales 780 + inscripción 2810 = 33457.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Biología',
    'Facultad de Ciencias y Humanidades',
    'Estudia organismos, evolución y ecosistemas mediante investigación científica para comprender y conservar la diversidad biológica.',
    5,
    33457,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Ecología", "Zoología y botánica", "Genética", "Biología molecular", "Bioinformática", "Gestión ambiental"]',
    '["Investigar sistemas biológicos.", "Analizar datos de biodiversidad.", "Proponer acciones de conservación."]',
    '["Investigación biológica", "Conservación", "Gestión ambiental", "Educación científica"]',
    NULL,
    NULL,
    'El folleto 2026 incluye tablas de créditos, pero la extracción disponible no permitió verificar de forma inequívoca todos los totales; no se asigna una carga total aproximada.',
    'https://res.cloudinary.com/webuvg/image/upload/v1778602439/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/2024/biologia-2026.pdf',
    'https://www.uvg.edu.gt/carreras/biologia/'
);

-- 36/48 | Licenciatura en Bioquímica y Microbiología
-- Costo GTQ: cursos 32214 + laboratorios 3679 + recursos digitales 780 + inscripción 2810 = 39483.
-- Duración y modalidad: https://ceps.edu.gt/ceps/prog-view-one?programa_id=311
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Bioquímica y Microbiología',
    'Facultad de Ciencias y Humanidades',
    'Estudia procesos moleculares de los seres vivos y microorganismos, con aplicaciones a investigación, producción y análisis de laboratorio.',
    5,
    39483,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Bioquímica", "Microbiología", "Genética", "Biología molecular", "Análisis de laboratorio"]',
    '["Aplicar técnicas bioquímicas y microbiológicas.", "Interpretar resultados experimentales.", "Investigar procesos biológicos."]',
    '["Laboratorios de análisis", "Investigación", "Biotecnología", "Control microbiológico de alimentos y ambiente"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1765503854/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/2024/folleto-lic-bioquimica-microbiologia.pdf',
    'https://www.uvg.edu.gt/carreras/bioquimica-y-microbiologia/'
);

-- 37/48 | Licenciatura en Biotecnología Molecular
-- Costo GTQ: cursos 32214 + laboratorios 3679 + recursos digitales 780 + inscripción 2810 = 39483.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Biotecnología Molecular',
    'Facultad de Ciencias y Humanidades',
    'Utiliza herramientas moleculares y sistemas biológicos para desarrollar aplicaciones y productos biotecnológicos.',
    5,
    39483,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Biología molecular", "Genética", "Microbiología", "Bioquímica", "Ingeniería de sistemas biológicos"]',
    '["Aplicar técnicas moleculares.", "Diseñar soluciones biotecnológicas.", "Analizar resultados e investigar aplicaciones."]',
    '["Investigación biotecnológica", "Desarrollo de productos", "Laboratorios moleculares", "Biotecnología industrial y ambiental"]',
    NULL,
    NULL,
    'La extracción del folleto contiene un rótulo residual de otra carrera. Se identifica este programa por el título principal, el enlace y la página específica de Biotecnología Molecular; no se trasladan datos de Ingeniería Química Industrial.',
    'https://res.cloudinary.com/webuvg/image/upload/v1765468175/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/2024/folleto-lic-biotecnologia-molecular.pdf',
    'https://www.uvg.edu.gt/carreras/biotecnologia-molecular/'
);

-- 38/48 | Licenciatura en Física
-- Costo GTQ: cursos 32214 + laboratorios 3022 + recursos digitales 780 + inscripción 2810 = 38826.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Física',
    'Facultad de Ciencias y Humanidades',
    'Estudia principios de la materia y la energía mediante modelos matemáticos, experimentación y análisis científico.',
    4,
    38826,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Mecánica", "Electromagnetismo", "Termodinámica", "Física cuántica", "Óptica", "Modelación matemática"]',
    '["Construir y evaluar modelos físicos.", "Diseñar experimentos.", "Analizar y comunicar resultados científicos."]',
    '["Investigación científica", "Desarrollo tecnológico", "Modelación y análisis", "Educación y divulgación"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1710179570/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/2024/fisica.pdf',
    'https://www.uvg.edu.gt/carreras/fisica/'
);

-- 39/48 | Licenciatura en Matemática Aplicada
-- Costo GTQ: cursos 32214 + laboratorios 3022 + recursos digitales 780 + inscripción 2810 = 38826.
-- Fuente discrepante: https://ceps.edu.gt/ceps/prog-view-one?programa_id=428
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Matemática Aplicada',
    'Facultad de Ciencias y Humanidades',
    'Desarrolla modelos y métodos matemáticos para analizar datos y resolver problemas de ciencias, ingeniería, industria y finanzas.',
    NULL,
    38826,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Álgebra y análisis", "Probabilidad y estadística", "Métodos numéricos", "Optimización", "Modelación", "Ciencia de datos"]',
    '["Formular modelos matemáticos.", "Interpretar datos y evaluar soluciones.", "Aplicar herramientas computacionales."]',
    '["Modelación y análisis de datos", "Optimización", "Asesoría en industria y finanzas", "Investigación y educación"]',
    NULL,
    NULL,
    'Duración pendiente: el registro CEPS indica cuatro años; el texto extraído del folleto suministrado incluye rótulos hasta quinto año y bloques repetidos. No se resuelve la discrepancia a partir de una extracción ambigua.',
    'https://res.cloudinary.com/webuvg/image/upload/v1771521745/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/folleto-matematica-aplicada-2025.pdf',
    'https://www.uvg.edu.gt/carreras/matematica-aplicada/'
);

-- 40/48 | Licenciatura en Nutrición
-- Costo GTQ: cursos 32214 + laboratorios 3022 + recursos digitales 780 + inscripción 2810 = 38826.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Nutrición',
    'Facultad de Ciencias y Humanidades',
    'Estudia alimentación y nutrición para evaluar necesidades y desarrollar intervenciones en personas, comunidades y servicios alimentarios.',
    5,
    38826,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Nutrición clínica", "Nutrición deportiva", "Salud pública", "Seguridad alimentaria", "Ciencia de alimentos", "Servicios alimentarios"]',
    '["Evaluar el estado nutricional.", "Diseñar intervenciones alimentarias.", "Gestionar programas y servicios de nutrición."]',
    '["Atención nutricional", "Hospitales y servicios alimentarios", "Nutrición deportiva", "Salud pública", "Industria alimentaria"]',
    NULL,
    NULL,
    'Duración de la licenciatura: cinco años. No se incorpora la duración de maestrías complementarias mencionadas por UVG.',
    'https://res.cloudinary.com/webuvg/image/upload/v1722293764/WEB/Academico/Carreras/Ciencias%20y%20Humanidades/Nutrici%C3%B3n/licencitura-nutricion-folleto.pdf',
    'https://www.uvg.edu.gt/carreras/nutricion/'
);

-- 41/48 | Licenciatura en Química
-- Costo GTQ: cursos 32214 + laboratorios 3022 + recursos digitales 780 + inscripción 2810 = 38826.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Química',
    'Facultad de Ciencias y Humanidades',
    'Estudia composición, propiedades y transformaciones de la materia para investigar, analizar y desarrollar sustancias y productos.',
    4,
    38826,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Química orgánica e inorgánica", "Análisis instrumental", "Fisicoquímica", "Síntesis", "Calidad", "Gestión de laboratorios"]',
    '["Caracterizar y cuantificar sustancias.", "Desarrollar métodos analíticos.", "Investigar y sintetizar productos."]',
    '["Análisis químico", "Investigación", "Desarrollo de productos", "Control de calidad", "Gestión de laboratorios"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1765423018/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/2024/folleto-lic-quimica.pdf',
    'https://www.uvg.edu.gt/carreras/licenciatura-quimica/'
);

-- 42/48 | Licenciatura en Química Farmacéutica
-- Costo GTQ: cursos 32214 + laboratorios 3022 + recursos digitales 780 + inscripción 2810 = 38826.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Química Farmacéutica',
    'Facultad de Ciencias y Humanidades',
    'Forma profesionales para investigar, formular, producir y evaluar medicamentos y cosméticos y contribuir a su uso adecuado.',
    5,
    38826,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Farmacología", "Tecnología farmacéutica", "Análisis y calidad", "Farmacovigilancia", "Atención farmacéutica", "Regulación"]',
    '["Formular y evaluar productos farmacéuticos.", "Aplicar controles de calidad.", "Participar en atención y seguimiento del uso de medicamentos."]',
    '["Industria farmacéutica y cosmética", "Hospitales y farmacias", "Investigación", "Control y regulación farmacéutica"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1778618536/WEB/Academico/Plan%20de%20estudios/Ciencias%20y%20Humanidades/2024/folleto-quimica-farmaceutica-2026.pdf',
    'https://www.uvg.edu.gt/carreras/quimica-farmaceutica/'
);

-- 43/48 | Licenciatura en Antropología
-- Costo GTQ: cursos 32214 + laboratorios 1440 + recursos digitales 780 + inscripción 2810 = 37244.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Antropología',
    'Facultad de Ciencias Sociales',
    'Investiga culturas y sociedades para comprender procesos humanos y desarrollar intervenciones pertinentes a distintos contextos.',
    5,
    37244,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Antropología sociocultural", "Historia", "Métodos cualitativos y cuantitativos", "Análisis político", "Gestión de proyectos"]',
    '["Realizar investigación social.", "Analizar desigualdades y contextos culturales.", "Diseñar proyectos con participación comunitaria."]',
    '["Investigación y consultoría", "Organizaciones públicas y sociales", "Cooperación internacional", "Proyectos de desarrollo", "Museos"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1738855640/WEB/Academico/Plan%20de%20estudios/Ciencias%20Sociales/2024/folleto-antropologia.pdf',
    'https://www.uvg.edu.gt/carreras/licenciatura-en-antropologia/'
);

-- 44/48 | Licenciatura en Arqueología
-- Costo GTQ: cursos 32214 + laboratorios 1440 + recursos digitales 780 + inscripción 2810 = 37244.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Arqueología',
    'Facultad de Ciencias Sociales',
    'Estudia sociedades del pasado mediante sus evidencias materiales y contribuye a investigar, interpretar y gestionar patrimonio cultural.',
    5,
    37244,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Arqueología mesoamericana", "Métodos de campo", "Análisis de materiales", "Patrimonio", "Investigación"]',
    '["Investigar evidencias arqueológicas.", "Interpretar procesos históricos.", "Comunicar y gestionar patrimonio cultural."]',
    '["Investigación arqueológica", "Proyectos de patrimonio", "Museos", "Consultoría arqueológica"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1710255146/WEB/Academico/Plan%20de%20estudios/Ciencias%20Sociales/2024/arqueologia.pdf',
    'https://www.uvg.edu.gt/carreras/licenciatura-en-arqueologia/'
);

-- 45/48 | Licenciatura en Psicología
-- Costo GTQ: cursos 32214 + laboratorios 2386 + recursos digitales 780 + inscripción 2810 = 38190.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Psicología',
    'Facultad de Ciencias Sociales',
    'Estudia comportamiento y procesos mentales para evaluar necesidades y desarrollar intervenciones sustentadas en investigación.',
    5,
    38190,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Psicología clínica y de la salud", "Psicología organizacional", "Cognición y aprendizaje", "Neurociencias", "Investigación"]',
    '["Evaluar procesos psicológicos.", "Diseñar intervenciones fundamentadas.", "Analizar evidencia y comunicar resultados."]',
    '["Servicios psicológicos", "Organizaciones y talento humano", "Ámbito educativo", "Investigación"]',
    NULL,
    NULL,
    'Los ámbitos descritos son opciones del perfil formativo; el ejercicio profesional depende de los requisitos y habilitaciones aplicables.',
    'https://res.cloudinary.com/webuvg/image/upload/v1710255153/WEB/Academico/Plan%20de%20estudios/Ciencias%20Sociales/2024/psicologia.pdf',
    'https://www.uvg.edu.gt/carreras/licenciatura-en-psicologia/'
);

-- 46/48 | Licenciatura en Relaciones Internacionales
-- Costo GTQ: cursos 32214 + laboratorios 1440 + recursos digitales 780 + inscripción 2810 = 37244.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Relaciones Internacionales',
    'Facultad de Ciencias Sociales',
    'Analiza relaciones entre actores internacionales y prepara para formular estrategias, proyectos y respuestas a problemas globales.',
    4,
    37244,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Política internacional", "Economía", "Derecho internacional", "Geopolítica", "Cooperación", "Negociación"]',
    '["Analizar escenarios internacionales.", "Formular propuestas y proyectos.", "Comunicar y negociar en contextos diversos."]',
    '["Cooperación internacional", "Organismos internacionales", "Sector público", "Análisis y gestión de proyectos"]',
    NULL,
    NULL,
    'Se registra la licenciatura de cuatro años. El nombre del enlace antiguo menciona Master of Arts in Global Affairs; no se suma esa maestría ni se presupone que esté incluida en el costo.',
    'https://res.cloudinary.com/webuvg/image/upload/v1710255158/WEB/Academico/Plan%20de%20estudios/Ciencias%20Sociales/2024/relaciones-internacionales.pdf',
    'https://www.uvg.edu.gt/carreras/relaciones-internacionales/'
);

-- 47/48 | Licenciatura en Composición y Producción Musical
-- Costo GTQ: cursos 32214 + laboratorios 1440 + recursos digitales 780 + inscripción 2810 = 37244.
-- Duración: https://noticias.uvg.edu.gt/descubre-tu-pasion-en-la-carrera-de-composicion-y-produccion-musical/
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Composición y Producción Musical',
    'Design Innovation & Arts School',
    'Integra composición y práctica musical con tecnologías de grabación, producción y gestión de proyectos de la industria creativa.',
    4,
    37244,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Composición y arreglos", "Producción musical", "Grabación", "Sonido en vivo", "Diseño sonoro", "Negocios musicales"]',
    '["Crear y arreglar música.", "Producir grabaciones.", "Diseñar soluciones sonoras.", "Gestionar proyectos musicales."]',
    '["Composición", "Producción musical", "Estudios de grabación", "Sonido en vivo", "Proyectos creativos"]',
    NULL,
    NULL,
    'El texto del folleto presenta problemas de codificación; perfil contrastado con la página oficial y duración de cuatro años corroborada en una publicación institucional.',
    'https://res.cloudinary.com/webuvg/image/upload/v1710261194/WEB/Academico/Plan%20de%20estudios/Design%20Innovation%20Arts%20School/2024/composicion-produccion-musical.pdf',
    'https://www.uvg.edu.gt/carreras/composicion-y-produccion-musical/'
);

-- 48/48 | Licenciatura en Diseño de Producto e Innovación
-- Costo GTQ: cursos 32214 + laboratorios 2480 + recursos digitales 780 + inscripción 2810 = 38284.
INSERT INTO uvg_datos_20261028 (
    nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
) VALUES (
    'Licenciatura en Diseño de Producto e Innovación',
    'Design Innovation & Arts School',
    'Desarrolla productos y soluciones a partir de necesidades de las personas, integrando diseño, tecnología e innovación.',
    4,
    38284,
    'Primer semestre, estudiante de primer ingreso, incluida inscripción ordinaria',
    2026,
    'Presencial',
    NULL,
    '["Diseño centrado en usuarios", "Materiales", "Manufactura", "Modelado y prototipado", "Innovación", "Emprendimiento"]',
    '["Investigar necesidades de usuarios.", "Diseñar y evaluar prototipos.", "Integrar criterios técnicos y de viabilidad."]',
    '["Diseño de productos", "Consultoría de diseño", "Desarrollo e innovación", "Emprendimiento"]',
    NULL,
    NULL,
    NULL,
    'https://res.cloudinary.com/webuvg/image/upload/v1710261189/WEB/Academico/Plan%20de%20estudios/Design%20Innovation%20Arts%20School/2024/dise%C3%B1o-producto.pdf',
    'https://www.uvg.edu.gt/carreras/diseno-de-producto-e-innovacion/'
);

-- Actualiza por nombre conservando el id y las relaciones existentes.
UPDATE carreras AS existente
SET
    facultad = datos.facultad,
    descripcion = datos.descripcion,
    duracion = datos.duracion,
    costo = datos.costo,
    costo_periodo = datos.costo_periodo,
    costo_anio = datos.costo_anio,
    modalidad = datos.modalidad,
    carga_creditos = datos.carga_creditos,
    areas_formacion = datos.areas_formacion,
    competencias_desarrolladas = datos.competencias_desarrolladas,
    oportunidades_laborales = datos.oportunidades_laborales,
    salario_estimado = datos.salario_estimado,
    empleabilidad = datos.empleabilidad,
    contexto_laboral = datos.contexto_laboral,
    pdf_oficial_url = datos.pdf_oficial_url,
    url_carrera_oficial = datos.url_carrera_oficial
FROM uvg_datos_20261028 AS datos
WHERE existente.nombre = datos.nombre AND existente.universidad = 'UVG';

-- Inserta solo nombres ausentes; ejecutar de nuevo no duplica estos registros.
INSERT INTO carreras (
    universidad, nombre, facultad, descripcion, duracion, costo, costo_periodo, costo_anio, modalidad, carga_creditos, areas_formacion, competencias_desarrolladas, oportunidades_laborales, salario_estimado, empleabilidad, contexto_laboral, pdf_oficial_url, url_carrera_oficial
)
SELECT 'UVG', datos.nombre, datos.facultad, datos.descripcion, datos.duracion, datos.costo, datos.costo_periodo, datos.costo_anio, datos.modalidad, datos.carga_creditos, datos.areas_formacion, datos.competencias_desarrolladas, datos.oportunidades_laborales, datos.salario_estimado, datos.empleabilidad, datos.contexto_laboral, datos.pdf_oficial_url, datos.url_carrera_oficial
FROM uvg_datos_20261028 AS datos
WHERE NOT EXISTS (
    SELECT 1 FROM carreras AS existente WHERE existente.nombre = datos.nombre AND existente.universidad = 'UVG'
);

-- Verifica que los 48 nombres del lote estén presentes en la tabla de destino.
SELECT COUNT(*) AS programas_del_lote,
       COUNT(*) FILTER (WHERE EXISTS (
            SELECT 1 FROM carreras AS c WHERE c.nombre = d.nombre AND c.universidad = 'UVG'
       )) AS nombres_cargados
FROM uvg_datos_20261028 AS d;

COMMIT;

-- USAC: oferta y perfiles confirmados en el portal oficial de EMI/Facultad de Ingenieria.
-- Fuente: https://emi.ingenieria.usac.edu.gt/carreras/
-- No se infieren costos, duracion, modalidad, creditos, salarios ni empleabilidad.
-- NULL y listas vacias se muestran como "No disponible". No sobrescribir ediciones del encargado.
INSERT INTO carreras (universidad, nombre, facultad, descripcion, areas_formacion,
    competencias_desarrolladas, oportunidades_laborales, url_carrera_oficial)
SELECT 'USAC', datos.nombre, 'Facultad de Ingeniería', datos.descripcion, datos.areas,
    '[]', '[]', 'https://emi.ingenieria.usac.edu.gt/carreras/'
FROM (VALUES
    ('Ingeniería Industrial',
     'Formación orientada a mejorar la eficiencia de procesos de producción, calidad, logística y servicios, con herramientas de gestión y toma de decisiones.',
     '["Gestión empresarial", "Producción y operaciones", "Investigación e innovación"]'),
    ('Ingeniería Mecánica Industrial',
     'Formación en diseño, implementación y mantenimiento de maquinaria y sistemas mecánicos y térmicos aplicados a la industria.',
     '["Gestión empresarial", "Producción y operaciones", "Investigación e innovación", "Mecánica"]')
) AS datos(nombre, descripcion, areas)
WHERE NOT EXISTS (
    SELECT 1 FROM carreras AS existente
    WHERE existente.nombre = datos.nombre AND existente.universidad = 'USAC'
);

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


-- T-10: ejemplos de actividades configurables, sin duplicar ni reactivar las retiradas.
INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones,
acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-t10-cita', '2026-10-20', 'Preparar una cita de orientacion universitaria',
'Conversar con orientacion sobre las carreras de interes',
'Consulta los canales oficiales de la universidad para solicitar una cita',
'Preparar preguntas y confirmar la disponibilidad de una cita',
'Entrevista orientada por preguntas', 'Canales oficiales y agenda personal',
'Fecha acordada, preguntas y conclusiones', 'Cita universitaria')
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones,
acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-t10-familia', '2026-10-22', 'Conversar con tu familia sobre tus opciones',
'Reflexionar sobre expectativas y apoyos para la decision',
'Busca un momento tranquilo para compartir tus opciones de carrera',
'Explicar tus intereses y escuchar las perspectivas de tu familia',
'Conversacion guiada', 'Lista de carreras y preguntas personales',
'Acuerdos, inquietudes y conclusiones personales', 'Acompanamiento familiar')
ON CONFLICT (id_actividad) DO NOTHING;

-- Fecha sugerida para investigar: no representa un plazo oficial de la UVG.
-- Fuente: https://www.uvg.edu.gt/admisiones/becas/
INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones,
acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-ayuda-financiera', '2026-11-16', 'Investigar planes de ayuda financiera',
'Conocer las opciones de apoyo económico de la UVG para evaluar cómo financiar la carrera elegida y planificar el ingreso a la universidad.',
'Consulta el enlace oficial. Compara becas y créditos, revisa sus requisitos y confirma con Admisiones las fechas vigentes. La solicitud de ayuda financiera se realiza después de ser admitido.',
'Revisa el proceso: ingresar al portal, pagar la admisión, completar el formulario y programar la prueba. Tras ser admitido, solicitar ayuda financiera con los documentos requeridos y completar la inscripción.',
'Investigación individual en línea de 45 a 60 minutos; compara las opciones con tu familia o encargado y prepara preguntas para Admisiones.',
'Página oficial de becas y ayudas financieras UVG: https://www.uvg.edu.gt/admisiones/becas/',
'Guarda en tu bitácora las opciones de interés, diferencias entre beca y crédito, requisitos, documentos pendientes, fechas confirmadas y tu siguiente paso.',
'Investigación de ayuda financiera')
ON CONFLICT (id_actividad) DO NOTHING;

-- Actividades de exploración y preparación. Fechas sugeridas de realización,
-- no convocatorias oficiales de la UVG ni fechas de admisión.
-- Fuentes: https://www.uvg.edu.gt/admisiones/aplica-nuestros-programas/
-- https://www.uvg.edu.gt/medios/
-- https://noticias.uvg.edu.gt/abrimos-las-puertas-de-uvg-en-open-house-2025/
-- https://excel.cloud.microsoft/create/es/budget-templates/
-- https://support.google.com/docs/answer/148833?hl=es
-- https://help.libreoffice.org/latest/es/text/scalc/main0503.html
-- https://excel.cloud.microsoft/create/es/blog/budget-plan-examples/

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones,
acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-comparar-planes',
'2026-11-13',
'Comparar los planes de estudio de tres carreras',
'Conocer qué estudiarías en cada opción ayuda a distinguir las carreras y a relacionar sus materias con tus intereses, fortalezas y metas. Continúa la exploración de carreras que ya realizaste en Rumbo.',
'1. Elige tres carreras de interés. 2. Busca sus planes de estudio oficiales. 3. Anota duración, materias, prácticas y requisitos. 4. Compara sus diferencias y marca las asignaturas que más te atraen o te generan dudas.',
'Crea una tabla con una columna por carrera. Compara materias comunes y diferentes, duración y prácticas. Identifica tres materias favoritas por opción y dos dudas para conversar con un profesional o estudiante.',
'Investigación individual de 60 a 90 minutos. Usa el catálogo y comparador de Rumbo como apoyo; confirma la información en las páginas de cada universidad y distingue los datos confirmados de los que faltan por consultar.',
'Catálogo de carreras y comparador de Rumbo; páginas oficiales de las universidades; hojas de cálculo o cuaderno. Oferta académica UVG: https://www.uvg.edu.gt/academico/carreras/',
'Guarda en tu bitácora la tabla comparativa, los enlaces consultados, las materias favoritas, las dudas pendientes y qué carrera se ajusta mejor a tus intereses, explicando por qué.',
'Comparación de planes de estudio')
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones,
acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-entrevista-carrera',
'2026-11-17',
'Entrevistar a un profesional o estudiante de una carrera',
'Conocer experiencias reales permite contrastar lo que leíste en los planes de estudio con las clases, proyectos, retos y trabajo cotidiano. Busca a un profesional del área o a un estudiante que curse la carrera de tu interés.',
'1. Elige una carrera y una persona relacionada. 2. Pregunta qué estudia o en qué trabaja y acuerda la entrevista. 3. Prepara cinco preguntas sobre materias, dificultades, proyectos, trabajo y consejos. 4. Resume las respuestas.',
'También puedes usar Foro: con tu cuenta de estudiante verificada, publica una duda, selecciona la carrera y pide respuestas a quienes la estudien o ejerzan. Revisa publicaciones y responde en el hilo para ampliar tus preguntas.',
'Entrevista de 20 a 30 minutos o intercambio de preguntas en el foro. Pide a la persona indicar su relación con la carrera; distingue experiencias personales de requisitos oficiales y solicita permiso antes de grabar.',
'Menú Foro de Rumbo: crea una publicación de tipo Duda y selecciona la carrera relacionada. Usa las respuestas públicas para conversar; si aún no estás verificado, solicita verificación o realiza la entrevista fuera del foro.',
'Registra la carrera, el perfil de la persona, cinco preguntas y respuestas, tres aprendizajes y si cambió tu interés. Si usaste el foro, anota el título de la publicación para volver a consultarla.',
'Entrevista y exploración vocacional')
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones,
acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-charla-uvg',
'2026-11-19',
'Participar en una charla o taller de orientación de la UVG',
'La UVG ha organizado Experiencia UVG y Open House para conocer carreras, proyectos y laboratorios, conversar con docentes y estudiantes y participar en actividades de orientación. Revisa qué opciones se ajustan a tus intereses.',
'1. Revisa los canales oficiales. 2. Busca una charla, taller o visita sobre tu carrera de interés. 3. Confirma fecha, modalidad, registro y disponibilidad. 4. Prepara tres preguntas y participa cuando exista una convocatoria vigente.',
'El 19 de noviembre revisa la oferta y elige una opción. Si no hay eventos próximos, consulta una charla grabada oficial o pide información a Admisiones. La fecha de Rumbo es para investigar; no anuncia un evento de la UVG.',
'Participación presencial o virtual, según la convocatoria. Ejemplo de un evento anterior, no una invitación vigente: https://noticias.uvg.edu.gt/abrimos-las-puertas-de-uvg-en-open-house-2025/',
'Puedes revisar en sus redes si alguna actividad está pronta a realizarse en la universidad: redes de la Universidad del Valle de Guatemala https://www.uvg.edu.gt/medios/ | Instagram: https://www.instagram.com/uvggt/',
'Anota evento o grabación, enlace, fecha confirmada, carrera relacionada, tres aprendizajes y respuestas a tus preguntas. Explica si la experiencia cambió tu percepción de la carrera; si no hay evento, registra tu siguiente consulta.',
'Charlas y orientación universitaria')
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones,
acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-presupuesto-universitario',
'2026-11-23',
'Preparar y comparar presupuestos universitarios',
'Después de investigar ayudas financieras, estima el costo de estudiar tus opciones de carrera. Incluye matrícula, cuotas, materiales, transporte, alimentación e internet; distingue gastos mensuales de pagos únicos.',
'1. Elige dos opciones y consulta costos oficiales. 2. Usa una plantilla con concepto, monto, frecuencia y fuente. 3. Calcula totales mensuales y anuales por el mismo período. 4. Compara ingresos disponibles con gastos.',
'Compara un escenario sin ayuda con otro de apoyo confirmado; marca cualquier beca hipotética como pendiente. Añade un margen para imprevistos y comprueba que no repitas pagos únicos. Identifica cuánto falta financiar en cada opción.',
'Si eres principiante, usa una plantilla. Una IA como Copilot puede sugerir categorías, explicar fórmulas y comparar escenarios; algunas funciones requieren un plan compatible. Comprueba los cálculos con costos confirmados.',
'Excel (plantillas): https://excel.cloud.microsoft/create/es/budget-templates/ | Google Sheets: https://sheets.google.com/ | LibreOffice Calc: https://help.libreoffice.org/latest/es/text/scalc/main0503.html',
'Guarda el archivo o su enlace, costos y fuentes, supuestos, totales, diferencia entre ingresos y gastos y opción más viable. Si usaste IA, anota qué te ayudó a hacer y qué cifras o fórmulas verificaste.',
'Planificación económica universitaria')
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones,
acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad)
VALUES ('act-plan-ingreso',
'2026-11-25',
'Crear un plan de preparación para el ingreso universitario',
'Las universidades suelen solicitar identificación, fotografía, notas y constancia de graduación. Requisitos y pruebas varían por programa; organiza tus pendientes usando el ingreso ordinario a licenciaturas de la UVG como ejemplo.',
'UVG solicita fotografía, certificado oficial de cursos desde 1.º básico hasta 4.º o 5.º diversificado, y DPI; si eres menor, partida de nacimiento; si eres extranjero, pasaporte. Consulta los requisitos aplicables a tu caso.',
'1. Crea tu usuario y completa la solicitud. 2. Adjunta los documentos. 3. Paga, agenda, prepara y realiza la PAA. 4. Consulta la resolución del Comité: necesitas ser admitido para iniciar la preinscripción; la prueba no garantiza admisión.',
'Crea una lista de pendientes con responsable y fecha. Confirma documentos de inscripción y el proceso de tu programa con Admisiones: la Facultad de Educación tiene un procedimiento distinto. Si ya hiciste la PAA, consulta cómo validar el resultado.',
'Proceso y documentos UVG: https://www.uvg.edu.gt/admisiones/aplica-nuestros-programas/ | Solicitud: https://admisiones.uvg.edu.gt/ | Fechas de evaluación: https://www.uvg.edu.gt/fechas-de-admision/',
'Registra universidad, carrera, documentos listos y pendientes, fechas confirmadas y plan de estudio para la PAA. Anota el estado de admisión y próximos trámites; conserva tus documentos personales en un lugar privado.',
'Preparación para admisión universitaria')
ON CONFLICT (id_actividad) DO NOTHING;
