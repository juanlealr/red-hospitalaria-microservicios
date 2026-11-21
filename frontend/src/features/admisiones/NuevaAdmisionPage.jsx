import { useState } from 'react';
import { Link } from 'react-router-dom';
import { consultarCoberturaPaciente, registrarIngreso, verificarCoberturaAdmision } from './api';
import { useInventario } from './useInventario';
import { libres, sedesDe } from './catalogo';

export default function NuevaAdmisionPage() {
    const { inventario, cargando, error: errorInventario, recargar } = useInventario();
    const [pacienteId, setPacienteId] = useState('');
    const [sedeId, setSedeId] = useState('');
    const [servicio, setServicio] = useState('');
    const [medicoTratante, setMedicoTratante] = useState('');
    const [motivoIngreso, setMotivoIngreso] = useState('');
    const [coberturaPrevia, setCoberturaPrevia] = useState(null);
    const [consultandoCob, setConsultandoCob] = useState(false);
    const [errorCob, setErrorCob] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState('');
    const [creada, setCreada] = useState(null);

    const sedes = sedesDe(inventario);
    const serviciosDeSede = inventario.filter((i) => i.sedeId === sedeId);

    function cambiarSede(valor) {
        setSedeId(valor);
        setServicio('');
    }

    function otraAdmision() {
        setCreada(null);
        setPacienteId('');
        setSedeId('');
        setServicio('');
        setMedicoTratante('');
        setMotivoIngreso('');
        setCoberturaPrevia(null);
        setErrorCob('');
        setError('');
    }

    async function consultarCobertura() {
        if (!pacienteId.trim() || consultandoCob) return;
        setConsultandoCob(true);
        setErrorCob('');
        setCoberturaPrevia(null);
        try {
            const res = await consultarCoberturaPaciente(pacienteId.trim());
            setCoberturaPrevia(res.porcentajeCobertura);
        } catch (err) {
            setErrorCob(`${err.message} (El ingreso puede continuar sin bloqueo).`);
        } finally {
            setConsultandoCob(false);
        }
    }

    async function verificarEnAdmisionCreada() {
        if (!creada || consultandoCob) return;
        setConsultandoCob(true);
        setErrorCob('');
        try {
            const actualizada = await verificarCoberturaAdmision(creada.id);
            setCreada(actualizada);
        } catch (err) {
            setErrorCob(err.message);
        } finally {
            setConsultandoCob(false);
        }
    }

    async function enviar(e) {
        e.preventDefault();
        if (enviando) return;
        setError('');
        setErrorCob('');
        setEnviando(true);
        try {
            const admision = await registrarIngreso({
                pacienteId: pacienteId.trim(),
                sedeId,
                servicio,
                medicoTratante: medicoTratante.trim(),
                motivoIngreso: motivoIngreso.trim(),
            });
            setCreada(admision);
        } catch (err) {
            setError(err.message);
        } finally {
            setEnviando(false);
            recargar();
        }
    }

    if (creada) {
        return (
            <>
                <h1>Admisión registrada</h1>
                <p className="subtitulo">El paciente ya está ingresado y la cama fue ocupada.</p>
                <div className="tarjeta exito">
                    <dl>
                        <dt>N.º de admisión</dt><dd className="mono">{creada.id}</dd>
                        <dt>Paciente</dt><dd>{creada.pacienteId}</dd>
                        <dt>Sede</dt><dd>{creada.sedeActualId}</dd>
                        <dt>Servicio</dt><dd>{creada.servicioActual}</dd>
                        <dt>Médico tratante</dt><dd>{creada.medicoTratante}</dd>
                        <dt>Motivo de ingreso</dt><dd>{creada.motivoIngreso}</dd>
                        <dt>Cobertura aseguradora</dt>
                        <dd>{creada.coberturaPorcentaje == null ? 'Sin verificar' : `${creada.coberturaPorcentaje}%`}</dd>
                        <dt>Fecha de ingreso</dt><dd>{new Date(creada.fechaIngreso).toLocaleString()}</dd>
                    </dl>
                    {errorCob && <p className="error" role="alert">{errorCob}</p>}
                    <div className="acciones">
                        {creada.coberturaPorcentaje == null && (
                            <button className="boton-secundario" type="button" disabled={consultandoCob}
                                onClick={verificarEnAdmisionCreada}>
                                {consultandoCob ? 'Verificando…' : 'Verificar cobertura ahora'}
                            </button>
                        )}
                        <button className="boton" onClick={otraAdmision}>Registrar otra admisión</button>
                        <Link className="boton-secundario" to="/camas">Ver disponibilidad</Link>
                    </div>
                </div>
            </>
        );
    }

    const sinInventario = !cargando && !errorInventario && sedes.length === 0;
    const formularioIncompleto = !pacienteId.trim() || !sedeId || !servicio || !medicoTratante.trim() || !motivoIngreso.trim();

    return (
        <>
            <h1>Nueva admisión</h1>
            <p className="subtitulo">Registro de ingreso de paciente</p>

            {errorInventario && <p className="error" role="alert">{errorInventario}</p>}
            {sinInventario && (
                <p className="error" role="alert">
                    No hay camas registradas en ninguna sede, así que no se puede admitir a nadie todavía.
                </p>
            )}

            <form className="formulario" onSubmit={enviar}>
                <label htmlFor="paciente">Paciente (documento o ID)</label>
                <div className="acciones" style={{ marginTop: 0 }}>
                    <input id="paciente" required maxLength={60} value={pacienteId}
                        onChange={(e) => { setPacienteId(e.target.value); setCoberturaPrevia(null); setErrorCob(''); }} />
                    <button type="button" className="boton-secundario"
                        disabled={!pacienteId.trim() || consultandoCob} onClick={consultarCobertura}>
                        {consultandoCob ? 'Consultando…' : 'Verificar cobertura'}
                    </button>
                </div>
                {coberturaPrevia != null && (
                    <p className="aviso-ok" role="status" style={{ marginTop: 8 }}>
                        Cobertura confirmada por aseguradora: <strong>{coberturaPrevia}%</strong>
                    </p>
                )}
                {errorCob && <p className="error" role="alert">{errorCob}</p>}

                <label htmlFor="sede">Sede</label>
                <select id="sede" required value={sedeId} onChange={(e) => cambiarSede(e.target.value)}>
                    <option value="" disabled>Selecciona una sede</option>
                    {sedes.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>

                <label htmlFor="servicio">Servicio</label>
                <select id="servicio" required value={servicio} disabled={!sedeId}
                    onChange={(e) => setServicio(e.target.value)}>
                    <option value="" disabled>
                        {sedeId ? 'Selecciona un servicio' : 'Elige primero una sede'}
                    </option>
                    {serviciosDeSede.map((i) => (
                        <option key={i.servicio} value={i.servicio} disabled={libres(i) === 0}>
                            {i.servicio} — {libres(i) === 0 ? 'sin camas libres' : `${libres(i)} libres`}
                        </option>
                    ))}
                </select>

                <label htmlFor="medico">Médico tratante</label>
                <input id="medico" required maxLength={150} placeholder="Nombre o ID del médico"
                    value={medicoTratante} onChange={(e) => setMedicoTratante(e.target.value)} />

                <label htmlFor="motivo">Motivo de ingreso</label>
                <textarea id="motivo" required rows={3} maxLength={500}
                    placeholder="Descripción clínica o motivo de la admisión"
                    value={motivoIngreso} onChange={(e) => setMotivoIngreso(e.target.value)} />

                {error && <p className="error" role="alert">{error}</p>}

                <div className="acciones">
                    <button className="boton" type="submit" disabled={enviando || formularioIncompleto}>
                        {enviando ? 'Registrando…' : 'Registrar'}
                    </button>
                </div>
            </form>
        </>
    );
}