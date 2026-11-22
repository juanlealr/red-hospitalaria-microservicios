import { useCallback, useEffect, useMemo, useState } from 'react';

function buscarJwtEnStorage(storage) {
    if (!storage) return null;
    for (let i = 0; i < storage.length; i += 1) {
        const valor = storage.getItem(storage.key(i));
        if (!valor) continue;
        const limpio = valor.trim();
        if (/^eyJ[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+$/.test(limpio)) {
            return limpio;
        }
        if (limpio.startsWith('{')) {
            try {
                const obj = JSON.parse(limpio);
                for (const prop of ['token', 'accessToken', 'jwt', 'idToken']) {
                    if (typeof obj?.[prop] === 'string' && obj[prop].startsWith('eyJ')) {
                        return obj[prop];
                    }
                }
            } catch {
                // Ignorar si no es JSON válido
            }
        }
    }
    return null;
}

function obtenerToken() {
    return buscarJwtEnStorage(window.sessionStorage) || buscarJwtEnStorage(window.localStorage);
}

function clasificarDisponibilidad(totales, ocupadas) {
    if (totales <= 0) {
        return {
            clase: 'sin-camas',
            etiqueta: 'Sin camas',
            porcentajeOcupacion: 0,
            porcentajeDisponible: 0,
        };
    }

    const libres = Math.max(0, totales - ocupadas);
    const porcentajeDisponible = Math.round((libres / totales) * 100);
    const porcentajeOcupacion = 100 - porcentajeDisponible;

    if (libres === 0) {
        return {
            clase: 'critico',
            etiqueta: 'Sin disponibilidad',
            porcentajeOcupacion: 100,
            porcentajeDisponible: 0,
        };
    }
    if (porcentajeDisponible <= 25) {
        return {
            clase: 'alto',
            etiqueta: `${porcentajeDisponible}% disponible`,
            porcentajeOcupacion,
            porcentajeDisponible,
        };
    }
    return {
        clase: 'ok',
        etiqueta: `${porcentajeDisponible}% disponible`,
        porcentajeOcupacion,
        porcentajeDisponible,
    };
}

export default function ReportePage() {
    const [inventario, setInventario] = useState([]);
    const [cargando, setCargando] = useState(true);
    const [error, setError] = useState('');

    const cargarReporte = useCallback(async () => {
        setError('');
        setCargando(true);
        try {
            const token = obtenerToken();
            const respuesta = await fetch('/api/admisiones/inventario-camas/todas', {
                headers: token ? { Authorization: `Bearer ${token}` } : {},
            });
            if (respuesta.status === 401) {
                throw new Error('Tu sesión expiró (401). Cierra sesión e inicia nuevamente.');
            }
            if (!respuesta.ok) {
                throw new Error(`No se pudo cargar el reporte (${respuesta.status})`);
            }
            const datos = await respuesta.json();
            setInventario(Array.isArray(datos) ? datos : []);
        } catch (err) {
            setError(err.message || 'Error al consultar la disponibilidad de camas.');
        } finally {
            setCargando(false);
        }
    }, []);

    useEffect(() => {
        cargarReporte();
    }, [cargarReporte]);

    const { resumenRed, sedes } = useMemo(() => {
        const mapaSedes = new Map();
        let totalesRed = 0;
        let ocupadasRed = 0;

        for (const item of inventario) {
            const sedeId = item.sedeId || 'SIN SEDE';
            const totales = Number(item.camasTotales) || 0;
            const ocupadas = Number(item.camasOcupadas) || 0;
            const libres = item.camasLibres != null
                ? Number(item.camasLibres)
                : Math.max(0, totales - ocupadas);

            totalesRed += totales;
            ocupadasRed += ocupadas;

            if (!mapaSedes.has(sedeId)) {
                mapaSedes.set(sedeId, {
                    sedeId,
                    totales: 0,
                    ocupadas: 0,
                    libres: 0,
                    servicios: [],
                });
            }

            const grupo = mapaSedes.get(sedeId);
            grupo.totales += totales;
            grupo.ocupadas += ocupadas;
            grupo.libres += libres;
            grupo.servicios.push({
                servicio: item.servicio,
                totales,
                ocupadas,
                libres,
                estado: clasificarDisponibilidad(totales, ocupadas),
            });
        }

        const listaSedes = Array.from(mapaSedes.values())
            .sort((a, b) => a.sedeId.localeCompare(b.sedeId, 'es'))
            .map((sede) => ({
                ...sede,
                servicios: sede.servicios.sort((a, b) => a.servicio.localeCompare(b.servicio, 'es')),
                estado: clasificarDisponibilidad(sede.totales, sede.ocupadas),
            }));

        const libresRed = Math.max(0, totalesRed - ocupadasRed);
        const estadoRed = clasificarDisponibilidad(totalesRed, ocupadasRed);

        return {
            resumenRed: {
                totales: totalesRed,
                ocupadas: ocupadasRed,
                libres: libresRed,
                estado: estadoRed,
            },
            sedes: listaSedes,
        };
    }, [inventario]);

    return (
        <>
            <div className="reporte-cabecera">
                <div>
                    <h1>Reporte de ocupación</h1>
                    <p className="subtitulo">Disponibilidad de camas por sede en este momento</p>
                </div>
                <div className="reporte-acciones">
                    <button
                        type="button"
                        className="boton-secundario"
                        onClick={cargarReporte}
                        disabled={cargando}
                    >
                        {cargando ? 'Actualizando…' : 'Actualizar'}
                    </button>
                    <button
                        type="button"
                        className="boton-secundario"
                        onClick={() => window.print()}
                    >
                        Imprimir
                    </button>
                </div>
            </div>

            {error && <p className="error" role="alert">{error}</p>}

            <section className="reporte-metricas" aria-label="Resumen de la red">
                <div className="reporte-metrica">
                    <span className="reporte-metrica-etiqueta">Camas totales</span>
                    <strong className="reporte-metrica-valor">{resumenRed.totales}</strong>
                </div>

                <div className="reporte-metrica">
                    <span className="reporte-metrica-etiqueta">Disponibles</span>
                    <strong className="reporte-metrica-valor valor-ok">{resumenRed.libres}</strong>
                </div>

                <div className="reporte-metrica">
                    <span className="reporte-metrica-etiqueta">Ocupadas</span>
                    <strong className="reporte-metrica-valor">{resumenRed.ocupadas}</strong>
                </div>

                <div className="reporte-metrica">
                    <span className="reporte-metrica-etiqueta">Disponibilidad</span>
                    <strong className={`reporte-metrica-valor valor-${resumenRed.estado.clase}`}>
                        {resumenRed.estado.porcentajeDisponible}%
                    </strong>
                </div>
            </section>

            <h2 style={{ marginTop: 32 }}>Sedes</h2>

            {!cargando && sedes.length === 0 ? (
                <div className="tarjeta">
                    <p className="subtitulo" style={{ margin: 0 }}>
                        No hay sedes ni camas configuradas en el inventario.
                    </p>
                </div>
            ) : (
                <div className="sedes-grid">
                    {sedes.map((sede) => (
                        <article key={sede.sedeId} className="tarjeta sede-card">
                            <div className="sede-card-top">
                                <h3 className="sede-nombre">{sede.sedeId}</h3>
                                <span className={`estado-pill ${sede.estado.clase}`}>
                                    <span className="estado-punto" />
                                    {sede.estado.etiqueta}
                                </span>
                            </div>

                            <div className="sede-disponibilidad">
                                <strong className={`sede-libres-num ${sede.estado.clase}`}>
                                    {sede.libres}
                                </strong>
                                <span className="sede-libres-texto">
                                    {sede.libres === 1 ? 'cama libre' : 'camas libres'} de {sede.totales}
                                </span>
                            </div>

                            <div
                                className={`barra-capacidad ${sede.libres > 0 ? 'con-disponibilidad' : ''}`}
                                role="progressbar"
                                aria-valuenow={sede.estado.porcentajeDisponible}
                                aria-valuemin={0}
                                aria-valuemax={100}
                                aria-label={`Disponibilidad en ${sede.sedeId}: ${sede.estado.porcentajeDisponible}%`}
                            >
                                <div
                                    className={`barra-capacidad-relleno ${sede.estado.clase}`}
                                    style={{ width: `${sede.estado.porcentajeOcupacion}%` }}
                                />
                            </div>

                            <ul className="sede-servicios-lista">
                                {sede.servicios.map((srv) => (
                                    <li key={srv.servicio} className="sede-servicio-item">
                                        <span className="sede-servicio-nombre">{srv.servicio}</span>
                                        <span className={`sede-servicio-cupo ${srv.estado.clase}`}>
                                            {srv.libres} / {srv.totales} libres
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        </article>
                    ))}
                </div>
            )}
        </>
    );
}