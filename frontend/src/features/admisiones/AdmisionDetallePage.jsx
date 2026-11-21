import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { darDeAlta, obtenerAdmision, trasladarAdmision, verificarCoberturaAdmision } from './api';
import BadgeEstado from './BadgeEstado';
import { libres, sedesDe } from './catalogo';
import { fechaHora } from './formato';
import { useInventario } from './useInventario';

function construirLinea(a) {
    const primero = a.traslados[0];
    const eventos = [{
        clave: 'ingreso',
        titulo: 'Ingreso',
        detalle: `${primero ? primero.sedeOrigenId : a.sedeActualId} · ${primero ? primero.servicioOrigen ?? '—' : a.servicioActual}`,
        fecha: a.fechaIngreso,
    }];
    a.traslados.forEach((t, i) => eventos.push({
        clave: `traslado-${i}`,
        titulo: 'Traslado',
        detalle: `${t.sedeOrigenId} · ${t.servicioOrigen ?? '—'}  →  ${t.sedeDestinoId} · ${t.servicioDestino ?? '—'}`,
        fecha: t.fecha,
    }));
    if (a.fechaAlta) {
        eventos.push({ clave: 'alta', titulo: 'Alta médica', detalle: a.sedeActualId, fecha: a.fechaAlta });
    }
    return eventos;
}

