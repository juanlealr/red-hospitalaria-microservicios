import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useInventario } from './useInventario';
import { estadoDisponibilidad, libres, porcentajeLibre, sedesDe } from './catalogo';

export default function CamasPage() {
    const { usuario } = useAuth();
    const { inventario, cargando, error, actualizado, recargar } = useInventario();
    const [sede, setSede] = useState('');
    const [soloConCamas, setSoloConCamas] = useState(false);

    const sedes = sedesDe(inventario);
    const deLaSede = inventario.filter((i) => !sede || i.sedeId === sede);
    const filas = deLaSede.filter((i) => !soloConCamas || libres(i) > 0);
    const totalLibres = deLaSede.reduce((suma, i) => suma + libres(i), 0);
    const totalCamas = deLaSede.reduce((suma, i) => suma + i.camasTotales, 0);
    const puedeGestionar = usuario.rol === 'ADMINISTRADOR';

    return (
        <>
            <div className="encabezado">
                <div>
                    <h1>Disponibilidad de camas</h1>
                    <p className="subtitulo">Camas libres por sede y servicio para decidir dónde admitir</p>
                </div>
                {usuario.rol === 'ADMISIONES' && (
                    <Link className="boton" to="/admisiones/nueva">+ Registrar admisión</Link>
                )}
            </div>

            <div className="filtros">
                <label htmlFor="filtro-sede">Sede</label>
                <select id="filtro-sede" value={sede} onChange={(e) => setSede(e.target.value)}>
                    <option value="">Todas las sedes</option>
                    {sedes.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
                <label className="check-filtro">
                    <input
                        type="checkbox"
                        checked={soloConCamas}
                        onChange={(e) => setSoloConCamas(e.target.checked)}
                    />
                    Solo con camas libres
                </label>
                <span className="resumen">
                    <strong>{totalLibres}</strong> camas libres de {totalCamas}
                </span>
                <button className="boton-secundario" onClick={recargar}>Actualizar</button>
                {actualizado && (
                    <span className="nota">Actualizado {actualizado.toLocaleTimeString()}</span>
                )}
            </div>

            {error && <p className="error" role="alert">{error}</p>}
            {cargando && <p className="subtitulo">Cargando…</p>}

            {!cargando && !error && deLaSede.length === 0 && (
                <div className="tarjeta vacio">
                    <p>Aún no hay camas registradas.</p>
                    {puedeGestionar && <Link to="/capacidad">Registrar capacidad de camas</Link>}
                </div>
            )}

            {!cargando && !error && deLaSede.length > 0 && filas.length === 0 && (
                <div className="tarjeta vacio"><p>Ningún servicio tiene camas libres en este momento.</p></div>
            )}

            {filas.length > 0 && (
                <table className="tabla">
                    <thead>
                        <tr>
                            <th>Sede</th>
                            <th>Servicio</th>
                            <th>Camas libres</th>
                            <th>Estado</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filas.map((i) => {
                            const est = estadoDisponibilidad(i);
                            const l = libres(i);
                            return (
                                <tr key={`${i.sedeId}|${i.servicio}`}>
                                    <td>{i.sedeId}</td>
                                    <td>{i.servicio}</td>
                                    <td>
                                        <div className="libres-celda">
                                            <span className={`libres-num libres-${est.clase}`}>{l}</span>
                                            <span className="tenue">de {i.camasTotales} libres</span>
                                        </div>
                                        <div
                                            className="barra-ocupacion barra-fina"
                                            role="img"
                                            aria-label={`${l} de ${i.camasTotales} camas libres`}
                                        >
                                            <div
                                                className={`relleno relleno-${est.clase}`}
                                                style={{ width: `${porcentajeLibre(i)}%` }}
                                            />
                                        </div>
                                    </td>
                                    <td>
                                        <span className={`badge badge-${est.clase}`}>
                                            <span className="badge-punto" />
                                            {est.texto}
                                        </span>
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            )}

            <p className="nota leyenda-camas">
                La barra muestra qué parte de las camas está libre. Verde: hay camas · Amarillo: quedan pocas · Rojo: sin camas libres.
            </p>
        </>
    );
}