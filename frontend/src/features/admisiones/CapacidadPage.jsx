import { useState } from 'react';
import { registrarCapacidad } from './api';
import { libres, sedesDe } from './catalogo';
import { useInventario } from './useInventario';

export default function CapacidadPage() {
    const { inventario, cargando, error: errorInventario, recargar } = useInventario(0);
    const [sedeId, setSedeId] = useState('');
    const [servicio, setServicio] = useState('');
    const [camasTotales, setCamasTotales] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState('');
    const [aviso, setAviso] = useState('');

    const serviciosConocidos = [...new Set(inventario.map((i) => i.servicio))].sort();

    async function enviar(e) {
        e.preventDefault();
        if (enviando) return;
        const sede = sedeId.trim().toUpperCase();
        const serv = servicio.trim().toUpperCase();
        const total = Number(camasTotales);
        setError('');
        setAviso('');
        if (!Number.isInteger(total) || total < 0) {
            setError('El total de camas debe ser un número entero mayor o igual a 0.');
            return;
        }
        setEnviando(true);
        try {
            await registrarCapacidad({ sedeId: sede, servicio: serv, camasTotales: total });
            setAviso(`Capacidad de ${sede} · ${serv} fijada en ${total} camas.`);
            setCamasTotales('');
            await recargar();
        } catch (err) {
            setError(err.message);
        } finally {
            setEnviando(false);
        }
    }

    function editar(i) {
        setSedeId(i.sedeId);
        setServicio(i.servicio);
        setCamasTotales(String(i.camasTotales));
        setAviso('');
        setError('');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    return (
        <>
            <h1>Capacidad de camas</h1>
            <p className="subtitulo">Define cuántas camas tiene cada sede y servicio</p>

            <form className="formulario" onSubmit={enviar}>
                <label htmlFor="cap-sede">Sede</label>
                <input id="cap-sede" required list="lista-sedes" value={sedeId}
                    onChange={(e) => setSedeId(e.target.value)} />
                <datalist id="lista-sedes">
                    {sedesDe(inventario).map((s) => <option key={s} value={s} />)}
                </datalist>

                <label htmlFor="cap-servicio">Servicio</label>
                <input id="cap-servicio" required list="lista-servicios" value={servicio}
                    onChange={(e) => setServicio(e.target.value)} />
                <datalist id="lista-servicios">
                    {serviciosConocidos.map((s) => <option key={s} value={s} />)}
                </datalist>

                <label htmlFor="cap-total">Total de camas</label>
                <input id="cap-total" type="number" min="0" step="1" required value={camasTotales}
                    onChange={(e) => setCamasTotales(e.target.value)} />

                {error && <p className="error" role="alert">{error}</p>}
                {aviso && <p className="aviso-ok" role="status" style={{ marginTop: 16 }}>{aviso}</p>}

                <div className="acciones">
                    <button className="boton" type="submit" disabled={enviando}>
                        {enviando ? 'Guardando…' : 'Guardar capacidad'}
                    </button>
                </div>
                <p className="nota">Si la sede y el servicio ya existen, se actualiza el total. No puede ser menor que las camas ocupadas.</p>
            </form>

            <h2 style={{ marginTop: 40 }}>Inventario actual</h2>
            {errorInventario && <p className="error" role="alert">{errorInventario}</p>}
            {cargando && <p className="subtitulo">Cargando…</p>}
            {inventario.length > 0 && (
                <table className="tabla">
                    <thead>
                        <tr><th>Sede</th><th>Servicio</th><th>Totales</th><th>Ocupadas</th><th>Libres</th><th /></tr>
                    </thead>
                    <tbody>
                        {inventario.map((i) => (
                            <tr key={`${i.sedeId}|${i.servicio}`}>
                                <td>{i.sedeId}</td><td>{i.servicio}</td>
                                <td>{i.camasTotales}</td><td>{i.camasOcupadas}</td><td>{libres(i)}</td>
                                <td><button type="button" className="boton-link boton-link-oscuro" onClick={() => editar(i)}>Editar</button></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </>
    );
}