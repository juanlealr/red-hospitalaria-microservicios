import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fechaHora } from '../admisiones/formato';
import BadgeEstadoFarmacia from './BadgeEstadoFarmacia';
import { despacharSolicitud, listarSolicitudes, verificarCobertura } from './api';

const FILTROS = [
    ['', 'Todas'],
    ['RECIBIDA', 'Recibidas'],
    ['COBERTURA_VERIFICADA', 'Cobertura verificada'],
    ['DESPACHADA', 'Despachadas'],
];

export default function FarmaciaPage() {
    const { usuario } = useAuth();

    // Requisito: el despacho es del FARMACEUTICO. El médico y admisiones
    // pueden ver la bandeja, pero no despachar. Para cambiarlo, edita esta lista.
    const ROLES_DESPACHO = ['FARMACEUTICO'];
    const puedeDespachar = ROLES_DESPACHO.includes(usuario?.rol);

    const [solicitudes, setSolicitudes] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');
    const [avisoOk, setAvisoOk] = useState('');
    const [estado, setEstado] = useState('');
    const [busqueda, setBusqueda] = useState('');
    const [procesando, setProcesando] = useState('');
    const [confirmando, setConfirmando] = useState('');

    const cargar = useCallback(async () => {
        try {
            const datos = await listarSolicitudes();
            setSolicitudes(Array.isArray(datos) ? datos : []);
            setError('');
        } catch (err) {
            setError(err.message);
        } finally {
            setCargando(false);
        }
    }, []);

    useEffect(() => { cargar(); }, [cargar]);

    const texto = busqueda.trim().toLowerCase();
    const filas = useMemo(
        () => solicitudes
            .filter((s) => (!estado || s.estado === estado)
                && (!texto
                    || (s.pacienteId ?? '').toLowerCase().includes(texto)
                    || (s.medicamento ?? '').toLowerCase().includes(texto)))
            .sort((a, b) => (b.fechaSolicitud ?? '').localeCompare(a.fechaSolicitud ?? '')),
        [solicitudes, estado, texto],
    );

    const pendientes = solicitudes.filter((s) => s.estado !== 'DESPACHADA').length;

    async function ejecutar(id, accion, mensajeOk) {
        if (procesando) return;
        setError('');
        setAvisoOk('');
        setProcesando(id);
        setConfirmando('');
        try {
            await accion(id);
            setAvisoOk(mensajeOk);
            await cargar();
        } catch (err) {
            setError(err.message);
        } finally {
            setProcesando('');
        }
    }

    return (
        <>
            <div className="encabezado">
                <div>
                    <h1>Farmacia</h1>
                    <p className="subtitulo">Solicitudes y despacho de medicamentos</p>
                </div>
            </div>

            <div className="filtros">
                <label htmlFor="buscar-farmacia">Paciente o medicamento</label>
                <input
                    id="buscar-farmacia"
                    placeholder="Buscar por documento, ID o medicamento"
                    value={busqueda}
                    onChange={(e) => setBusqueda(e.target.value)}
                    style={{ width: 300 }}
                />
                <label htmlFor="estado-farmacia">Estado</label>
                <select id="estado-farmacia" value={estado} onChange={(e) => setEstado(e.target.value)}>
                    {FILTROS.map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
                </select>
                <button type="button" className="boton-secundario" onClick={cargar}>Actualizar</button>
            </div>

            <p className="nota">
                {pendientes === 0
                    ? 'No hay solicitudes pendientes de despacho.'
                    : `${pendientes} ${pendientes === 1 ? 'solicitud pendiente' : 'solicitudes pendientes'} de despacho.`}
            </p>

            {avisoOk && <p className="aviso-ok" role="status">{avisoOk}</p>}
            {error && <p className="error" role="alert">{error}</p>}
            {cargando && <p className="subtitulo">Cargando…</p>}
            {!cargando && !error && filas.length === 0 && (
                <div className="tarjeta vacio"><p>No hay solicitudes que coincidan.</p></div>
            )}

            {filas.length > 0 && (
                <table className="tabla">
                    <thead>
                        <tr>
                            <th>Medicamento</th><th>Paciente</th><th>Estado</th>
                            <th>Cobertura</th><th>Solicitud</th><th>Despacho</th><th />
                        </tr>
                    </thead>
                    <tbody>
                        {filas.map((s) => {
                            const ocupada = procesando === s.id;
                            return (
                                <tr key={s.id}>
                                    <td>
                                        {s.medicamento}
                                        <div className="mono tenue">Médico: {s.medicoId}</div>
                                    </td>
                                    <td>{s.pacienteId}</td>
                                    <td><BadgeEstadoFarmacia estado={s.estado} /></td>
                                    <td>{s.cobertura == null ? '—' : `${s.cobertura}%`}</td>
                                    <td>{fechaHora(s.fechaSolicitud)}</td>
                                    <td>{fechaHora(s.fechaDespacho)}</td>
                                    <td>
                                        {s.estado === 'RECIBIDA' && puedeDespachar && (
                                            <button
                                                type="button"
                                                className="boton-secundario"
                                                disabled={ocupada}
                                                onClick={() => ejecutar(
                                                    s.id,
                                                    verificarCobertura,
                                                    'Cobertura verificada. La solicitud ya puede despacharse.',
                                                )}
                                            >
                                                {ocupada ? 'Verificando…' : 'Verificar cobertura'}
                                            </button>
                                        )}

                                        {s.estado === 'COBERTURA_VERIFICADA' && puedeDespachar && (
                                            confirmando === s.id ? (
                                                <>
                                                    <button
                                                        type="button"
                                                        className="boton"
                                                        disabled={ocupada}
                                                        onClick={() => ejecutar(
                                                            s.id,
                                                            despacharSolicitud,
                                                            'Medicamento despachado.',
                                                        )}
                                                    >
                                                        {ocupada ? 'Despachando…' : 'Confirmar'}
                                                    </button>
                                                    {' '}
                                                    <button
                                                        type="button"
                                                        className="boton-secundario"
                                                        disabled={ocupada}
                                                        onClick={() => setConfirmando('')}
                                                    >
                                                        Cancelar
                                                    </button>
                                                </>
                                            ) : (
                                                <button
                                                    type="button"
                                                    className="boton"
                                                    onClick={() => setConfirmando(s.id)}
                                                >
                                                    Despachar
                                                </button>
                                            )
                                        )}

                                        {!puedeDespachar && s.estado !== 'DESPACHADA' && (
                                            <span className="tenue">Solo farmacia</span>
                                        )}
                                        {s.estado === 'DESPACHADA' && <span className="tenue">Completada</span>}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            )}
        </>
    );
}
