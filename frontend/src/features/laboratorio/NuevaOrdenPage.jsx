import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { listarAdmisiones } from '../admisiones/api';
import { fechaHora } from '../admisiones/formato';
import { solicitarExamen } from './api';
import { EXAMENES_FRECUENTES } from './catalogo';

/**
 * HU-04: el médico solicita un examen de laboratorio.
 * El paciente puede elegirse de las admisiones activas o escribirse a mano
 * (por ejemplo para un paciente ambulatorio que no tiene admisión abierta).
 */
export default function NuevaOrdenPage() {
    const [parametros] = useSearchParams();
    const pacienteSugerido = parametros.get('paciente') ?? '';

    const [modo, setModo] = useState(pacienteSugerido ? 'manual' : 'admitido');
    const [admisiones, setAdmisiones] = useState([]);
    const [errorAdmisiones, setErrorAdmisiones] = useState('');
    const [busqueda, setBusqueda] = useState('');
    const [pacienteElegido, setPacienteElegido] = useState(pacienteSugerido);
    const [pacienteManual, setPacienteManual] = useState(pacienteSugerido);

    const [tipoExamen, setTipoExamen] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState('');
    const [creada, setCreada] = useState(null);

    useEffect(() => {
        listarAdmisiones()
            .then(setAdmisiones)
            .catch((err) => setErrorAdmisiones(err.message));
    }, []);

    // Un paciente por ID, con la sede donde está hoy.
    const pacientes = useMemo(() => {
        const mapa = new Map();
        admisiones.forEach((a) => {
            const previo = mapa.get(a.pacienteId);
            const fecha = a.fechaAlta ?? a.fechaIngreso;
            const activa = a.estado === 'ACTIVA';
            if (!previo || activa || fecha > (previo.ultima ?? '')) {
                mapa.set(a.pacienteId, {
                    pacienteId: a.pacienteId,
                    activa: activa || previo?.activa === true,
                    ultima: fecha,
                    sede: a.sedeActualId,
                    servicio: a.servicioActual,
                });
            }
        });
        return [...mapa.values()]
            .sort((x, y) => Number(y.activa) - Number(x.activa)
                || (y.ultima ?? '').localeCompare(x.ultima ?? ''));
    }, [admisiones]);

    const texto = busqueda.trim().toLowerCase();
    const coincidencias = pacientes
        .filter((p) => !texto || p.pacienteId.toLowerCase().includes(texto))
        .slice(0, 6);

    const pacienteId = modo === 'manual' ? pacienteManual.trim() : pacienteElegido;
    const examen = tipoExamen.trim();
    const incompleto = !pacienteId || !examen;

    function cambiarModo(nuevo) {
        setModo(nuevo);
        setError('');
    }

    function otraOrden() {
        setCreada(null);
        setTipoExamen('');
        setError('');
    }

    async function enviar(e) {
        e.preventDefault();
        if (enviando || incompleto) return;
        setError('');
        setEnviando(true);
        try {
            setCreada(await solicitarExamen({ pacienteId, tipoExamen: examen }));
        } catch (err) {
            setError(err.status === 403
                ? 'Tu rol no puede solicitar exámenes. Solo el médico puede hacerlo.'
                : err.message);
        } finally {
            setEnviando(false);
        }
    }

    if (creada) {
        return (
            <>
                <h1>Examen solicitado</h1>
                <p className="subtitulo">La orden quedó registrada y el laboratorio ya puede verla.</p>

                <div className="tarjeta exito">
                    <dl>
                        <dt>Paciente</dt><dd className="id-asignado">{creada.pacienteId}</dd>
                        <dt>Examen</dt><dd>{creada.tipoExamen}</dd>
                        <dt>N.º de orden</dt><dd className="mono">{creada.id}</dd>
                        <dt>Estado</dt><dd>Solicitada</dd>
                        <dt>Fecha de solicitud</dt><dd>{fechaHora(creada.fechaSolicitud)}</dd>
                    </dl>
                    <p className="nota">
                        Mientras la orden no se cierre, el alta médica de este paciente será rechazada.
                    </p>
                    <div className="acciones">
                        <button className="boton" type="button" onClick={otraOrden}>
                            Solicitar otro examen
                        </button>
                        <Link className="boton-secundario" to={`/laboratorio/${creada.id}`}>Ver orden</Link>
                        <Link className="boton-secundario" to="/laboratorio">Ver todas</Link>
                    </div>
                </div>
            </>
        );
    }

    return (
        <>
            <h1>Solicitar examen</h1>
            <p className="subtitulo">Orden de laboratorio para un paciente</p>

            {errorAdmisiones && (
                <p className="nota">
                    No se pudo cargar la lista de pacientes admitidos ({errorAdmisiones}).
                    Puedes escribir el ID del paciente a mano.
                </p>
            )}

            <form className="formulario" onSubmit={enviar}>
                <label>Paciente</label>
                <div className="modo-paciente">
                    <button
                        type="button"
                        aria-pressed={modo === 'admitido'}
                        onClick={() => cambiarModo('admitido')}
                        disabled={enviando}
                    >
                        Paciente admitido
                    </button>
                    <button
                        type="button"
                        aria-pressed={modo === 'manual'}
                        onClick={() => cambiarModo('manual')}
                        disabled={enviando}
                    >
                        Escribir ID
                    </button>
                </div>

                {modo === 'admitido' && !pacienteElegido && (
                    <>
                        <input
                            type="text"
                            value={busqueda}
                            onChange={(e) => setBusqueda(e.target.value)}
                            placeholder="Busca por ID, por ejemplo PAC-1001"
                            aria-label="Buscar paciente"
                        />
                        {coincidencias.length === 0 ? (
                            <p className="nota">
                                {pacientes.length === 0
                                    ? 'Aún no hay pacientes con admisiones registradas.'
                                    : 'Ningún paciente coincide con la búsqueda.'}
                            </p>
                        ) : (
                            <ul className="lista-pacientes">
                                {coincidencias.map((p) => (
                                    <li key={p.pacienteId}>
                                        <button type="button" onClick={() => setPacienteElegido(p.pacienteId)}>
                                            <strong>{p.pacienteId}</strong>
                                            <span className="tenue">
                                                {p.activa
                                                    ? `Admitido en ${p.sede} · ${p.servicio}`
                                                    : `Última atención: ${p.sede} · ${fechaHora(p.ultima)}`}
                                            </span>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </>
                )}

                {modo === 'admitido' && pacienteElegido && (
                    <div className="paciente-elegido">
                        <span><strong>{pacienteElegido}</strong></span>
                        <button
                            type="button"
                            className="boton-link"
                            onClick={() => setPacienteElegido('')}
                            disabled={enviando}
                        >
                            Cambiar
                        </button>
                    </div>
                )}

                {modo === 'manual' && (
                    <input
                        type="text"
                        value={pacienteManual}
                        onChange={(e) => setPacienteManual(e.target.value)}
                        placeholder="PAC-1001"
                        aria-label="ID del paciente"
                        maxLength={255}
                        disabled={enviando}
                    />
                )}

                <label htmlFor="examen">Tipo de examen</label>
                <input
                    id="examen"
                    list="examenes-frecuentes"
                    value={tipoExamen}
                    onChange={(e) => setTipoExamen(e.target.value)}
                    placeholder="Hemograma"
                    maxLength={150}
                    disabled={enviando}
                    required
                />
                <datalist id="examenes-frecuentes">
                    {EXAMENES_FRECUENTES.map((e) => <option key={e} value={e} />)}
                </datalist>
                <p className="nota">
                    Puedes elegir uno de la lista o escribir cualquier otro examen.
                </p>

                {error && <p className="error" role="alert">{error}</p>}

                <div className="acciones">
                    <button type="submit" className="boton" disabled={enviando || incompleto}>
                        {enviando ? 'Solicitando…' : 'Solicitar examen'}
                    </button>
                    <Link className="boton-secundario" to="/laboratorio">Cancelar</Link>
                </div>
            </form>
        </>
    );
}
