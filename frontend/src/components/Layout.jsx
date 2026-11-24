import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthContext';

const ENLACES = {
    ADMISIONES: [['/camas', 'Disponibilidad'], ['/admisiones/nueva', 'Nueva admisión'],
    ['/admisiones', 'Admisiones'], ['/capacidad', 'Capacidad'], ['/farmacia', 'Farmacia']],
    MEDICO: [['/admisiones', 'Admisiones'], ['/laboratorio', 'Laboratorio'],
    ['/paciente', 'Paciente consolidado'],
    ['/farmacia/nueva', 'Solicitar medicamento'], ['/farmacia', 'Farmacia']],
    ADMINISTRADOR: [['/reporte', 'Reporte de ocupación'], ['/camas', 'Disponibilidad'],
    ['/admisiones', 'Admisiones'], ['/capacidad', 'Capacidad'], ['/usuarios', 'Usuarios']],
    LABORATORIO: [['/laboratorio', 'Órdenes de laboratorio']],
    FARMACEUTICO: [['/farmacia', 'Bandeja de farmacia']],
};

export default function Layout() {
    const { usuario, logout } = useAuth();
    const navegar = useNavigate();

    function salir() {
        logout();
        navegar('/login', { replace: true });
    }

    return (
        <>
            <header className="barra">
                <div className="marca"><span className="punto" /> RedSalud</div>
                <nav>
                    {(ENLACES[usuario.rol] ?? []).map(([ruta, texto]) => (
                        <NavLink key={ruta} to={ruta} end>{texto}</NavLink>
                    ))}
                </nav>
                <div className="sesion">
                    <span className="rol-badge" title={usuario.email}>
                        <span className="rol-punto" />
                        {usuario.rol}
                    </span>
                    <button type="button" className="boton-cerrar-sesion" onClick={salir}>
                        CERRAR SESIÓN
                    </button>
                </div>
            </header>
            <main className="contenido"><Outlet /></main>
        </>
    );
}