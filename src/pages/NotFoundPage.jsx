import { Link } from 'react-router-dom';

const NotFoundPage = () => {
  return (
    <div className="not-found-container">
      <h1>404 - Página no encontrada</h1>
      <p>¡Ups! Parece que te has perdido por la cocina.</p>
      <p>La ruta que intentas buscar no existe en nuestro sistema.</p>
      
      <Link to="/" className="btn-volver">
        Volver a la Terminal
      </Link>
    </div>
  );
};

export default NotFoundPage;