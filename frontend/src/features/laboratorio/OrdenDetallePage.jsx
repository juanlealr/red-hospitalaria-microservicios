import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fechaHora } from '../admisiones/formato';
import { cerrarOrden, obtenerOrden, registrarResultado } from './api';
import BadgeOrden from './BadgeOrden';
import { estadoDe, resultadoTexto, UNIDADES_FRECUENTES } from './catalogo';

export default function OrdenDetallePage() {
    const { id } = useParams();
    const { usuario } = useAuth();

    // HU-05: el resultado y el cierre los hace el LABORATORIO. El médico solo consulta.
    const esLaboratorio = usuario?.rol === 'LABORATORIO';

    const [orden, setOrden] = useState(null);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');
    const [avisoOk, setAvisoOk] = useState('');
    const [valor, setValor] = useState('');
    const [unidad, setUnidad] = useState('');
    const [procesando, setProcesando] = useState(false);
    const [confirmandoCierre, setConfirmandoCierre] = useState(false);

    const cargar = useCallback(() => obtenerOrden(id)
        .then((datos) => { setOrden(datos); })
        .catch((err) => {
            setError(err.status === 404 ? 'Orden de laboratorio no encontrada.' : err.message);
        })
        .finally(() => { setCargando(false); }), [id]);

    useEffect(() => {
        let vigente = true;
        obtenerOrden(id)
            .then((datos) => { if (vigente) setOrden(datos); })
            .catch((err) => {
                if (vigente) {
                    setError(err.status === 404 ? 'Orden de laboratorio no encontrada.' : err.message);
                }
            })
            .finally(() => { if (vigente) setCargando(false); });
        return () => { vigente = false; };
    }, [id]);

    async function guardarResultado(e) {
        e.preventDefault();
        if (procesando || !valor.trim() || !unidad.trim()) return;
        setError(''); setAvisoOk(''); setProcesando(true);
        try {
            const actualizada = await registrarResultado(id, {
                valor: valor.trim(),
                unidad: unidad.trim(),
            });
            setOrden(actualizada);
            setValor(''); setUnidad('');
            setAvisoOk('Resultado registrado. Falta cerrar la orden para liberar el alta médica.');
        } catch (err) {
            setError(err.message);
        } finally {
            setProcesando(false);
        }
    }

    async function ejecutarCierre() {
        if (procesando) return;
        setError(''); setAvisoOk(''); setProcesando(true);
        try {
            await cerrarOrden(id);
            setConfirmandoCierre(false);
            setAvisoOk('Orden cerrada. El paciente ya puede recibir el alta médica.');
            await cargar();
        } catch (err) {
            setConfirmandoCierre(false);
            setError(err.message);
        } finally {
            setProcesando(false);
        }
    }

    if (cargando) return <p className="subtitulo">Cargando orden…</p>;

    if (!orden) {
        return (
            <>
                <p><Link to="/laboratorio" className="boton-link-oscuro">← Volver a órdenes</Link></p>
                {error && <p className="error" role="alert">{error}</p>}
            </>
        );
    }

    const estado = estadoDe(orden);
    const tieneResultado = Boolean(orden.resultado);
    const cerrada = orden.estado === 'CERRADA';

    return (
        <>
            <p><Link to="/laboratorio" className="boton-link-oscuro">← Volver a órdenes</Link></p>

            <div className="encabezado">
                <div>
                    <h1>{orden.tipoExamen}</h1>
                    <p className="subtitulo">Paciente {orden.pacienteId}</p>
                    <p className="mono tenue">{orden.id}</p>
                </div>
                <BadgeOrden orden={orden} />
            </div>

            {usuario?.rol === 'MEDICO' && (
                <p>
                    <Link
                        className="boton-link-oscuro"
                        to={`/paciente/${encodeURIComponent(orden.pacienteId)}`}
                    >
                        Ver estado consolidado →
                    </Link>
                </p>
            )}

            {avisoOk && <p className="aviso-ok" role="status">{avisoOk}</p>}

            <div className="tarjeta">
                <dl className="datos">
                    <dt>Paciente</dt><dd>{orden.pacienteId}</dd>
                    <dt>Examen</dt><dd>{orden.tipoExamen}</dd>
                    <dt>Estado</dt><dd>{estado.texto}</dd>
                    <dt>Resultado</dt><dd>{resultadoTexto(orden)}</dd>
                    <dt>Fecha de solicitud</dt><dd>{fechaHora(orden.fechaSolicitud)}</dd>
                    <dt>Alta médica</dt>
                    <dd>{orden.pendiente ? 'Bloqueada por esta orden' : 'No bloqueada por esta orden'}</dd>
                </dl>
                <p className="nota">{estado.ayuda}</p>
            </div>

            <h2 style={{ marginTop: 28 }}>Historial de la orden</h2>
            <ul className="linea-tiempo">
                <li>
                    <strong>Examen solicitado</strong>
                    <span>El médico pidió {orden.tipoExamen}</span>
                    <small>{fechaHora(orden.fechaSolicitud)}</small>
                </li>
                {tieneResultado && (
                    <li>
                        <strong>Resultado registrado</strong>
                        <span>{resultadoTexto(orden)}</span>
                    </li>
                )}
                {cerrada && (
                    <li>
                        <strong>Orden cerrada</strong>
                        <span>Ya no bloquea el alta médica</span>
                    </li>
                )}
            </ul>

            {error && <p className="error" role="alert">{error}</p>}

            {!esLaboratorio && !cerrada && (
                <p className="nota">
                    El resultado y el cierre los registra el personal de laboratorio.
                </p>
            )}

            {esLaboratorio && !cerrada && (
                <div className="paneles">
                    <section className="tarjeta">
                        <h2>{tieneResultado ? 'Corregir resultado' : 'Registrar resultado'}</h2>
                        <form onSubmit={guardarResultado} noValidate>
                            <label htmlFor="valor">Valor</label>
                            <input
                                id="valor"
                                value={valor}
                                onChange={(e) => { setValor(e.target.value); setError(''); }}
                                placeholder="120"
                                maxLength={255}
                                disabled={procesando}
                            />

                            <label htmlFor="unidad">Unidad</label>
                            <input
                                id="unidad"
                                list="unidades-frecuentes"
                                value={unidad}
                                onChange={(e) => { setUnidad(e.target.value); setError(''); }}
                                placeholder="g/L"
                                maxLength={50}
                                disabled={procesando}
                            />
                            <datalist id="unidades-frecuentes">
                                {UNIDADES_FRECUENTES.map((u) => <option key={u} value={u} />)}
                            </datalist>

                            <div className="acciones">
                                <button
                                    type="submit"
                                    className="boton"
                                    disabled={procesando || !valor.trim() || !unidad.trim()}
                                >
                                    {procesando ? 'Guardando…' : 'Guardar resultado'}
                                </button>
                            </div>
                        </form>
                    </section>

                    <section className="tarjeta">
                        <h2>Cerrar orden</h2>
                        <p className="nota">
                            {tieneResultado
                                ? 'Al cerrarla, el paciente queda libre para recibir el alta médica.'
                                : 'Primero registra el resultado: una orden sin resultado no se puede cerrar.'}
                        </p>
                        <div className="acciones">
                            {!confirmandoCierre ? (
                                <button
                                    type="button"
                                    className="boton"
                                    disabled={procesando || !tieneResultado}
                                    onClick={() => setConfirmandoCierre(true)}
                                >
                                    Cerrar orden
                                </button>
                            ) : (
                                <>
                                    <button
                                        type="button"
                                        className="boton"
                                        disabled={procesando}
                                        onClick={ejecutarCierre}
                                    >
                                        {procesando ? 'Cerrando…' : 'Confirmar cierre'}
                                    </button>
                                    <button
                                        type="button"
                                        className="boton-secundario"
                                        disabled={procesando}
                                        onClick={() => setConfirmandoCierre(false)}
                                    >
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
