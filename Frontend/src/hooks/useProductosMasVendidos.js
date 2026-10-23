import { useState, useEffect } from "react";
import { obtenerProductosMasVendidos } from "../api/estadisticasApi";

export function useProductosMasVendidos() {
  const [datos, setDatos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    obtenerProductosMasVendidos()
      .then((data) => {
        setDatos(data);
        setLoading(false);
      })
      .catch(() => {
        setDatos([]);
        setLoading(false);
        setError("No se pudieron cargar las estadísticas");
      });
  }, []);

  return { datos, loading, error };
}
