import { useEffect, useState } from 'react';
import { reporteOcupacion } from './api';
import BarraOcupacion from './BarraOcupacion';
import { nivelOcupacion, porcentaje, resumenPorSede } from './catalogo';
import { useInventario } from './useInventario';

export default function ReportePage() {
    const { inventario, cargando, error: errorInventario, recargar } = useInventario(30000);
    const [sede, setSede] = useState('');
    const [version, setVersion] = useState(0);
    const [resultado, setResultado] = useState(null);

    useEffect(() => {
        if (!sede) return undefined;
        let vigente = true;
        reporteOcupacion(sede)
            .then((datos) => { if (vigente) setResultado({ sede, datos, generado: new Date() }); })
            .catch((err) => { if (vigente) setResultado({ sede, error: err.message }); });
        return () => { vigente = false; };
    }, [sede, version]);

    const sedes = resumenPorSede(inventario);
    const red = sedes.reduce(
        (a, s) => ({ camasTotales: a.camasTotales + s.camasTotales, camasOcupadas: a.camasOcupadas + s.camasOcupadas }),
        { camasTotales: 0, camasOcupadas: 0 },
    );
    const detalle = resultado?.sede === sede ? resultado : null;

    function actualizar() {
        recargar();
        setVersion((v) => v + 1);
    }

    return (
        <>
            <div className="encabezado">
                <div>
                    <h1>Reporte de ocupación</h1>
                    <p className="subtitulo">Camas por sede en este momento</p>
                </div>
                <div className="acciones no-imprimir" style={{ marginTop: 0 }}>
                    <button className="boton-secundario" onClick={actualizar}>Actualizar</button>
                    <button className="boton-secundario" onClick={() => window.print()}>Imprimir</button>
                </div>
            </div>

            {errorInventario && <p className="error" role="alert">{errorInventario}</p>}
            {cargando && <p className="subtitulo">Cargando…</p>}
            {!cargando && !errorInventario && sedes.length === 0 && (
                <div className="tarjeta vacio"><p>Aún no hay camas registradas.</p></div>
            )}

            {sedes.length > 0 && (
                <>
                    <div className="tarjetas-resumen">
                        <div className="tarjeta"><span className="tenue">Camas totales</span><strong className="cifra">{red.camasTotales}</strong></div>
                        <div className="tarjeta"><span className="tenue">Ocupadas</span><strong className="cifra">{red.camasOcupadas}</strong></div>
                        <div className="tarjeta"><span className="tenue">Libres</span><strong className="cifra">{red.camasTotales - red.camasOcupadas}</strong></div>
                        <div className="tarjeta"><span className="tenue">Ocupación de la red</span><strong className="cifra">{porcentaje(red)}%</strong></div>
                    </div>

                    <h2>Sedes</h2>
                    <div className="tarjetas-resumen">
                        {sedes.map((s) => {
                            const nivel = nivelOcupacion(s);
                            return (
                                <button key={s.sedeId} type="button" aria-pressed={sede === s.sedeId}
                                    className={`tarjeta sede-card ${sede === s.sedeId ? 'sede-activa' : ''}`}
                                    onClick={() => setSede(s.sedeId)}>
                                    <strong>{s.sedeId}</strong>
                                    <span>{s.camasOcupadas} de {s.camasTotales} ocupadas</span>
                                    <BarraOcupacion porcentaje={porcentaje(s)} clase={nivel.clase} />
                                    <span className={`badge badge-${nivel.clase}`}>
                                        <span className="badge-punto" />{porcentaje(s)}% · {nivel.texto}
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                </>
            )}

            {sede && (
                <>
                    <h2>Detalle de {sede}</h2>
                    {detalle?.error && <p className="error" role="alert">{detalle.error}</p>}
                    {!detalle && <p className="subtitulo">Cargando detalle…</p>}
                    {detalle?.datos && (
                        <>
                            <table className="tabla">
                                <thead>
                                    <tr><th>Servicio</th><th>Totales</th><th>Ocupadas</th><th>Libres</th><th>Ocupación</th></tr>
                                </thead>
                                <tbody>
                                    {detalle.datos.servicios.map((s) => {
                                        const nivel = nivelOcupacion(s);
                                        return (
                                            <tr key={s.servicio}>
                                                <td>{s.servicio}</td>
                                                <td>{s.camasTotales}</td>
                                                <td>{s.camasOcupadas}</td>
                                                <td>{s.camasLibres}</td>
                                                <td>
                                                    <span className={`badge badge-${nivel.clase}`}>
                                                        <span className="badge-punto" />{s.porcentajeOcupacion}% · {nivel.texto}
                                                    </span>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                            <p className="nota">
                                Generado el {detalle.generado.toLocaleString()} ·
                                Total sede: {detalle.datos.camasOcupadas}/{detalle.datos.camasTotales} camas ocupadas
                                ({detalle.datos.porcentajeOcupacion}%)
                            </p>
                        </>
                    )}
                </>
            )}
        </>
    );
}