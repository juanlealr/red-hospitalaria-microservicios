import { api } from '../../api/cliente';

export const consultarConsolidado = (pacienteId) =>
    api(`/api/consultas/admisiones/paciente/${encodeURIComponent(pacienteId)}/consolidado`);