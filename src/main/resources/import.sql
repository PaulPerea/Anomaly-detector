-- Usuarios de prueba
-- password para todos: Password123!
-- hash bcrypt generado con cost=12

INSERT INTO users (username, password_hash, email, role, active, created_at) VALUES
('admin', '$2a$12$LcM2u7GIuUaxSH.o5gxJp.bFBtJ8F6BOZxhiSnGgHWUBi0lfH25Yq', 'admin@security.com', 'ADMIN', true, NOW()),
('juan_perez', '$2a$12$LcM2u7GIuUaxSH.o5gxJp.bFBtJ8F6BOZxhiSnGgHWUBi0lfH25Yq', 'juan@example.com', 'USER', true, NOW()),
('maria_gomez', '$2a$12$LcM2u7GIuUaxSH.o5gxJp.bFBtJ8F6BOZxhiSnGgHWUBi0lfH25Yq', 'maria@example.com', 'USER', true, NOW()),
('carlos_rivas', '$2a$12$LcM2u7GIuUaxSH.o5gxJp.bFBtJ8F6BOZxhiSnGgHWUBi0lfH25Yq', 'carlos@example.com', 'USER', true, NOW());

-- Datos sensibles ficticios
INSERT INTO sensitive_data (owner_id, dni, email, phone, account_number, created_at) VALUES
(2, '12345678', 'juan@example.com', '+51987654321', 'BCP-001-2024-9876', NOW()),
(2, '12345678', 'juan.alt@gmail.com', '+51912345678', 'BCP-001-2024-0001', NOW()),
(3, '87654321', 'maria@example.com', '+51998765432', 'BCP-002-2024-5432', NOW()),
(4, '11223344', 'carlos@example.com', '+51976543210', 'BCP-003-2024-7890', NOW());
