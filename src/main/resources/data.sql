INSERT INTO usuario (id_usuario, nombre_usuario, nombre_completo, correo, contrasenia, rol, estado_verificacion)
VALUES ('usr-0001', 'admin', 'Administrador General', 'admin@rumbo.com',
        '$2a$10$TpLF6dwLPJqJiTr25OrXj.GblWwrnBOb1QyFtXs0vr0hjwvld9bji',
        'ADMIN', 'VERIFICADO')
ON CONFLICT (id_usuario) DO UPDATE SET
    contrasenia = EXCLUDED.contrasenia,
    correo = EXCLUDED.correo;
