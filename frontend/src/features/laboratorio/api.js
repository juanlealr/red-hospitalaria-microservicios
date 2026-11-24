import { api } from '../../api/cliente';

export const listarOrdenesPaciente = (pacienteId) =>
    api('/api/laboratorio/ordenes', { params: { pacienteId } });