import { useState } from 'react';
import { Link } from 'react-router-dom';
import { registrarIngreso } from './api';
import { useInventario } from './useInventario';
import { libres, sedesDe } from './catalogo';

export default function NuevaAdmisionPage() {
    const { inventario, cargando, error: errorInventario, recargar } = useInventario();
    const [pacienteId, setPacienteId] = useState('');
    const [sedeId, setSedeId] = useState('');
    const [servicio, setServicio] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState('');
    const [creada, setCreada] = useState(null);

    const sedes = sedesDe(inventario);
    const serviciosDeSede = inventario.filter((i) => i.sedeId === sedeId);

    function cambiarSede(valor) {
        setSedeId(valor);
        setServicio('');
    }

    function otraAdmision() {
        setCreada(null);
        setPacienteId('');
        setSedeId('');
        setServicio('');
        setError('');
    }

    async function enviar(e) {
        e.preventDefault();
        if (enviando) return;
        setError('');
        setEnviando(true);
        try {
            const admision = await registrarIngreso({
                pacienteId: pacienteId.trim(),
                sedeId,
                servicio,
            });
            setCreada(admision);
        } catch (err) {
            setError(err.message);
        } finally {
            setEnviando(false);
            recargar(); // las camas libres cambiaron (o cambiaron mientras tanto)
        }
    }

    if (creada) {
        return (
            <>
                <h1>Admisión registrada</h1>
                <p className="subtitulo">El paciente ya está ingresado y la cama fue ocupada.</p>
                <div className="tarjeta exito">
                    <dl>
                        <dt>N.º de admisión</dt><dd className="mono">{creada.id}</dd>
                        <dt>Paciente</dt><dd>{creada.pacienteId}</dd>
                        <dt>Sede</dt><dd>{creada.sedeActualId}</dd>
                        <dt>Servicio</dt><dd>{creada.servicioActual}</dd>
                        <dt>Fecha de ingreso</dt><dd>{new Date(creada.fechaIngreso).toLocaleString()}</dd>
                    </dl>
                    <div className="acciones">
                        <button className="boton" onClick={otraAdmision}>Registrar otra admisión</button>
                        <Link className="boton-secundario" to="/camas">Ver disponibilidad</Link>
                    </div>
                </div>
            </>
        );
    }

    const sinInventario = !cargando && !errorInventario && sedes.length === 0;

    return (
        <>
            <h1>Nueva admisión</h1>
            <p className="subtitulo">Registro de ingreso de paciente</p>

            {errorInventario && <p className="error" role="alert">{errorInventario}</p>}
            {sinInventario && (
                <p className="error" role="alert">
                    No hay camas registradas en ninguna sede, así que no se puede admitir a nadie todavía.
                </p>
            )}

            <form className="formulario" onSubmit={enviar}>
                <label htmlFor="paciente">Paciente (documento o ID)</label>
                <input id="paciente" required maxLength={60} value={pacienteId}
                    onChange={(e) => setPacienteId(e.target.value)} />

                <label htmlFor="sede">Sede</label>
                <select id="sede" required value={sedeId} onChange={(e) => cambiarSede(e.target.value)}>
                    <option value="" disabled>Selecciona una sede</option>
                    {sedes.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>

                <label htmlFor="servicio">Servicio</label>
                <select id="servicio" required value={servicio} disabled={!sedeId}
                    onChange={(e) => setServicio(e.target.value)}>
                    <option value="" disabled>
                        {sedeId ? 'Selecciona un servicio' : 'Elige primero una sede'}
                    </option>
                    {serviciosDeSede.map((i) => (
                        <option key={i.servicio} value={i.servicio} disabled={libres(i) === 0}>
                            {i.servicio} — {libres(i) === 0 ? 'sin camas libres' : `${libres(i)} libres`}
                        </option>
                    ))}
                </select>

                {error && <p className="error" role="alert">{error}</p>}

                <div className="acciones">
                    <button className="boton" type="submit"
                        disabled={enviando || !pacienteId.trim() || !sedeId || !servicio}>
                        {enviando ? 'Registrando…' : 'Registrar'}
                    </button>
                </div>
            </form>
        </>
    );
}