import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useInventario } from './useInventario';
import { libres, nivelOcupacion, porcentaje, sedesDe } from './catalogo';

export default function CamasPage() {
    const { usuario } = useAuth();
    const { inventario, cargando, error, actualizado, recargar } = useInventario();
    const [sede, setSede] = useState('');

    const sedes = sedesDe(inventario);
    const filas = inventario.filter((i) => !sede || i.sedeId === sede);
    const totalLibres = filas.reduce((suma, i) => suma + libres(i), 0);
    const puedeGestionar = ['ADMISIONES', 'ADMINISTRADOR'].includes(usuario.rol);

    return (
        <>
            <div className="encabezado">
                <div>
                    <h1>Disponibilidad de camas</h1>
                    <p className="subtitulo">Ocupación por sede y servicio</p>
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
                <span className="resumen"><strong>{totalLibres}</strong> camas libres</span>
                <button className="boton-secundario" onClick={recargar}>Actualizar</button>
                {actualizado && (
                    <span className="nota">Actualizado {actualizado.toLocaleTimeString()}</span>
                )}
            </div>

            {error && <p className="error" role="alert">{error}</p>}
            {cargando && <p className="subtitulo">Cargando…</p>}

            {!cargando && !error && filas.length === 0 && (
                <div className="tarjeta vacio">
                    <p>Aún no hay camas registradas.</p>
                    {puedeGestionar && <Link to="/capacidad">Registrar capacidad de camas</Link>}
                </div>
            )}

            {filas.length > 0 && (
                <table className="tabla">
                    <thead>
                        <tr>
                            <th>Sede</th>
                            <th>Servicio</th>
                            <th>Camas libres</th>
                            <th>Ocupación</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filas.map((i) => {
                            const nivel = nivelOcupacion(i);
                            return (
                                <tr key={`${i.sedeId}|${i.servicio}`}>
                                    <td>{i.sedeId}</td>
                                    <td>{i.servicio}</td>
                                    <td>{libres(i)} <span className="tenue">de {i.camasTotales}</span></td>
                                    <td>
                                        <span className={`badge badge-${nivel.clase}`}>
                                            <span className="badge-punto" />
                                            {porcentaje(i)}% · {nivel.texto}
                                        </span>
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