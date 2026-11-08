-- Catálogo de especialidades (Fig. 11) y usuarios de demostración (mismos user_id que auth_db)

INSERT INTO specialty (code, description) VALUES
 ('HARDWARE_MANTENIMIENTO', 'Diagnóstico y reparación de servidores, estaciones de trabajo, impresoras y periféricos'),
 ('SISTEMAS_SOFTWARE',      'Fallas de sistemas operativos (Windows, Linux), suite ofimática y software empresarial'),
 ('REDES_CONECTIVIDAD',     'Switches, routers, VPNs, enlaces de red, firewalls y direccionamiento IP'),
 ('BASES_DATOS',            'Optimización de consultas, bloqueos, respaldos y fallas en motores relacionales o NoSQL'),
 ('SEGURIDAD_INFORMATICA',  'Malware, certificados SSL, respuesta a incidentes de seguridad y políticas de acceso');

INSERT INTO client (seq, user_id, first_name, last_name, email, phone, company_name, ruc, status, created_at, updated_at) VALUES
 (1, 'USR-C0001', 'Carlos', 'Gonzalez', 'cgonzalez@techsupport.pe', '+51987654321', 'Manufacturas del Norte S.A.C.', '20512345678', 'ACTIVO',     UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 (2, 'USR-C0002', 'Ana',    'Torres',   'atorres@textilessur.pe',   '+51976543210', 'Textiles del Sur S.A.C.',       '20498765432', 'ACTIVO',     UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 (3, 'USR-C0003', 'Miguel', 'Rojas',    'mrojas@comercialandina.pe','+51965432109', 'Comercial Andina S.R.L.',       '20123456789', 'SUSPENDIDO', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO technician (seq, user_id, first_name, last_name, email, phone, support_level, status, is_available, current_tickets, max_concurrent_tickets, created_at, updated_at) VALUES
 (1, 'USR-T0001', 'Luis',  'Ramírez',  'lramirez@techsupport.pe',  '+51912345678', 'N2', 'ACTIVO', TRUE, 0, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 (2, 'USR-T0002', 'Mario', 'Torres',   'mtorres@techsupport.pe',   '+51923456789', 'N3', 'ACTIVO', TRUE, 0, 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 (3, 'USR-T0003', 'Ana',   'Quispe',   'aquispe@techsupport.pe',   '+51934567890', 'N1', 'ACTIVO', TRUE, 0, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 (4, 'USR-T0004', 'Jorge', 'Salazar',  'jsalazar@techsupport.pe',  '+51945678901', 'N1', 'ACTIVO', TRUE, 0, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 (5, 'USR-T0005', 'Rosa',  'Huamán',   'rhuaman@techsupport.pe',   '+51956789012', 'N2', 'ACTIVO', TRUE, 0, 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
 (6, 'USR-T0006', 'Pedro', 'Castillo', 'pcastillo@techsupport.pe', '+51967890123', 'N3', 'ACTIVO', TRUE, 0, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO technician_specialty (technician_seq, specialty) VALUES
 (1, 'HARDWARE_MANTENIMIENTO'), (1, 'REDES_CONECTIVIDAD'), (1, 'BASES_DATOS'),
 (2, 'BASES_DATOS'), (2, 'SEGURIDAD_INFORMATICA'),
 (3, 'HARDWARE_MANTENIMIENTO'), (3, 'SISTEMAS_SOFTWARE'),
 (4, 'REDES_CONECTIVIDAD'),
 (5, 'SISTEMAS_SOFTWARE'), (5, 'SEGURIDAD_INFORMATICA'),
 (6, 'REDES_CONECTIVIDAD'), (6, 'SISTEMAS_SOFTWARE'), (6, 'HARDWARE_MANTENIMIENTO');
