# Gestión de Pedidos Frontend

Aplicación web desarrollada con React para el consumo de la API REST de Gestión de Pedidos.

El frontend actúa como interfaz visual para los distintos roles del sistema (terminal, cocina y pantalla de recogida), permitiendo gestionar los pedidos mediante llamadas HTTP al backend desarrollado en Spring Boot.

La aplicación no contiene lógica de negocio, sino que consume los endpoints expuestos por la API y muestra la información al usuario, manteniendo una clara separación entre frontend y backend.

## Tecnologías utilizadas

* React
* JavaScript
* Vite
* React Router DOM
* Fetch API
* CSS
* Sonner

## Arquitectura del proyecto

El proyecto sigue una arquitectura modular basada en responsabilidades:

Pages → Components → Hooks → API → Backend

Además se utilizan:

* Hooks personalizados para la carga de datos.
* Componentes reutilizables para estados de carga y errores.
* React Router para la navegación entre vistas.
* DTOs para documentar la estructura de los datos recibidos desde la API.
* Separación de estilos por páginas y componentes.

## Funcionalidades principales

### Terminal

* Seleccionar una terminal.
* Listar los productos disponibles agrupados por categorías.
* Añadir y eliminar productos del carrito.
* Visualizar el importe total del pedido.
* Registrar un nuevo pedido.
* Mostrar el código generado automáticamente.

### Cocina

* Listar pedidos en estado CREADO.
* Listar pedidos en estado PREPARACION.
* Visualizar el detalle de cada pedido.
* Cambiar el estado respetando el flujo definido.
* Actualización automática de pedidos cada 15 segundos.

### Recogida

* Listar pedidos en estado LISTO.
* Cobrar pedidos (LISTO → PAGADO).
* Entregar pedidos (PAGADO → ENTREGADO).
* Actualización automática de pedidos cada 15 segundos.

## Estados del pedido

Los pedidos siguen el mismo flujo definido en el backend:

**CREADO → PREPARACION → LISTO → PAGADO → ENTREGADO**

No se permiten saltos de estado.

| Estado      | Descripción                               |
| ----------- | ----------------------------------------- |
| CREADO      | Pedido registrado desde la terminal       |
| PREPARACION | La cocina está preparando el pedido       |
| LISTO       | El pedido está preparado para su recogida |
| PAGADO      | El pedido ha sido abonado                 |
| ENTREGADO   | El pedido ha sido entregado al cliente    |

## Endpoints consumidos

### Terminales

GET /api/terminales

### Productos

GET /api/productos

### Pedidos

POST /api/pedidos

GET /api/pedidos?estado=CREADO

GET /api/pedidos?estado=PREPARACION

GET /api/pedidos?estado=LISTO

GET /api/pedidos?estado=PAGADO

PATCH /api/pedidos/{pedidoId}/estado

## Flujo de usuario

1. El usuario selecciona una terminal.
2. Consulta los productos disponibles.
3. Añade productos al carrito.
4. Registra el pedido.
5. Cocina cambia el estado del pedido de CREADO a PREPARACION y posteriormente a LISTO.
6. Desde la pantalla de recogida se cobra el pedido.
7. Finalmente el pedido se entrega al cliente.

## Estructura del proyecto

```text
src/
├── api/
├── components/
├── hooks/
├── pages/
├── router/
├── styles/
├── utils/
│   ├── constants.js
│   └── dtos.js
├── App.jsx
└── main.jsx
```

## Manejo de errores

La aplicación incorpora un manejo básico de errores para mejorar la experiencia de usuario.

* Captura de errores en las llamadas a la API.
* Componente reutilizable ErrorMessage.
* Estados de carga mediante el componente Loading.
* Notificaciones mediante Sonner.
* Mensajes amigables cuando el servidor no está disponible.
* Página personalizada para rutas no existentes (404).

Ejemplo de mensaje mostrado:

> Servidor no disponible. Inténtalo de nuevo más tarde.

## Capturas de la aplicación

### Selección de terminal

![Selección de terminal](docs/seleccion-terminal.png)

Pantalla inicial donde el usuario selecciona la terminal desde la que va a registrar los pedidos.

### Vista Terminal

![Vista Terminal](docs/terminal.png)

Permite seleccionar productos, gestionar el carrito y crear nuevos pedidos.

### Vista Cocina

![Vista Cocina](docs/cocina.png)

Permite gestionar los pedidos pendientes y avanzar su estado de preparación.

### Vista Recogida

![Vista Recogida](docs/recogida.png)

Permite cobrar y entregar los pedidos completando el flujo de trabajo.

## Configuración del proyecto

La aplicación consume la API REST desarrollada en el Proyecto I.

La URL base utilizada es:

```text
http://localhost:8080/api
```

Es necesario que el backend esté en ejecución antes de iniciar el frontend.

## Cómo ejecutar el proyecto

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
```

### 2. Instalar dependencias

```bash
npm install
```

### 3. Ejecutar la aplicación

```bash
npm run dev
```

### 4. Generar la versión de producción

```bash
npm run build
```

## Mejoras futuras

Funcionalidades identificadas como posibles mejoras para futuras iteraciones:

* Integración con autenticación JWT.
* Gestión de usuarios y roles.
* Persistencia de la terminal seleccionada mediante LocalStorage.
* Dashboard con estadísticas de ventas.
* Tests unitarios con Vitest.
* Paginación de listados.
* Mejoras de accesibilidad.
* Diseño responsive más avanzado.
* Gestión avanzada de errores HTTP diferenciando respuestas 400, 404 y 500.

## Autores

Laura Arias

Daniel Norbert

Rafael Porcel

Proyecto desarrollado como parte de un bootcamp de programación Full Stack.
