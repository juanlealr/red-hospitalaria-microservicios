import { useMemo, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { registrarCapacidad } from './api';
import { libres } from './catalogo';
import { useInventario } from './useInventario';

const SERVICIOS_SUGERIDOS = ['URGENCIAS', 'UCI', 'HOSPITALIZACION', 'PEDIATRIA', 'CIRUGIA'];

export default function CapacidadPage() {
    const { usuario } = useAuth();
    const puedeEditar = usuario?.rol === 'ADMINISTRADOR';

    const { inventario, cargando, error: errorCarga, recargar } = useInventario(0);
    const [sede, setSede] = useState('');
    const [servicio, setServicio] = useState('');
    const [camasTotales, setCamasTotales] = useState('');
    const [editando, setEditando] = useState(false);
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState('');
    const [guardado, setGuardado] = useState(null);

    const sedesExistentes = useMemo(
        () => [...new Set(inventario.map((i) => i.sedeId))].sort((a, b) => a.localeCompare(b, 'es')),
        [inventario],
    );

    function limpiarFormulario() {
        setSede('');
        setServicio('');
        setCamasTotales('');
        setEditando(false);
        setError('');
    }

    function cargarParaEditar(item) {
        setSede(item.sedeId);
        setServicio(item.servicio);
        setCamasTotales(String(item.camasTotales));
        setEditando(true);
        setError('');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    async function enviar(e) {
        e.preventDefault();
        if (!puedeEditar || enviando) return;

        const sedeEscrita = sede.trim();
        const sedeExistente = inventario.find(
            (i) => i.sedeId.toLowerCase() === sedeEscrita.toLowerCase(),
        );
        const sedeId = sedeExistente ? sedeExistente.sedeId : sedeEscrita;

        const servicioEscrito = servicio.trim();
        const servicioExistente = inventario.find(
            (i) => i.sedeId === sedeId && i.servicio.toLowerCase() === servicioEscrito.toLowerCase(),
        );
        const servicioNormalizado = servicioExistente
            ? servicioExistente.servicio
            : servicioEscrito.toUpperCase();

        const totales = Number(camasTotales);

        if (!sedeId) { setError('Ingresa el nombre de la sede.'); return; }
        if (!servicioNormalizado) { setError('Ingresa el nombre del servicio.'); return; }
        if (camasTotales === '' || !Number.isInteger(totales) || totales < 0) {
            setError('El total de camas debe ser un número entero mayor o igual a 0.');
            return;
        }

        const yaExistia = Boolean(servicioExistente);

        setError('');
        setEnviando(true);
        try {
            const resultado = await registrarCapacidad({
                sedeId,
                servicio: servicioNormalizado,
                camasTotales: totales,
            });
            await recargar();
            limpiarFormulario();
            setGuardado({
                titulo: yaExistia ? 'Capacidad actualizada' : 'Sede y servicio registrados',
                sedeId: resultado?.sedeId ?? sedeId,
                servicio: resultado?.servicio ?? servicioNormalizado,
                camasTotales: resultado?.camasTotales ?? totales,
                camasLibres: resultado?.camasLibres ?? totales - (resultado?.camasOcupadas ?? 0),
            });
        } catch (err) {
            setError(err.message);
        } finally {
            setEnviando(false);
        }
    }

    return (
        <>
            <h1>Capacidad</h1>
            <p className="subtitulo">
                {puedeEditar
                    ? 'Define cuántas camas tiene cada sede y servicio'
                    : 'Camas que tiene cada sede y servicio (solo consulta)'}
            </p>

            {guardado && (
                <div
                    className="modal-fondo"
                    onClick={() => setGuardado(null)}
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="modal-sede-titulo"
                >
                    <div className="tarjeta exito modal-ventana" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-cabecera">
                            <strong id="modal-sede-titulo" className="modal-titulo">
                                <span className="modal-check">✓</span>
                                {guardado.titulo}
                            </strong>
                            <button
                                type="button"
                                className="modal-cerrar-x"
                                onClick={() => setGuardado(null)}
                                aria-label="Cerrar aviso"
                            >
                                ×
                            </button>
                        </div>
                        <dl>
                            <dt>Sede</dt><dd>{guardado.sedeId}</dd>
                            <dt>Servicio</dt><dd>{guardado.servicio}</dd>
                            <dt>Total de camas</dt><dd>{guardado.camasTotales}</dd>
                            <dt>Camas libres</dt><dd>{guardado.camasLibres}</dd>
                        </dl>
                        <div className="modal-pie">
                            <button type="button" className="boton" onClick={() => setGuardado(null)}>
                                Cerrar
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {puedeEditar && (
                <form className="formulario" onSubmit={enviar} noValidate>
                    <label htmlFor="cap-sede">Sede</label>
                    <input
                        id="cap-sede"
                        type="text"
                        list="lista-sedes"
                        value={sede}
                        onChange={(e) => { setSede(e.target.value); setError(''); }}
                    />
                    <datalist id="lista-sedes">
                        {sedesExistentes.map((s) => <option key={s} value={s} />)}
                    </datalist>

                    <label htmlFor="cap-servicio">Servicio</label>
                    <input
                        id="cap-servicio"
                        type="text"
                        list="lista-servicios"
                        value={servicio}
                        onChange={(e) => { setServicio(e.target.value); setError(''); }}
                    />
                    <datalist id="lista-servicios">
                        {SERVICIOS_SUGERIDOS.map((s) => <option key={s} value={s} />)}
                    </datalist>

                    <label htmlFor="cap-totales">Total de camas</label>
                    <input
                        id="cap-totales"
                        type="number"
                        min={0}
                        value={camasTotales}
                        onChange={(e) => { setCamasTotales(e.target.value); setError(''); }}
                    />

                    {error && <p className="error" role="alert">{error}</p>}

                    <div className="acciones">
                        <button className="boton" type="submit" disabled={enviando}>
                            {enviando ? 'Guardando…' : 'Guardar capacidad'}
                        </button>
                        {editando && (
                            <button
                                type="button"
                                className="boton-secundario"
                                onClick={limpiarFormulario}
                                disabled={enviando}
                            >
                                Cancelar
                            </button>
                        )}
                    </div>

                    <p className="nota">
                        Si la sede y el servicio ya existen, se actualiza el total. No puede ser menor que las camas ocupadas.
                    </p>
                </form>
            )}

            <h2 style={{ marginTop: puedeEditar ? 40 : 24 }}>Inventario actual</h2>

            {errorCarga && <p className="error" role="alert">{errorCarga}</p>}
            {cargando && <p className="subtitulo">Cargando…</p>}

            {!cargando && !errorCarga && inventario.length === 0 ? (
                <div className="tarjeta">
                    <p className="subtitulo" style={{ margin: 0 }}>Aún no hay sedes ni camas registradas.</p>
                </div>
            ) : (
                inventario.length > 0 && (
                    <table className="tabla">
                        <thead>
                            <tr>
                                <th>Sede</th>
                                <th>Servicio</th>
                                <th>Totales</th>
                                <th>Ocupadas</th>
                                <th>Libres</th>
                                {puedeEditar && <th />}
                            </tr>
                        </thead>
                        <tbody>
                            {inventario.map((item) => (
                                <tr key={`${item.sedeId}|${item.servicio}`}>
                                    <td>{item.sedeId}</td>
                                    <td>{item.servicio}</td>
                                    <td>{item.camasTotales}</td>
                                    <td>{item.camasOcupadas}</td>
                                    <td>{libres(item)}</td>
                                    {puedeEditar && (
                                        <td>
                                            <button
                                                type="button"
                                                className="boton-link"
                                                onClick={() => cargarParaEditar(item)}
                                            >
                                                Editar
                                            </button>
                                        </td>
                                    )}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )
            )}
        </>
    );
}