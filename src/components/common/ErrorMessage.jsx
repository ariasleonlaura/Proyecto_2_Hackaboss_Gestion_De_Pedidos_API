const ErrorMessage = ({ message }) => {
  return (
    <div className="error-container" style={{ padding: '1rem', color: 'red', textAlign: 'center' }}>
      <h3>⚠️ ¡Vaya! Algo ha fallado</h3>
      <p>{message || "Ha ocurrido un error inesperado al conectar con el servidor."}</p>
    </div>
  );
};

export default ErrorMessage;