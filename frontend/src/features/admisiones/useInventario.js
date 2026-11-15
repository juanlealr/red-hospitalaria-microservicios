import { useCallback, useEffect, useState } from 'react';
import { listarInventario } from './api';

export function useInventario(intervaloMs = 15000) {
    const [inventario, setInventario] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');
    const [actualizado, setActualizado] = useState(null);

    const recargar = useCallback(async () => {
        try {
            const datos = await listarInventario();
            setInventario(datos);
            setError('');
            setActualizado(new Date());
        } catch (err) {
            setError(err.message);
        } finally {
            setCargando(false);
        }
    }, []);

    useEffect(() => {
        recargar();
        if (!intervaloMs) return undefined;
        const id = setInterval(recargar, intervaloMs);
        return () => clearInterval(id);
    }, [recargar, intervaloMs]);

    return { inventario, cargando, error, actualizado, recargar };
}