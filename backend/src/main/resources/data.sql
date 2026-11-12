INSERT INTO USUARIOS (username, password, nombre_completo, email, rol) VALUES
('admin', '$2a$12$c9cmUKX80PGUEtXUcwpPCe0tVhbWj/bIZnw2sThP4pReaPtVly2Zq', 'Administrador General de Sistemas', 'admin@insecurepay.fi.cr', 'ADMIN'),
('auditor_seguridad', '$2a$12$2LlqXxceJ5hpBEfPs8Lw4.hnpf65yOISYYEM/HNLCQmKrx2FLDJ7a', 'Lic. Esteban Solano Chaves', 'esolano@insecurepay.fi.cr', 'AUDITOR'),
('carlos_mora', '$2a$12$ccth/Fk1Z7f3uqYgZ2EbRO31iYjwpeeLSoSYIxk1hPYjmOlpaTLT2', 'Carlos Mora Morales', 'cmora@correo.cr', 'CLIENTE'),
('maria_rojas', '$2a$12$u/61e.w9lNNKzIRTPWAqQeBz3/Uias1sP5/8g3ySqcFcS/OfFH65C', 'María Rojas Soto', 'mrojas@empresa.cr', 'CLIENTE'),
('juan_perez', '$2a$12$QrTC1mqAFjs0g/U4Apk86.k5PZUf6MxyFEbhLn5dVzn.CzRaEkviK', 'Juan Pérez Delgado', 'jperez@correo.cr', 'CLIENTE');

INSERT INTO CUENTAS_BANCARIAS (usuario_id, numero_cuenta, tipo_cuenta, saldo, moneda, activa) VALUES
(1, 'CR05015202001026458900', 'CORRIENTE', 50000000.00, 'CRC', TRUE),
(3, 'CR05015202001026458901', 'AHORROS', 250000.00, 'CRC', TRUE),
(4, 'CR05015202001026458902', 'CORRIENTE', 4850000.00, 'CRC', TRUE),
(5, 'CR05015202001026458903', 'AHORROS', 75000.00, 'CRC', TRUE);

INSERT INTO TRANSACCIONES (cuenta_origen_id, cuenta_destino_id, monto, concepto_html, fecha, estado) VALUES
(2, 3, 45000.00, 'Pago de servicios profesionales de programacion. Verificado', '2026-03-01 10:15:00', 'COMPLETADA'),
(3, 4, 120000.00, 'Cancelacion cuota de alquiler oficina central. Factura electronica #8849.', '2026-03-05 14:30:00', 'COMPLETADA'),
(3, 2, 15000.00, 'Reembolso almuerzo equipo de trabajo.', '2026-03-12 11:20:00', 'COMPLETADA'),
(4, 2, 35000.00, 'Aporte fondo solidario cooperativo. Transferencia inmediata SINPE Movil', '2026-03-20 09:45:00', 'COMPLETADA');