export default function AdmisionDetallePage() {
    const { id } = useParams();
    const { usuario } = useAuth();
    const { inventario, recargar: recargarInventario } = useInventario();

    const [admision, setAdmision] = useState(null);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');
    const [sedeDestino, setSedeDestino] = useState('');
    const [servicioDestino, setServicioDestino] = useState('');
    const [confirmandoAlta, setConfirmandoAlta] = useState(false);
    const [ocupado, setOcupado] = useState(false);
    const [errorAccion, setErrorAccion] = useState('');
    const [aviso, setAviso] = useState('');

    const cargar = useCallback(async () => {
        try {
            setAdmision(await obtenerAdmision(id));
            setError('');
        } catch (err) {
            setError(err.message);
        } finally {
            setCargando(false);
        }
    }, [id]);

    useEffect(() => { cargar(); }, [cargar]);

    async function ejecutar(accion, mensajeOk) {
        if (ocupado) return;
        setOcupado(true);
        setErrorAccion('');
        setAviso('');
        try {
            await accion();
            setAviso(mensajeOk);
            await cargar();
        } catch (err) {
            setErrorAccion(err.message);
        } finally {
            setOcupado(false);
            recargarInventario();
        }
    }

    function verificarCobertura() {
        ejecutar(
            () => verificarCoberturaAdmision(id),
            'Cobertura verificada con la aseguradora externa y asociada a la admisión.',
        );
    }

    function trasladar(e) {
        e.preventDefault();
        ejecutar(async () => {
            await trasladarAdmision(id, { sedeDestinoId: sedeDestino, servicioDestino });
            setSedeDestino('');
            setServicioDestino('');
        }, 'Traslado registrado correctamente.');
    }

    function alta() {
        setConfirmandoAlta(false);
        ejecutar(() => darDeAlta(id), 'Alta registrada. La cama fue liberada.');
    }

    if (cargando) return <p className="subtitulo">Cargando…</p>;
    if (error || !admision) {
        return (
            <>
                <p className="error" role="alert">{error || 'No se encontró la admisión.'}</p>
                <Link to="/admisiones">← Volver a admisiones</Link>
            </>
        );
    }

    const activa = admision.estado === 'ACTIVA';
    const esMedico = usuario.rol === 'MEDICO';
    const esAdmisiones = usuario.rol === 'ADMISIONES';
    const sedes = sedesDe(inventario);
    const opcionesServicio = inventario.filter((i) => i.sedeId === sedeDestino);

    return (
        <>
            <Link to="/admisiones">← Volver a admisiones</Link>
            <div className="encabezado" style={{ marginTop: 12 }}>
                <div>
                    <h1>Admisión de {admision.pacienteId}</h1>
                    <p className="subtitulo mono">{admision.id}</p>
                    {esMedico && (
                        <Link to={`/paciente/${encodeURIComponent(admision.pacienteId)}`}>
                            Ver estado consolidado →
                        </Link>
                    )}
                </div>
                <BadgeEstado estado={admision.estado} />
            </div>

            {aviso && <p className="aviso-ok" role="status">{aviso}</p>}

            <div className="tarjeta" style={{ marginBottom: 24 }}>
                <dl className="datos">
                    <dt>Ubicación actual</dt><dd>{admision.sedeActualId} · {admision.servicioActual}</dd>
                    <dt>Médico tratante</dt><dd>{admision.medicoTratante}</dd>
                    <dt>Motivo de ingreso</dt><dd>{admision.motivoIngreso}</dd>
                    <dt>Cobertura aseguradora</dt>
                    <dd>{admision.coberturaPorcentaje == null ? 'Sin verificar' : `${admision.coberturaPorcentaje}%`}</dd>
                    <dt>Fecha de ingreso</dt><dd>{fechaHora(admision.fechaIngreso)}</dd>
                    <dt>Fecha de alta</dt><dd>{fechaHora(admision.fechaAlta)}</dd>
                    <dt>Traslados</dt><dd>{admision.traslados.length}</dd>
                </dl>
            </div>

            <h2>Historial del paciente</h2>
            <ol className="linea-tiempo">
                {construirLinea(admision).map((e) => (
                    <li key={e.clave}>
                        <strong>{e.titulo}</strong>
                        <span>{e.detalle}</span>
                        <small>{fechaHora(e.fecha)}</small>
                    </li>
                ))}
            </ol>

            {errorAccion && <p className="error" role="alert">{errorAccion}</p>}

            {activa && esAdmisiones && (
                <div className="tarjeta" style={{ marginBottom: 24 }}>
                    <h2>Verificación de cobertura</h2>
                    <p className="subtitulo">
                        Consulta el porcentaje cubierto por la aseguradora externa para este paciente (HU-07).
                    </p>
                    <button className="boton" type="button" disabled={ocupado} onClick={verificarCobertura}>
                        {ocupado ? 'Consultando aseguradora…' : 'Verificar cobertura con aseguradora'}
                    </button>
                </div>
            )}

            {activa && esMedico && (
                <div className="paneles">
                    <form className="tarjeta" onSubmit={trasladar}>
                        <h2>Trasladar paciente</h2>
                        <label htmlFor="sede-destino">Sede destino</label>
                        <select id="sede-destino" required value={sedeDestino}
                            onChange={(e) => { setSedeDestino(e.target.value); setServicioDestino(''); }}>
                            <option value="" disabled>Selecciona una sede</option>
                            {sedes.map((s) => <option key={s} value={s}>{s}</option>)}
                        </select>

                        <label htmlFor="servicio-destino">Servicio destino</label>
                        <select id="servicio-destino" required value={servicioDestino} disabled={!sedeDestino}
                            onChange={(e) => setServicioDestino(e.target.value)}>
                            <option value="" disabled>
                                {sedeDestino ? 'Selecciona un servicio' : 'Elige primero una sede'}
                            </option>
                            {opcionesServicio.map((i) => {
                                const actual = i.sedeId === admision.sedeActualId && i.servicio === admision.servicioActual;
                                const sinCama = libres(i) === 0;
                                return (
                                    <option key={i.servicio} value={i.servicio} disabled={actual || sinCama}>
                                        {i.servicio} — {actual ? 'ubicación actual' : sinCama ? 'sin camas libres' : `${libres(i)} libres`}
                                    </option>
                                );
                            })}
                        </select>

                        <div className="acciones">
                            <button className="boton" type="submit"
                                disabled={ocupado || !sedeDestino || !servicioDestino}>
                                {ocupado ? 'Procesando…' : 'Trasladar'}
                            </button>
                        </div>
                    </form>

                    <div className="tarjeta">
                        <h2>Alta médica</h2>
                        <p className="subtitulo">
                            Cierra la admisión y libera la cama. Se rechaza si el paciente tiene órdenes de
                            laboratorio pendientes.
                        </p>
                        {!confirmandoAlta ? (
                            <button className="boton boton-peligro" disabled={ocupado}
                                onClick={() => setConfirmandoAlta(true)}>
                                Dar de alta
                            </button>
                        ) : (
                            <div className="confirmacion">
                                <p><strong>¿Confirmas el alta de {admision.pacienteId}?</strong></p>
                                <div className="acciones">
                                    <button className="boton boton-peligro" disabled={ocupado} onClick={alta}>
                                        Sí, dar de alta
                                    </button>
                                    <button className="boton-secundario" onClick={() => setConfirmandoAlta(false)}>
                                        Cancelar
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {activa && !esMedico && (
                <p className="nota">Solo el personal médico puede trasladar o dar de alta a un paciente.</p>
            )}
        </>
    );
}