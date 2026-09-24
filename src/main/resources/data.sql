VALUES ('usr-0001', 'admin', 'Administrador General', 'admin@rumbo.com',
        '$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji',
        'ADMIN', 'VERIFICADO')
ON CONFLICT (id_usuario) DO UPDATE SET
    contrasenia = EXCLUDED.contrasenia,
    correo = EXCLUDED.correo;
    
INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones, acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad, completada)
VALUES ('act-0001', '2026-01-15', 'Autoevaluación de intereses vocacionales', 'Primer contacto del estudiante con el proceso de orientación', 'Completa el test de intereses vocacionales de forma honesta y sin apuros', 'Realizar el test externo y anotar tus 3 áreas de interés principales', 'Prueba estandarizada de intereses (tipo Holland)', 'Enlace externo a prueba vocacional, papel y lápiz', 'Guarda tus 3 áreas de interés principales en tu bitácora', 'Autoevaluación', false)
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones, acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad, completada)
VALUES ('act-0002', '2026-02-10', 'Exploración de carreras afines', 'Después de conocer tus intereses, busca carreras que coincidan', 'Investiga al menos 5 carreras relacionadas con tus áreas de interés', 'Consultar guías de carreras y anotar pros y contras de cada una', 'Investigación documental y comparativa', 'Guías universitarias, internet, revistas de orientación', 'Lista de 5 carreras con sus principales fortalezas', 'Exploración', false)
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones, acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad, completada)
VALUES ('act-0003', '2026-03-05', 'Prueba de aptitudes académicas', 'Evaluación de fortalezas cognitivas para orientar la decisión', 'Realiza la prueba de aptitudes en un ambiente tranquilo y sin distracciones', 'Completar el cuestionario de aptitudes y calificar según las instrucciones', 'Prueba psicométrica de aptitudes', 'Cuestionario impreso, lápiz, cronómetro', 'Tus puntajes en cada dimensión de aptitud', 'Evaluación', false)
ON CONFLICT (id_actividad) DO NOTHING;

INSERT INTO actividad (id_actividad, fecha, descripcion, contexto, instrucciones, acciones_a_realizar, metodo_recomendado, herramientas_disponibles, informacion_a_guardar, tipo_actividad, completada)
VALUES ('act-0004', '2026-04-20', 'Toma de decisión y planificación', 'Momento de elegir la carrera y diseñar el plan de estudio', 'Reúne toda tu información y toma una decisión fundamentada', 'Revisar bitácora, comparar opciones y elegir la carrera que mejor se alinee', 'Matriz de decisión ponderada', 'Bitácora, hoja de cálculo o cuaderno', 'Carrera elegida y razones de tu decisión', 'Decisión', false)
ON CONFLICT (id_actividad) DO NOTHING;
