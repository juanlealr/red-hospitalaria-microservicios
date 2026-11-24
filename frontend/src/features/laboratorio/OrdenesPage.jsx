import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fechaHora } from '../admisiones/formato';
import { listarOrdenes } from './api';
import BadgeOrden from './BadgeOrden';
import { resultadoTexto, resumen } from './catalogo';

export default function OrdenesPage() {
    const { usuario } = useAuth();
    const esMedico = usuario?.rol === 'MEDICO';
    const esLaboratorio = usuario?.rol === 'LABORATORIO';

    const [ordenes, setOrdenes] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');
    const [estado, setEstado] = useState('');
    const [busqueda, setBusqueda] = useState('');
    const [soloPendientes, setSoloPendientes] = useState(false);
    const [version, setVersion] = useState(0);

    useEffect(() => {
        let vigente = true;
        listarOrdenes()
            .then((datos) => {
                if (!vigente) return;
                setOrdenes(datos);
                setError('');
                setCargando(false);
            })
            .catch((err) => {
                if (!vigente) return;
                setError(err.message);
                setCargando(false);
            });
        return () => { vigente = false; };
    }, [version]);

    const recargar = () => setVersion((v) => v + 1);

    const texto = busqueda.trim().toLowerCase();
    const filas = ordenes
        .filter((o) => (!estado || o.estado === estado)
            && (!soloPendientes || o.pendiente)
            && (!texto
                || o.pacienteId.toLowerCase().includes(texto)
                || o.tipoExamen.toLowerCase().includes(texto)))
        .sort((a, b) => (b.fechaSolicitud ?? '').localeCompare(a.fechaSolicitud ?? ''));

    const cuenta = resumen(ordenes);

    return (
        <>
            <div className="encabezado">
                <div>
                    <h1>Órdenes de laboratorio</h1>
                    <p className="subtitulo">
                        {esLaboratorio
                            ? 'Exámenes solicitados por los médicos de la red'
                            : 'Exámenes solicitados a laboratorio'}
                    </p>
                </div>
                {esMedico && (
                    <Link className="boton" to="/laboratorio/nueva">Solicitar examen</Link>
                )}
            </div>

            {!cargando && !error && ordenes.length > 0 && (
                <div className="tarjetas-resumen">
                    <div className="tarjeta">
                        <span className="tenue">Pendientes</span>
                        <span className="cifra">{cuenta.pendientes}</span>
                        <span className="tenue">Bloquean el alta médica</span>
                    </div>
                    <div className="tarjeta">
                        <span className="tenue">Por procesar</span>
                        <span className="cifra">{cuenta.solicitadas}</span>
                        <span className="tenue">Sin resultado registrado</span>
                    </div>
                    <div className="tarjeta">
                        <span className="tenue">Por cerrar</span>
                        <span className="cifra">{cuenta.conResultado}</span>
                        <span className="tenue">Con resultado, falta el cierre</span>
                    </div>
                    <div className="tarjeta">
                        <span className="tenue">Cerradas</span>
                        <span className="cifra">{cuenta.cerradas}</span>
                        <span className="tenue">De {cuenta.total} en total</span>
                    </div>
                </div>
            )}

            <div className="filtros">
                <label htmlFor="buscar">Buscar</label>
                <input
                    id="buscar"
                    placeholder="Paciente o tipo de examen"
                    value={busqueda}
                    onChange={(e) => setBusqueda(e.target.value)}
                    style={{ width: 260 }}
                />
                <label htmlFor="estado">Estado</label>
                <select id="estado" value={estado} onChange={(e) => setEstado(e.target.value)}>
                    <option value="">Todos</option>
                    <option value="SOLICITADA">Solicitadas</option>
                    <option value="CON_RESULTADO">Con resultado</option>
                    <option value="CERRADA">Cerradas</option>
                </select>
                <label className="check-filtro" htmlFor="pendientes">
                    <input
                        id="pendientes"
                        type="checkbox"
                        checked={soloPendientes}
                        onChange={(e) => setSoloPendientes(e.target.checked)}
                    />
                    Solo pendientes
                </label>
                <button className="boton-secundario" type="button" onClick={recargar}>Actualizar</button>
            </div>

            {error && <p className="error" role="alert">{error}</p>}
            {cargando && <p className="subtitulo">Cargando…</p>}

            {!cargando && !error && ordenes.length === 0 && (
                <div className="tarjeta vacio">
                    <p>Todavía no hay órdenes de laboratorio.</p>
                    {esMedico && (
                        <p className="nota">Usa «Solicitar examen» para crear la primera.</p>
                    )}
                </div>
            )}

            {!cargando && !error && ordenes.length > 0 && filas.length === 0 && (
                <div className="tarjeta vacio"><p>Ninguna orden coincide con el filtro.</p></div>
            )}

            {filas.length > 0 && (
                <table className="tabla">
                    <thead>
                        <tr>
                            <th>Paciente</th><th>Examen</th><th>Estado</th>
                            <th>Resultado</th><th>Solicitud</th><th />
                        </tr>
                    </thead>
                    <tbody>
                        {filas.map((o) => (
                            <tr key={o.id}>
                                <td>{o.pacienteId}</td>
                                <td>{o.tipoExamen}</td>
                                <td><BadgeOrden orden={o} /></td>
                                <td>{resultadoTexto(o)}</td>
                                <td>{fechaHora(o.fechaSolicitud)}</td>
                                <td>
                                    <Link to={`/laboratorio/${o.id}`}>
                                        {esLaboratorio && o.pendiente ? 'Atender' : 'Ver'}
                                    </Link>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}

            {filas.length > 0 && (
                <p className="nota">
                    Una orden deja de bloquear el alta médica solo cuando el laboratorio la cierra.
                </p>
            )}
        </>
    );
}
