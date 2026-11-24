import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { listarOrdenesPaciente } from '../laboratorio/api';
import { darDeAlta, listarInventario, obtenerAdmision, trasladarAdmision } from './api';
import { fechaHora } from './formato';

export default function AdmisionDetallePage() {
    const { id } = useParams();
    const { usuario } = useAuth();

    // Requisito (HU-03 y HU-09): traslada y da de alta el MÉDICO.
    // ADMISIONES y ADMINISTRADOR no ven los botones. Para cambiar quién puede, edita esta lista.
    const ROLES_CLINICOS = ['MEDICO'];
    const puedeGestionar = ROLES_CLINICOS.includes(usuario?.rol);

    const [admision, setAdmision] = useState(null);
    const [inventario, setInventario] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');
    const [avisoOk, setAvisoOk] = useState('');
    const [sedeDestino, setSedeDestino] = useState('');
    const [servicioDestino, setServicioDestino] = useState('');
    const [procesando, setProcesando] = useState(false);
    const [confirmandoAlta, setConfirmandoAlta] = useState(false);
    const [ordenesPendientes, setOrdenesPendientes] = useState([]);

    const cargarDatos = useCallback(async () => {
        try {
            const [adm, inv] = await Promise.all([
                obtenerAdmision(id),
                puedeGestionar ? listarInventario() : Promise.resolve([]),
            ]);
            setAdmision(adm);
            setInventario(Array.isArray(inv) ? inv : []);
        } catch (err) {
            setError(err.status === 404 ? 'Admisión no encontrada.' : err.message);
        } finally {
            setCargando(false);
        }
    }, [id, puedeGestionar]);

    useEffect(() => { cargarDatos(); }, [cargarDatos]);

    const sedes = useMemo(
        () => [...new Set(inventario.map((i) => i.sedeId))].sort((a, b) => a.localeCompare(b, 'es')),
        [inventario],
    );
    const serviciosDeSede = useMemo(
        () => inventario
            .filter((i) => i.sedeId === sedeDestino)
            .map((i) => ({
                servicio: i.servicio,
                libres: i.camasLibres ?? Math.max(0, (i.camasTotales ?? 0) - (i.camasOcupadas ?? 0)),
            }))
            .sort((a, b) => a.servicio.localeCompare(b.servicio, 'es')),
        [inventario, sedeDestino],
    );

    async function ejecutarTraslado(e) {
        e.preventDefault();
        if (procesando || !sedeDestino || !servicioDestino) return;
        setError(''); setAvisoOk(''); setProcesando(true);
        try {
            await trasladarAdmision(id, { sedeDestinoId: sedeDestino, servicioDestino });
            setAvisoOk(`Paciente trasladado a ${sedeDestino} · ${servicioDestino}.`);
            setSedeDestino(''); setServicioDestino('');
            await cargarDatos();
        } catch (err) {
            setError(err.message);
        } finally {
            setProcesando(false);
        }
    }

    async function ejecutarAlta() {
        if (procesando) return;
        setError('');
        setAvisoOk('');
        setOrdenesPendientes([]);
        setProcesando(true);
        try {
            await darDeAlta(id);
            setConfirmandoAlta(false);
            setAvisoOk('Alta registrada. La cama fue liberada.');
            await cargarDatos();
        } catch (err) {
            setConfirmandoAlta(false);
            if (err.status === 409 && /laboratorio/i.test(err.message)) {
                try {
                    const ordenes = await listarOrdenesPaciente(admision.pacienteId);
                    setOrdenesPendientes(ordenes.filter((o) => o.pendiente));
                } catch {
                    setOrdenesPendientes([]);
                }
            }
            setError(err.message);
        } finally {
            setProcesando(false);
        }
    }

    if (cargando) return <p className="subtitulo">Cargando admisión…</p>;

    if (!admision) {
        return (
            <>
                <p><Link to="/admisiones" className="boton-link-oscuro">← Volver a admisiones</Link></p>
                {error && <p className="error" role="alert">{error}</p>}
            </>
        );
    }

    const activa = admision.estado === 'ACTIVA';
    const traslados = admision.traslados ?? [];
    const sedeActual = admision.sedeActualId;
    const servicioActual = admision.servicioActual;
    const ingreso = traslados[0]
        ? `${traslados[0].sedeOrigenId} · ${traslados[0].servicioOrigen}`
        : `${sedeActual} · ${servicioActual}`;

    return (
        <>
            <p><Link to="/admisiones" className="boton-link-oscuro">← Volver a admisiones</Link></p>

            <div className="encabezado">
                <div>
                    <h1>Admisión de {admision.pacienteId}</h1>
                    <p className="mono subtitulo">{admision.id}</p>
                </div>
                <span className={`badge ${activa ? 'badge-ok' : 'badge-neutro'}`}>
                    <span className="badge-punto" />{activa ? 'Activa' : 'Dada de alta'}
                </span>
            </div>

            {usuario?.rol === 'MEDICO' && (
                <p>
                    <Link className="boton-link-oscuro" to={`/paciente/${encodeURIComponent(admision.pacienteId)}`}>
                        Ver estado consolidado →
                    </Link>
                </p>
            )}

            {avisoOk && <p className="aviso-ok" role="status">{avisoOk}</p>}

            <div className="tarjeta">
                <dl className="datos">
                    <dt>Ubicación actual</dt><dd>{sedeActual} · {servicioActual}</dd>
                    <dt>Médico tratante</dt><dd>{admision.medicoTratante || 'Sin especificar'}</dd>
                    <dt>Motivo de ingreso</dt><dd>{admision.motivoIngreso || 'Sin especificar'}</dd>
                    <dt>Cobertura aseguradora</dt>
                    <dd>{admision.coberturaPorcentaje != null ? `${admision.coberturaPorcentaje}%` : 'Sin verificar'}</dd>
                    <dt>Fecha de ingreso</dt><dd>{fechaHora(admision.fechaIngreso)}</dd>
                    <dt>Fecha de alta</dt><dd>{admision.fechaAlta ? fechaHora(admision.fechaAlta) : '—'}</dd>
                    <dt>Traslados</dt><dd>{traslados.length}</dd>
                </dl>
            </div>

            <h2 style={{ marginTop: 28 }}>Historial del paciente</h2>
            <ul className="linea-tiempo">
                <li><strong>Ingreso</strong><span>{ingreso}</span><small>{fechaHora(admision.fechaIngreso)}</small></li>
                {traslados.map((t, i) => (
                    <li key={i}>
                        <strong>Traslado</strong>
                        <span>{t.sedeOrigenId} · {t.servicioOrigen} → {t.sedeDestinoId} · {t.servicioDestino}</span>
                        <small>{fechaHora(t.fecha)}</small>
                    </li>
                ))}
                {admision.fechaAlta && (
                    <li><strong>Alta</strong><span>{sedeActual} · {servicioActual}</span><small>{fechaHora(admision.fechaAlta)}</small></li>
                )}
            </ul>

            {error && <p className="error" role="alert">{error}</p>}

            {!puedeGestionar && activa && (
                <p className="nota">El traslado y el alta médica los realiza el personal médico.</p>
            )}

            {puedeGestionar && activa && (
                <div className="paneles">
                    <section className="tarjeta">
                        <h2>Trasladar paciente</h2>
                        <form onSubmit={ejecutarTraslado} noValidate>
                            <label htmlFor="sede-destino">Sede destino</label>
                            <select id="sede-destino" value={sedeDestino} disabled={procesando}
                                onChange={(e) => { setSedeDestino(e.target.value); setServicioDestino(''); setError(''); }}>
                                <option value="" disabled>Selecciona una sede</option>
                                {sedes.map((s) => <option key={s} value={s}>{s}</option>)}
                            </select>

                            <label htmlFor="servicio-destino">Servicio destino</label>
                            <select id="servicio-destino" value={servicioDestino} disabled={!sedeDestino || procesando}
                                onChange={(e) => { setServicioDestino(e.target.value); setError(''); }}>
                                <option value="" disabled>{sedeDestino ? 'Selecciona un servicio' : 'Elige primero una sede'}</option>
                                {serviciosDeSede.map((s) => {
                                    const esActual = sedeDestino === sedeActual && s.servicio === servicioActual;
                                    return (
                                        <option key={s.servicio} value={s.servicio} disabled={s.libres === 0 || esActual}>
                                            {s.servicio} — {s.libres} {s.libres === 1 ? 'libre' : 'libres'}
                                            {esActual ? ' (ubicación actual)' : ''}
                                        </option>
                                    );
                                })}
                            </select>

                            <div className="acciones">
                                <button type="submit" className="boton" disabled={procesando || !sedeDestino || !servicioDestino}>
                                    {procesando ? 'Trasladando…' : 'Trasladar'}
                                </button>
                            </div>
                        </form>
                    </section>

                    <section className="tarjeta">
                        <h2>Alta médica</h2>
                        <p className="nota">
                            Cierra la admisión y libera la cama. Se rechaza si el paciente tiene órdenes de laboratorio pendientes.
                        </p>
                        {ordenesPendientes.length > 0 && (
                            <div className="pendientes-lab" role="alert">
                                <strong>
                                    Órdenes de laboratorio pendientes ({ordenesPendientes.length})
                                </strong>
                                <ul>
                                    {ordenesPendientes.map((o) => (
                                        <li key={o.id}>
                                            <b>{o.tipoExamen}</b> · {o.estado} · solicitada {fechaHora(o.fechaSolicitud)}
                                            {o.resultado
                                                ? ` · resultado ${o.resultado.valor} ${o.resultado.unidad} (falta cerrar la orden)`
                                                : ' · sin resultado'}
                                        </li>
                                    ))}
                                </ul>
                                <p className="tenue">El laboratorio debe registrar el resultado y cerrar cada orden antes del alta.</p>
                            </div>
                        )}
                        <div className="acciones">
                            {!confirmandoAlta ? (
                                <button type="button" className="boton boton-peligro" disabled={procesando}
                                    onClick={() => setConfirmandoAlta(true)}>
                                    Dar de alta
                                </button>
                            ) : (
                                <>
                                    <button type="button" className="boton boton-peligro" disabled={procesando} onClick={ejecutarAlta}>
                                        {procesando ? 'Procesando…' : 'Confirmar alta'}
                                    </button>
                                    <button type="button" className="boton-secundario" disabled={procesando}
                                        onClick={() => setConfirmandoAlta(false)}>
                                        Cancelar
                                    </button>
                                </>
                            )}
                        </div>
                    </section>
                </div>
            )}
        </>
    );
}
