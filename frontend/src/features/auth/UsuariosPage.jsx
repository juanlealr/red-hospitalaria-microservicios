import { useState } from 'react';
import { crearUsuario } from './api';

const ROLES = [
    ['ADMISIONES', 'Personal de admisiones'],
    ['MEDICO', 'Médico'],
    ['LABORATORIO', 'Personal de laboratorio'],
    ['FARMACEUTICO', 'Farmacéutico'],
    ['ADMINISTRADOR', 'Administrador'],
];

function generarClave() {
    const alfabeto = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
    const bytes = crypto.getRandomValues(new Uint32Array(12));
    return Array.from(bytes, (b) => alfabeto[b % alfabeto.length]).join('');
}

export default function UsuariosPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [rol, setRol] = useState('');
    const [verClave, setVerClave] = useState(false);
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState('');
    const [ultimo, setUltimo] = useState(null);
    const [creados, setCreados] = useState([]);

    async function enviar(e) {
        e.preventDefault();
        if (enviando) return;
        setError('');
        setEnviando(true);
        try {
            const usuario = await crearUsuario({ email: email.trim(), password, rol });
            setUltimo({ ...usuario, claveTemporal: password });
            setCreados((lista) => [usuario, ...lista]);
            setEmail('');
            setPassword('');
            setRol('');
            setVerClave(false);
        } catch (err) {
            setError(err.message);
        } finally {
            setEnviando(false);
        }
    }

    return (
        <>
            <h1>Usuarios</h1>
            <p className="subtitulo">Crea cuentas para el personal. No existe registro público.</p>

            {ultimo && (
                <div className="tarjeta exito" style={{ marginBottom: 24 }}>
                    <strong>Usuario creado</strong>
                    <dl>
                        <dt>Correo</dt><dd>{ultimo.email}</dd>
                        <dt>Rol</dt><dd>{ultimo.rol}</dd>
                        <dt>Clave temporal</dt><dd className="mono">{ultimo.claveTemporal}</dd>
                    </dl>
                    <p className="nota">Entrégale la clave por un canal seguro. No se vuelve a mostrar.</p>
                </div>
            )}

            <form className="formulario" onSubmit={enviar}>
                <label htmlFor="u-email">Correo</label>
                <input id="u-email" type="email" required value={email}
                    onChange={(e) => setEmail(e.target.value)} />

                <label htmlFor="u-rol">Rol</label>
                <select id="u-rol" required value={rol} onChange={(e) => setRol(e.target.value)}>
                    <option value="" disabled>Selecciona un rol</option>
                    {ROLES.map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
                </select>

                <label htmlFor="u-clave">Clave (mínimo 8 caracteres)</label>
                <input id="u-clave" type={verClave ? 'text' : 'password'} required minLength={8}
                    autoComplete="new-password" value={password}
                    onChange={(e) => setPassword(e.target.value)} />
                <div className="acciones" style={{ marginTop: 8 }}>
                    <button type="button" className="boton-secundario"
                        onClick={() => { setPassword(generarClave()); setVerClave(true); }}>Generar clave</button>
                    <button type="button" className="boton-secundario"
                        onClick={() => setVerClave((v) => !v)}>{verClave ? 'Ocultar' : 'Mostrar'}</button>
                </div>

                {error && <p className="error" role="alert">{error}</p>}

                <div className="acciones">
                    <button className="boton" type="submit" disabled={enviando || !rol}>
                        {enviando ? 'Creando…' : 'Crear usuario'}
                    </button>
                </div>
            </form>

            {creados.length > 0 && (
                <>
                    <h2 style={{ marginTop: 40 }}>Creados en esta sesión</h2>
                    <table className="tabla">
                        <thead><tr><th>Correo</th><th>Rol</th></tr></thead>
                        <tbody>
                            {creados.map((u) => <tr key={u.id}><td>{u.email}</td><td>{u.rol}</td></tr>)}
                        </tbody>
                    </table>
                </>
            )}
        </>
    );
}