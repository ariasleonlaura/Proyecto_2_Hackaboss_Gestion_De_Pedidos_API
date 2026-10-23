-- ==========================================
-- V2__catalogo_ejemplo.sql: Datos de catálogo inicial
-- ==========================================

-- ==========================================
-- 1. TERMINALES (IDs explícitos: 1, 2, 3)
-- ==========================================
INSERT INTO terminales (id, nombre) VALUES (1, 'Caja Principal');
INSERT INTO terminales (id, nombre) VALUES (2, 'Caja Secundaria');
INSERT INTO terminales (id, nombre) VALUES (3, 'Kiosco Autoservicio 1');

-- ==========================================
-- 2. CATEGORÍAS (IDs explícitos: 1 al 7)
-- ==========================================
INSERT INTO categorias (id, nombre) VALUES (1, 'Entrantes para compartir');
INSERT INTO categorias (id, nombre) VALUES (2, 'Hamburguesas de Ternera');
INSERT INTO categorias (id, nombre) VALUES (3, 'Hamburguesas de Pollo');
INSERT INTO categorias (id, nombre) VALUES (4, 'Opciones Veganas');
INSERT INTO categorias (id, nombre) VALUES (5, 'Bebidas');
INSERT INTO categorias (id, nombre) VALUES (6, 'Postres');
INSERT INTO categorias (id, nombre) VALUES (7, 'Salsas Extras');

-- ==========================================
-- 3. PRODUCTOS (IDs explícitos: 1 al 30, en el mismo orden que data.sql)
-- ==========================================

-- Entrantes (Categoria 1)
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (1, 'Patatas Fritas Clásicas', 2.90, true, 1);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (2, 'Patatas Deluxe con Bacon y Queso', 4.50, true, 1);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (3, 'Aros de Cebolla Crujientes (8 ud)', 3.90, true, 1);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (4, 'Tequeños de Queso Fundido (5 ud)', 5.50, true, 1);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (5, 'Alitas de Pollo BBQ (6 ud)', 4.90, true, 1);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (6, 'Nachos Machos (Guacamole, Pico de Gallo y Cheddar)', 6.50, true, 1);

-- Hamburguesas de Ternera (Categoria 2)
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (7, 'Classic Burger (Lechuga, Tomate, Cebolla)', 7.90, true, 2);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (8, 'Bacon Cheeseburger', 8.90, true, 2);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (9, 'Doble Smash Burger', 10.50, true, 2);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (10, 'La Trufada (Huevo Frito y Mayonesa de Trufa)', 11.90, true, 2);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (11, 'La Bestia (Triple carne, aros de cebolla y BBQ)', 13.50, true, 2);

-- Hamburguesas de Pollo (Categoria 3)
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (12, 'Crispy Chicken Burger', 7.50, true, 3);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (13, 'Chicken BBQ Dulce', 8.50, true, 3);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (14, 'Spicy Buffalo Chicken (¡Pica!)', 8.90, true, 3);

-- Opciones Veganas (Categoria 4)
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (15, 'Beyond Meat Burger Clásica', 9.50, true, 4);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (16, 'Ensalada César Vegana (No-Pollo)', 7.50, true, 4);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (17, 'Nuggets Veganos (6 ud)', 4.50, true, 4);

-- Bebidas (Categoria 5)
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (18, 'Coca-Cola Original 33cl', 2.50, true, 5);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (19, 'Coca-Cola Zero 33cl', 2.50, true, 5);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (20, 'Fanta Naranja 33cl', 2.50, true, 5);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (21, 'Agua Mineral 50cl', 1.50, true, 5);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (22, 'Cerveza Estrella Galicia', 2.80, true, 5);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (23, 'Cerveza Tostada 1906', 3.20, true, 5);

-- Postres (Categoria 6)
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (24, 'Tarta de Queso Casera', 4.90, true, 6);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (25, 'Brownie de Chocolate con Helado', 5.50, true, 6);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (26, 'Batido de Oreo', 4.50, true, 6);

-- Salsas (Categoria 7)
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (27, 'Ketchup', 0.50, true, 7);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (28, 'Mayonesa', 0.50, true, 7);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (29, 'Salsa BBQ', 0.80, true, 7);
INSERT INTO productos (id, nombre, precio, activo, categoria_id) VALUES (30, 'Salsa Queso Cheddar Líquido', 1.00, true, 7);

-- ==========================================
-- 4. Restablecer secuencias para que los IDs explícitos no colisionen
-- ==========================================
SELECT setval(pg_get_serial_sequence('terminales', 'id'), 3);
SELECT setval(pg_get_serial_sequence('categorias', 'id'), 7);
SELECT setval(pg_get_serial_sequence('productos', 'id'), 30);