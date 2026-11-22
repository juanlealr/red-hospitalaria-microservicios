import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { listarAdmisiones, registrarIngreso, verificarCoberturaAdmision } from './api';
import { useInventario } from './useInventario';
import { libres, sedesDe } from './catalogo';
import { fechaHora } from './formato';

export default function NuevaAdmisionPage() {
    const { inventario, cargando, error: errorInventario, recargar } = useInventario();

    const [modo, setModo] = useState('nuevo'); // 'nuevo' | 'existente'
    const [busqueda, setBusqueda] = useState('');
    const [pacienteElegido, setPacienteElegido] = useState('');
    const [admisiones, setAdmisiones] = useState([]);

    const [sedeId, setSedeId] = useState('');
    const [servicio, setServicio] = useState('');
    const [medicoTratante, setMedicoTratante] = useState('');
    const [motivoIngreso, setMotivoIngreso] = useState('');
    const [consultandoCob, setConsultandoCob] = useState(false);
    const [errorCob, setErrorCob] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState('');
    const [creada, setCreada] = useState(null);

    const cargarPacientes = useCallback(() => {
        listarAdmisiones().then(setAdmisiones).catch(() => setAdmisiones([]));
    }, []);

    useEffect(() => { cargarPacientes(); }, [cargarPacientes]);

    // Pacientes conocidos: uno por ID, con su última actividad y si hoy está admitido.
    const pacientes = useMemo(() => {
        const mapa = new Map();
        admisiones.forEach((a) => {
            const p = mapa.get(a.pacienteId)
                ?? { pacienteId: a.pacienteId, activa: false, ultima: null, sede: '', servicio: '' };
            if (a.estado === 'ACTIVA') p.activa = true;
            const fecha = a.fechaAlta ?? a.fechaIngreso;
            if (!p.ultima || fecha > p.ultima) {
                p.ultima = fecha;
                p.sede = a.sedeActualId;
                p.servicio = a.servicioActual;
            }
            mapa.set(a.pacienteId, p);
        });
        return [...mapa.values()].sort((x, y) => (y.ultima ?? '').localeCompare(x.ultima ?? ''));
    }, [admisiones]);

    const texto = busqueda.trim().toLowerCase();
    const coincidencias = pacientes
        .filter((p) => !texto || p.pacienteId.toLowerCase().includes(texto))
        .slice(0, 6);

    const sedes = sedesDe(inventario);
    const serviciosDeSede = inventario.filter((i) => i.sedeId === sedeId);

    function cambiarSede(valor) {
        setSedeId(valor);
        setServicio('');
    }

    function cambiarModo(nuevoModo) {
        setModo(nuevoModo);
        setPacienteElegido('');
        setBusqueda('');
        setError('');
    }

    function otraAdmision() {
        setCreada(null);
        setErrorCob('');
        setError('');
    }

    async function verificarCobertura(admisionId) {
        setConsultandoCob(true);
        setErrorCob('');
        try {
            setCreada(await verificarCoberturaAdmision(admisionId));
        } catch (err) {
            setErrorCob(`No se pudo verificar la cobertura: ${err.message}`);
        } finally {
            setConsultandoCob(false);
        }
    }

    async function enviar(e) {
        e.preventDefault();
        if (enviando) return;
        setError('');
        setErrorCob('');
        setEnviando(true);
        try {
            const admision = await registrarIngreso({
                pacienteId: modo === 'existente' ? pacienteElegido : '',
                sedeId,
                servicio,
                medicoTratante: medicoTratante.trim(),
                motivoIngreso: motivoIngreso.trim(),
            });
            setModo('nuevo');
            setPacienteElegido('');
            setBusqueda('');
            setSedeId('');
            setServicio('');
            setMedicoTratante('');
            setMotivoIngreso('');
            setCreada(admision);
            verificarCobertura(admision.id);
        } catch (err) {
            setError(err.message);
        } finally {
            setEnviando(false);
            recargar();
            cargarPacientes();
        }
    }

    if (creada) {
        return (
            <>
                <h1>Admisión registrada</h1>
                <p className="subtitulo">El paciente ya está ingresado y la cama fue ocupada.</p>
                <div className="tarjeta exito">
                    <dl>
                        <dt>ID del paciente</dt>
                        <dd className="id-asignado">{creada.pacienteId}</dd>
                        <dt>N.º de admisión</dt><dd className="mono">{creada.id}</dd>
                        <dt>Sede</dt><dd>{creada.sedeActualId}</dd>
                        <dt>Servicio</dt><dd>{creada.servicioActual}</dd>
                        <dt>Médico tratante</dt><dd>{creada.medicoTratante}</dd>
                        <dt>Motivo de ingreso</dt><dd>{creada.motivoIngreso}</dd>
                        <dt>Cobertura aseguradora</dt>
                        <dd>
                            {consultandoCob
                                ? 'Verificando…'
                                : creada.coberturaPorcentaje == null
                                    ? 'Sin verificar'
                                    : `${creada.coberturaPorcentaje}%`}
                        </dd>
                        <dt>Fecha de ingreso</dt><dd>{fechaHora(creada.fechaIngreso)}</dd>
                    </dl>
                    {errorCob && <p className="error" role="alert">{errorCob}</p>}
                    <div className="acciones">
                        {creada.coberturaPorcentaje == null && !consultandoCob && (
                            <button
                                className="boton-secundario"
                                type="button"
                                onClick={() => verificarCobertura(creada.id)}
                            >
                                Verificar cobertura
                            </button>
                        )}
                        <button className="boton" type="button" onClick={otraAdmision}>
                            Registrar otra admisión
                        </button>
                        <Link className="boton-secundario" to="/camas">Ver disponibilidad</Link>
                    </div>
                </div>
            </>
        );
    }

    const sinInventario = !cargando && !errorInventario && sedes.length === 0;
    const formularioIncompleto =
        (modo === 'existente' && !pacienteElegido)
        || !sedeId || !servicio || !medicoTratante.trim() || !motivoIngreso.trim();

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
                <label>Paciente</label>
                <div className="modo-paciente">
                    <button
                        type="button"
                        aria-pressed={modo === 'nuevo'}
                        onClick={() => cambiarModo('nuevo')}
                        disabled={enviando}
                    >
                        Paciente nuevo
                    </button>
                    <button
                        type="button"
                        aria-pressed={modo === 'existente'}
                        onClick={() => cambiarModo('existente')}
                        disabled={enviando}
                    >
                        Paciente existente (reingreso)
                    </button>
                </div>

                {modo === 'nuevo' && (
                    <p className="nota">
                        El sistema le asignará un ID nuevo al registrar (PAC-1002, PAC-1003…).
                    </p>
                )}

                {modo === 'existente' && !pacienteElegido && (
                    <>
                        <input
                            type="text"
                            value={busqueda}
                            onChange={(e) => setBusqueda(e.target.value)}
                            placeholder="Busca por ID, por ejemplo PAC-1001"
                            aria-label="Buscar paciente"
                            autoFocus
                        />
                        {coincidencias.length === 0 ? (
                            <p className="nota">
                                {pacientes.length === 0
                                    ? 'Aún no hay pacientes registrados.'
                                    : 'Ningún paciente coincide con la búsqueda.'}
                            </p>
                        ) : (
                            <ul className="lista-pacientes">
                                {coincidencias.map((p) => (
                                    <li key={p.pacienteId}>
                                        <button
                                            type="button"
                                            disabled={p.activa}
                                            onClick={() => setPacienteElegido(p.pacienteId)}
                                        >
                                            <strong>{p.pacienteId}</strong>
                                            <span className="tenue">
                                                {p.activa
                                                    ? `Ya está admitido en ${p.sede} · ${p.servicio}`
                                                    : `Última atención: ${p.sede} · ${fechaHora(p.ultima)}`}
                                            </span>
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </>
                )}

                {modo === 'existente' && pacienteElegido && (
                    <div className="paciente-elegido">
                        <span><strong>{pacienteElegido}</strong> · reingreso</span>
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

                <label htmlFor="sede">Sede</label>
                <select
                    id="sede"
                    value={sedeId}
                    onChange={(e) => cambiarSede(e.target.value)}
                    disabled={cargando || enviando || sinInventario}
                    required
                >
                    <option value="" disabled>Selecciona una sede</option>
                    {sedes.map((s) => (
                        <option key={s} value={s}>{s}</option>
                    ))}
                </select>

                <label htmlFor="servicio">Servicio</label>
                <select
                    id="servicio"
                    value={servicio}
                    onChange={(e) => setServicio(e.target.value)}
                    disabled={!sedeId || enviando}
                    required
                >
                    <option value="" disabled>
                        {sedeId ? 'Selecciona un servicio' : 'Elige primero una sede'}
                    </option>
                    {serviciosDeSede.map((item) => {
                        const disponibles = libres(item);
                        return (
                            <option key={item.servicio} value={item.servicio} disabled={disponibles === 0}>
                                {item.servicio} — {disponibles} {disponibles === 1 ? 'libre' : 'libres'}
                            </option>
                        );
                    })}
                </select>

                <label htmlFor="medico">Médico tratante</label>
                <input
                    id="medico"
                    type="text"
                    value={medicoTratante}
                    onChange={(e) => setMedicoTratante(e.target.value)}
                    disabled={enviando}
                    required
                />

                <label htmlFor="motivo">Motivo de ingreso</label>
                <textarea
                    id="motivo"
                    value={motivoIngreso}
                    onChange={(e) => setMotivoIngreso(e.target.value)}
                    disabled={enviando}
                    required
                />

                {error && <p className="error" role="alert">{error}</p>}

                <div className="acciones">
                    <button
                        type="submit"
                        className="boton"
                        disabled={enviando || formularioIncompleto || sinInventario}
                    >
                        {enviando ? 'Registrando…' : 'Registrar admisión'}
                    </button>
                    <Link className="boton-secundario" to="/admisiones">Cancelar</Link>
                </div>
            </form>
        </>
    );
}