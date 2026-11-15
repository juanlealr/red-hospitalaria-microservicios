import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

export default function RutaProtegida({ roles, children }) {
    const { usuario } = useAuth();
    const ubicacion = useLocation();

    if (!usuario) return <Navigate to="/login" replace state={{ desde: ubicacion }} />;
    if (roles && !roles.includes(usuario.rol)) return <Navigate to="/sin-permiso" replace />;
    return children;
}