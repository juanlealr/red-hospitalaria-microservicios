import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listarAdmisiones } from './api';
import BadgeEstado from './BadgeEstado';
import { fechaHora } from './formato';

export default function AdmisionesPage() {
    const [admisiones, setAdmisiones] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');
    const [estado, setEstado] = useState('');
    const [busqueda, setBusqueda] = useState('');

    const cargar = useCallback(async () => {
        try {
            setAdmisiones(await listarAdmisiones());
            setError('');
        } catch (err) {
            setError(err.message);
        } finally {
            setCargando(false);
        }
    }, []);

    useEffect(() => { cargar(); }, [cargar]);

    const texto = busqueda.trim().toLowerCase();
    const filas = admisiones
        .filter((a) => (!estado || a.estado === estado)
            && (!texto || a.pacienteId.toLowerCase().includes(texto)))
        .sort((a, b) => b.fechaIngreso.localeCompare(a.fechaIngreso));

    return (
        <>
            <div className="encabezado">
                <div>
                    <h1>Admisiones</h1>
                    <p className="subtitulo">Pacientes ingresados en la red</p>
                </div>
            </div>

            <div className="filtros">
                <label htmlFor="buscar">Paciente</label>
                <input id="buscar" placeholder="Buscar por documento o ID" value={busqueda}
                    onChange={(e) => setBusqueda(e.target.value)} style={{ width: 260 }} />
                <label htmlFor="estado">Estado</label>
                <select id="estado" value={estado} onChange={(e) => setEstado(e.target.value)}>
                    <option value="">Todas</option>
                    <option value="ACTIVA">Activas</option>
                    <option value="DADA_DE_ALTA">Dadas de alta</option>
                </select>
                <button className="boton-secundario" onClick={cargar}>Actualizar</button>
            </div>

            {error && <p className="error" role="alert">{error}</p>}
            {cargando && <p className="subtitulo">Cargando…</p>}
            {!cargando && !error && filas.length === 0 && (
                <div className="tarjeta vacio"><p>No hay admisiones que coincidan.</p></div>
            )}

            {filas.length > 0 && (
                <table className="tabla">
                    <thead>
                        <tr>
                            <th>Paciente</th><th>Sede</th><th>Servicio</th>
                            <th>Estado</th><th>Ingreso</th><th />
                        </tr>
                    </thead>
                    <tbody>
                        {filas.map((a) => (
                            <tr key={a.id}>
                                <td>{a.pacienteId}</td>
                                <td>{a.sedeActualId}</td>
                                <td>{a.servicioActual}</td>
                                <td><BadgeEstado estado={a.estado} /></td>
                                <td>{fechaHora(a.fechaIngreso)}</td>
                                <td><Link to={`/admisiones/${a.id}`}>Ver</Link></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </>
    );
}