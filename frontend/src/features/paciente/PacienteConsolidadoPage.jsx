import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import BadgeEstado from '../admisiones/BadgeEstado';
import { fechaHora } from '../admisiones/formato';
import { listarOrdenesPaciente } from '../laboratorio/api';
import BadgeOrden from '../laboratorio/BadgeOrden';
import { resultadoTexto } from '../laboratorio/catalogo';
import { consultarConsolidado } from './api';

const ESTADO_MEDICACION = {
    RECIBIDA: ['Recibida', 'badge-alto'],
    COBERTURA_VERIFICADA: ['Cobertura verificada', 'badge-alto'],
    DESPACHADA: ['Despachada', 'badge-ok'],
};

export default function PacienteConsolidadoPage() {
    const { pacienteId } = useParams();
    const navegar = useNavigate();

    const [busqueda, setBusqueda] = useState(pacienteId ?? '');
    const [version, setVersion] = useState(0);
    const [resultado, setResultado] = useState(null);
    // El laboratorio se consulta directo a su servicio: consultas_admisiones_service
    // todavía no proyecta el topic laboratorio.ordenes.
    const [laboratorio, setLaboratorio] = useState(null);

    useEffect(() => {
        if (!pacienteId) return undefined;
        let vigente = true;
        consultarConsolidado(pacienteId)
            .then((datos) => { if (vigente) setResultado({ pacienteId, datos }); })
            .catch((err) => { if (vigente) setResultado({ pacienteId, error: err.message }); });
        return () => { vigente = false; };
    }, [pacienteId, version]);

    useEffect(() => {
        if (!pacienteId) return undefined;
        let vigente = true;
        listarOrdenesPaciente(pacienteId)
            .then((ordenes) => { if (vigente) setLaboratorio({ pacienteId, ordenes }); })
            .catch((err) => { if (vigente) setLaboratorio({ pacienteId, error: err.message }); });
        return () => { vigente = false; };
    }, [pacienteId, version]);

    function buscar(e) {
        e.preventDefault();
        const id = busqueda.trim();
        if (!id) return;
        if (id === pacienteId) setVersion((v) => v + 1);
        else navegar(`/paciente/${encodeURIComponent(id)}`);
    }

    const actual = resultado?.pacienteId === pacienteId ? resultado : null;
    const cargando = Boolean(pacienteId) && !actual;
    const datos = actual?.datos;
    const admisiones = [...(datos?.admisiones ?? [])]
        .sort((a, b) => (b.fechaIngreso ?? '').localeCompare(a.fechaIngreso ?? ''));
    const medicaciones = [...(datos?.medicaciones ?? [])]
        .sort((a, b) => (b.fechaSolicitud ?? '').localeCompare(a.fechaSolicitud ?? ''));
    const lab = laboratorio?.pacienteId === pacienteId ? laboratorio : null;
    const ordenes = [...(lab?.ordenes ?? [])]
        .sort((a, b) => (b.fechaSolicitud ?? '').localeCompare(a.fechaSolicitud ?? ''));
    const pendientes = ordenes.filter((o) => o.pendiente).length;
    const vacio = datos && admisiones.length === 0 && medicaciones.length === 0 && ordenes.length === 0;

    return (
        <>
            <h1>Paciente consolidado</h1>
            <p className="subtitulo">Admisión, exámenes y medicación desde cualquier sede</p>

            <form className="filtros" onSubmit={buscar}>
                <label htmlFor="paciente">Paciente</label>
                <input
                    id="paciente"
                    placeholder="Documento o ID"
                    value={busqueda}
                    onChange={(e) => setBusqueda(e.target.value)}
                    style={{ width: 260 }}
                />
                <button className="boton" type="submit" disabled={!busqueda.trim()}>Consultar</button>
                {pacienteId && (
                    <button
                        type="button"
                        className="boton-secundario"
                        onClick={() => setVersion((v) => v + 1)}
                    >
                        Actualizar
                    </button>
                )}
            </form>

            {actual?.error && <p className="error" role="alert">{actual.error}</p>}
            {cargando && <p className="subtitulo">Cargando…</p>}
            {vacio && (
                <div className="tarjeta vacio"><p>No hay registros para el paciente «{pacienteId}».</p></div>
            )}

            {datos && !vacio && (
                <>
                    <h2>Admisiones</h2>
                    {admisiones.length === 0 && <p className="nota">Sin admisiones registradas.</p>}
                    {admisiones.map((a) => (
                        <div className="tarjeta bloque" key={a.admisionId}>
                            <div className="encabezado">
                                <strong>Ingreso del {fechaHora(a.fechaIngreso)}</strong>
                                <BadgeEstado estado={a.estado} />
                            </div>
                            <dl className="datos">
                                <dt>Sede actual</dt><dd>{a.sedeActualId}</dd>
                                <dt>Alta</dt><dd>{fechaHora(a.fechaAlta)}</dd>
                                <dt>Traslados</dt><dd>{a.totalTraslados}</dd>
                            </dl>

                            {a.traslados?.length > 0 && (
                                <ol className="linea-tiempo" style={{ marginTop: 16, marginBottom: 0 }}>
                                    {a.traslados.map((t, i) => (
                                        <li key={`${a.admisionId}-${i}`}>
                                            <strong>Traslado</strong>
                                            <span>{t.sedeOrigenId} → {t.sedeDestinoId}</span>
                                            <small>{fechaHora(t.fecha)}</small>
                                        </li>
                                    ))}
                                </ol>
                            )}
                        </div>
                    ))}

                    <h2>Exámenes de laboratorio</h2>
                    {lab?.error && <p className="error" role="alert">{lab.error}</p>}
                    {!lab && <p className="nota">Consultando laboratorio…</p>}
                    {lab && !lab.error && ordenes.length === 0 && (
                        <p className="nota">Sin exámenes solicitados.</p>
                    )}
                    {ordenes.length > 0 && (
                        <>
                            <table className="tabla">
                                <thead>
                                    <tr><th>Examen</th><th>Estado</th><th>Resultado</th><th>Solicitud</th><th /></tr>
                                </thead>
                                <tbody>
                                    {ordenes.map((o) => (
                                        <tr key={o.id}>
                                            <td>{o.tipoExamen}</td>
                                            <td><BadgeOrden orden={o} /></td>
                                            <td>{resultadoTexto(o)}</td>
                                            <td>{fechaHora(o.fechaSolicitud)}</td>
                                            <td><Link to={`/laboratorio/${o.id}`}>Ver</Link></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                            <p className="nota" style={{ marginBottom: 24 }}>
                                {pendientes === 0
                                    ? 'Ningún examen bloquea el alta médica.'
                                    : `${pendientes} ${pendientes === 1 ? 'examen pendiente bloquea' : 'exámenes pendientes bloquean'} el alta médica.`}
                            </p>
                        </>
                    )}

                    <h2>Medicación</h2>
                    {medicaciones.length === 0 && <p className="nota">Sin solicitudes de medicamentos.</p>}
                    {medicaciones.length > 0 && (
                        <table className="tabla">
                            <thead>
                                <tr><th>Medicamento</th><th>Estado</th><th>Cobertura</th><th>Solicitud</th><th>Despacho</th></tr>
                            </thead>
                            <tbody>
                                {medicaciones.map((m) => {
                                    const [texto, clase] = ESTADO_MEDICACION[m.estado] ?? [m.estado, 'badge-neutro'];
                                    return (
                                        <tr key={m.solicitudId}>
                                            <td>{m.medicamento}</td>
                                            <td><span className={`badge ${clase}`}><span className="badge-punto" />{texto}</span></td>
                                            <td>{m.coberturaPorcentaje == null ? '—' : `${m.coberturaPorcentaje}%`}</td>
                                            <td>{fechaHora(m.fechaSolicitud)}</td>
                                            <td>{fechaHora(m.fechaDespacho)}</td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    )}
                    <p className="nota">
                        Los datos se actualizan por eventos y pueden tardar unos segundos en reflejar el último cambio.
                    </p>
                </>
            )}
        </>
    );
}