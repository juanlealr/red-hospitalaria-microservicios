import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { rutaInicial, useAuth } from './AuthContext';

export default function LoginPage() {
    const { usuario, login } = useAuth();
    const navegar = useNavigate();
    const ubicacion = useLocation();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [cargando, setCargando] = useState(false);

    if (usuario) return <Navigate to={rutaInicial(usuario.rol)} replace />;

    async function enviar(e) {
        e.preventDefault();
        setError('');
        setCargando(true);
        try {
            const u = await login(email.trim(), password);
            navegar(ubicacion.state?.desde?.pathname ?? rutaInicial(u.rol), { replace: true });
        } catch (err) {
            setError(err.message);
        } finally {
            setCargando(false);
        }
    }

    return (
        <div className="login-fondo">
            <form className="tarjeta login-caja" onSubmit={enviar}>
                <div className="marca marca-grande"><span className="punto" /> RedSalud</div>
                <h1>Iniciar sesión</h1>
                <p className="subtitulo">Red hospitalaria multi-sede</p>

                <label htmlFor="email">Correo</label>
                <input id="email" type="email" required autoComplete="username"
                    value={email} onChange={(e) => setEmail(e.target.value)} />

                <label htmlFor="password">Contraseña</label>
                <input id="password" type="password" required autoComplete="current-password"
                    value={password} onChange={(e) => setPassword(e.target.value)} />

                {error && <p className="error" role="alert">{error}</p>}

                <button className="boton" type="submit" disabled={cargando}>
                    {cargando ? 'Ingresando…' : 'Ingresar'}
                </button>
                <p className="nota">Las cuentas las crea un administrador. No hay registro público.</p>
            </form>
        </div>
    );
}