-- Datos semilla para la demo. Contraseñas: Admin123! | Tecnico123! | Cliente123!
-- Los user_id coinciden con los perfiles sembrados en user_db.

INSERT INTO role_catalog (code, description) VALUES
 ('ADMIN',   'Administrador del sistema: gestión de usuarios, supervisión de tickets, asignación manual y métricas'),
 ('CLIENTE', 'Usuario de empresa abonada: registra y consulta sus incidencias y confirma el cierre'),
 ('TECNICO', 'Personal técnico: atiende tickets asignados, escala, reasigna y registra la solución');

INSERT INTO credential (user_id, email, password_hash, role, full_name, status, created_at, updated_at) VALUES
 ('USR-A0001', 'admin@techsupport.pe',     '$2a$10$cqEpms8sTp2hUbD4RDgjQe.QEuRwpNV7FRckFXvPo5srUabAFgEM.', 'ADMIN',   'Administrador General', 'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 ('USR-T0001', 'lramirez@techsupport.pe',  '$2a$10$AOrXvYiW/vilf3jPtP0BN.nHNIHnIL10TEhj7vCBJeWQcPErNdVla', 'TECNICO', 'Luis Ramírez',    'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 ('USR-T0002', 'mtorres@techsupport.pe',   '$2a$10$AOrXvYiW/vilf3jPtP0BN.nHNIHnIL10TEhj7vCBJeWQcPErNdVla', 'TECNICO', 'Mario Torres',    'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 ('USR-T0003', 'aquispe@techsupport.pe',   '$2a$10$AOrXvYiW/vilf3jPtP0BN.nHNIHnIL10TEhj7vCBJeWQcPErNdVla', 'TECNICO', 'Ana Quispe',      'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 ('USR-T0004', 'jsalazar@techsupport.pe',  '$2a$10$AOrXvYiW/vilf3jPtP0BN.nHNIHnIL10TEhj7vCBJeWQcPErNdVla', 'TECNICO', 'Jorge Salazar',   'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 ('USR-T0005', 'rhuaman@techsupport.pe',   '$2a$10$AOrXvYiW/vilf3jPtP0BN.nHNIHnIL10TEhj7vCBJeWQcPErNdVla', 'TECNICO', 'Rosa Huamán',     'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 ('USR-T0006', 'pcastillo@techsupport.pe', '$2a$10$AOrXvYiW/vilf3jPtP0BN.nHNIHnIL10TEhj7vCBJeWQcPErNdVla', 'TECNICO', 'Pedro Castillo',  'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 ('USR-C0001', 'cgonzalez@techsupport.pe', '$2a$10$ozTgM8IhjGdKzKsVbfp8wOOy/FwEhu2ZzJilnDoHPiRXYNHO/iteO', 'CLIENTE', 'Carlos Gonzalez', 'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 ('USR-C0002', 'atorres@textilessur.pe',   '$2a$10$ozTgM8IhjGdKzKsVbfp8wOOy/FwEhu2ZzJilnDoHPiRXYNHO/iteO', 'CLIENTE', 'Ana Torres',      'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 ('USR-C0003', 'mrojas@comercialandina.pe','$2a$10$ozTgM8IhjGdKzKsVbfp8wOOy/FwEhu2ZzJilnDoHPiRXYNHO/iteO', 'CLIENTE', 'Miguel Rojas',    'ACTIVO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
