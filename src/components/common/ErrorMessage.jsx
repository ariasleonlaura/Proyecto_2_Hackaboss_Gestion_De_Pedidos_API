import '../../styles/globals.css';

const ErrorMessage = ({ message }) => {
  return (
    <div className="error-container">
      <h3>⚠️ ¡Vaya! Algo ha fallado</h3>
      {/* El operador || funciona como "si message tiene valor úsalo, si no usa el texto por defecto" */}
      <p>{message || "Ha ocurrido un error inesperado al conectar con el servidor."}</p>
    </div>
  );
};

export default ErrorMessage;