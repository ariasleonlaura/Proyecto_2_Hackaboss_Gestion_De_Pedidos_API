-- ==========================================
-- 1. TERMINALES
-- ==========================================
INSERT INTO terminales (nombre) VALUES ('Caja Principal');        -- ID 1
INSERT INTO terminales (nombre) VALUES ('Caja Secundaria');       -- ID 2
INSERT INTO terminales (nombre) VALUES ('Kiosco Autoservicio 1'); -- ID 3

-- ==========================================
-- 2. CATEGORIAS (IDs del 1 al 7)
-- ==========================================
INSERT INTO categorias (nombre) VALUES ('Entrantes para compartir'); -- ID 1
INSERT INTO categorias (nombre) VALUES ('Hamburguesas de Ternera');  -- ID 2
INSERT INTO categorias (nombre) VALUES ('Hamburguesas de Pollo');    -- ID 3
INSERT INTO categorias (nombre) VALUES ('Opciones Veganas');         -- ID 4
INSERT INTO categorias (nombre) VALUES ('Bebidas');                  -- ID 5
INSERT INTO categorias (nombre) VALUES ('Postres');                  -- ID 6
INSERT INTO categorias (nombre) VALUES ('Salsas Extras');            -- ID 7

-- ==========================================
-- 3. PRODUCTOS (IDs del 1 al 30, en este orden)
-- ==========================================

-- Entrantes (Categoria 1)
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Patatas Fritas Clásicas', 2.90, true, 1);              -- ID 1
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Patatas Deluxe con Bacon y Queso', 4.50, true, 1);     -- ID 2
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Aros de Cebolla Crujientes (8 ud)', 3.90, true, 1);    -- ID 3
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Tequeños de Queso Fundido (5 ud)', 5.50, true, 1);     -- ID 4
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Alitas de Pollo BBQ (6 ud)', 4.90, true, 1);           -- ID 5
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Nachos Machos (Guacamole, Pico de Gallo y Cheddar)', 6.50, true, 1); -- ID 6

-- Hamburguesas de Ternera (Categoria 2)
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Classic Burger (Lechuga, Tomate, Cebolla)', 7.90, true, 2);   -- ID 7
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Bacon Cheeseburger', 8.90, true, 2);                          -- ID 8
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Doble Smash Burger', 10.50, true, 2);                         -- ID 9
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('La Trufada (Huevo Frito y Mayonesa de Trufa)', 11.90, true, 2); -- ID 10
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('La Bestia (Triple carne, aros de cebolla y BBQ)', 13.50, true, 2); -- ID 11

-- Hamburguesas de Pollo (Categoria 3)
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Crispy Chicken Burger', 7.50, true, 3);     -- ID 12
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Chicken BBQ Dulce', 8.50, true, 3);         -- ID 13
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Spicy Buffalo Chicken (¡Pica!)', 8.90, true, 3); -- ID 14

-- Opciones Veganas (Categoria 4)
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Beyond Meat Burger Clásica', 9.50, true, 4);     -- ID 15
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Ensalada César Vegana (No-Pollo)', 7.50, true, 4); -- ID 16
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Nuggets Veganos (6 ud)', 4.50, true, 4);          -- ID 17

-- Bebidas (Categoria 5)
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Coca-Cola Original 33cl', 2.50, true, 5); -- ID 18
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Coca-Cola Zero 33cl', 2.50, true, 5);     -- ID 19
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Fanta Naranja 33cl', 2.50, true, 5);      -- ID 20
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Agua Mineral 50cl', 1.50, true, 5);       -- ID 21
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Cerveza Estrella Galicia', 2.80, true, 5); -- ID 22
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Cerveza Tostada 1906', 3.20, true, 5);    -- ID 23

-- Postres (Categoria 6)
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Tarta de Queso Casera', 4.90, true, 6);             -- ID 24
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Brownie de Chocolate con Helado', 5.50, true, 6);   -- ID 25
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Batido de Oreo', 4.50, true, 6);                    -- ID 26

-- Salsas (Categoria 7)
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Ketchup', 0.50, true, 7);                       -- ID 27
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Mayonesa', 0.50, true, 7);                      -- ID 28
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Salsa BBQ', 0.80, true, 7);                     -- ID 29
INSERT INTO productos (nombre, precio, activo, categoria_id) VALUES ('Salsa Queso Cheddar Líquido', 1.00, true, 7);   -- ID 30

-- ==========================================
-- 4. PEDIDOS (de ejemplo, para tests con Postman/curl)
-- ==========================================

-- Pedido 1: 2x Classic Burger + 1x Coca-Cola Original -> total 18.30
INSERT INTO pedidos (codigo, fecha, total, estado_pedido, terminal_id)
VALUES ('PED-AAAA1111', NOW(), 18.30, 'CREADO', 1);

-- Pedido 2: 1x Bacon Cheeseburger + 1x Patatas Fritas Clásicas -> total 11.80
INSERT INTO pedidos (codigo, fecha, total, estado_pedido, terminal_id)
VALUES ('PED-BBBB2222', NOW(), 11.80, 'PREPARACION', 2);

-- Pedido 3: 2x Agua Mineral -> total 3.00
INSERT INTO pedidos (codigo, fecha, total, estado_pedido, terminal_id)
VALUES ('PED-CCCC3333', NOW(), 3.00, 'LISTO', 1);

-- ==========================================
-- 5. PEDIDOS_PRODUCTOS (líneas de cada pedido) OPCIONAL
-- ==========================================

/*
-- Pedido 1 (id 1)
INSERT INTO pedidos_productos (cantidad, precio_unitario, pedido_id, producto_id)
VALUES (2, 7.90, 1, 7);  -- 2x Classic Burger

INSERT INTO pedidos_productos (cantidad, precio_unitario, pedido_id, producto_id)
VALUES (1, 2.50, 1, 18); -- 1x Coca-Cola Original

-- Pedido 2 (id 2)
INSERT INTO pedidos_productos (cantidad, precio_unitario, pedido_id, producto_id)
VALUES (1, 8.90, 2, 8);  -- 1x Bacon Cheeseburger

INSERT INTO pedidos_productos (cantidad, precio_unitario, pedido_id, producto_id)
VALUES (1, 2.90, 2, 1);  -- 1x Patatas Fritas Clásicas

-- Pedido 3 (id 3)
INSERT INTO pedidos_productos (cantidad, precio_unitario, pedido_id, producto_id)
VALUES (2, 1.50, 3, 21); -- 2x Agua Mineral
*/