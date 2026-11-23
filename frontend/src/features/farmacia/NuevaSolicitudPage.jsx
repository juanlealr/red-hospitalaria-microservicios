import { useState } from 'react';
import { Link } from 'react-router-dom';
import { fechaHora } from '../admisiones/formato';
import BadgeEstadoFarmacia from './BadgeEstadoFarmacia';
import { recibirSolicitud } from './api';

export default function NuevaSolicitudPage() {
    const [pacienteId, setPacienteId] = useState('');
    const [medicoId, setMedicoId] = useState('');
    const [medicamento, setMedicamento] = useState('');
    const [enviando, setEnviando] = useState(false);
    const [error, setError] = useState('');
    const [creada, setCreada] = useState(null);

    async function enviar(e) {
        e.preventDefault();
        if (enviando) return;
        setError('');
        setEnviando(true);
        try {
            const solicitud = await recibirSolicitud({
                pacienteId: pacienteId.trim(),
                medicoId: medicoId.trim(),
                medicamento: medicamento.trim(),
            });
            setCreada(solicitud);
            setPacienteId('');
            setMedicoId('');
            setMedicamento('');
        } catch (err) {
            setError(err.message);
        } finally {
            setEnviando(false);
        }
    }

    if (creada) {
        return (
            <>
                <h1>Solicitud registrada</h1>
                <p className="subtitulo">Farmacia la recibió y la verá en su bandeja.</p>

                <div className="tarjeta exito">
                    <dl>
                        <dt>Medicamento</dt><dd>{creada.medicamento}</dd>
                        <dt>Paciente</dt><dd>{creada.pacienteId}</dd>
                        <dt>Médico</dt><dd>{creada.medicoId}</dd>
                        <dt>Estado</dt><dd><BadgeEstadoFarmacia estado={creada.estado} /></dd>
                        <dt>Fecha</dt><dd>{fechaHora(creada.fechaSolicitud)}</dd>
                    </dl>
                    <p className="mono tenue">{creada.id}</p>
                </div>

                <div className="acciones">
                    <button type="button" className="boton" onClick={() => setCreada(null)}>
                        Nueva solicitud
                    </button>
                    <Link className="boton-secundario" to="/farmacia">Ir a farmacia</Link>
                </div>
            </>
        );
    }

    return (
        <>
            <h1>Nueva solicitud de medicamento</h1>
            <p className="subtitulo">Un medicamento no se despacha sin una solicitud médica asociada</p>

            <form className="formulario" onSubmit={enviar} noValidate>
                <label htmlFor="paciente">Paciente</label>
                <input id="paciente" value={pacienteId} required
                    placeholder="Documento o ID del paciente"
                    onChange={(e) => setPacienteId(e.target.value)} />

                <label htmlFor="medico">Médico solicitante</label>
                <input id="medico" value={medicoId} required
                    placeholder="Documento o ID del médico"
                    onChange={(e) => setMedicoId(e.target.value)} />

                <label htmlFor="medicamento">Medicamento</label>
                <input id="medicamento" value={medicamento} required
                    placeholder="Nombre, dosis y presentación"
                    onChange={(e) => setMedicamento(e.target.value)} />

                {error && <p className="error" role="alert">{error}</p>}

                <div className="acciones">
                    <button type="submit" className="boton"
                        disabled={enviando || !pacienteId.trim() || !medicoId.trim() || !medicamento.trim()}>
                        {enviando ? 'Enviando…' : 'Registrar solicitud'}
                    </button>
                    <Link className="boton-secundario" to="/farmacia">Cancelar</Link>
                </div>
            </form>
        </>
    );
}
