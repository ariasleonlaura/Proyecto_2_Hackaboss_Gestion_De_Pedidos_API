import PedidoCard from "./PedidoCard";

export default function ListaPedidos({ titulo, pedidos, onAvanzar, colorClass }) {
  return (
    <section className={`cocina-columna ${colorClass}`}>
      <h2 className="cocina-columna__titulo">
        {titulo}
        <span className="cocina-columna__count">{pedidos.length}</span>
      </h2>
      {pedidos.length === 0 ? (
        <p className="cocina-columna__vacia">Sin pedidos ahora mismo</p>
      ) : (
        pedidos.map((p) => (
          <PedidoCard key={p.id} pedido={p} onAvanzar={onAvanzar} />
        ))
      )}
    </section>
  );
}