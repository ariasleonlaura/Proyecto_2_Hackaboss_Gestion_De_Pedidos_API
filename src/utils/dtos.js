// Estos objetos documentan la forma exacta de los datos que devuelve el backend.
// No se usan directamente, son solo referencia para saber qué campos existen sin tener que abrir el backend.

export const ProductoDto = {
  id: null,
  nombre: "",
  precio: 0,
  activo: true,
  nombreCategoria: "",
};

export const PedidoDto = {
  id: null,
  codigo: "",
  fechaCreacion: "",
  estado: "",
  total: 0,
  terminalId: null,
  productos: [], // array de ProductosPedidoDto
};

export const ProductosPedidoDto = {
  productoId: null,
  nombreProducto: "",
  cantidad: 0,
  precioUnitario: 0,
  subtotal: 0,
};

export const TerminalDto = {
  id: null,
  nombre: "",
};

export const CategoriaDto = {
  id: null,
  nombre: "",
};