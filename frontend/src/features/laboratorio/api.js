import { api } from '../../api/cliente';

// Todas pasan por el gateway (/api/laboratorio/**), que solo deja entrar a MEDICO y LABORATORIO.

/** Órdenes de un paciente. La usa el detalle de admisión y el estado consolidado. */
export const listarOrdenesPaciente = (pacienteId) =>
    api('/api/laboratorio/ordenes', { params: { pacienteId } });

/** Todas las órdenes de la red; con pacienteId filtra igual que listarOrdenesPaciente. */
export const listarOrdenes = (pacienteId) =>
    api('/api/laboratorio/ordenes', { params: { pacienteId } });

export const obtenerOrden = (id) =>
    api(`/api/laboratorio/ordenes/${encodeURIComponent(id)}`);

/** HU-04: solo el MÉDICO puede solicitar. */
export const solicitarExamen = ({ pacienteId, tipoExamen }) =>
    api('/api/laboratorio/ordenes', { metodo: 'POST', cuerpo: { pacienteId, tipoExamen } });

/** HU-05: solo el LABORATORIO puede registrar el resultado. */
export const registrarResultado = (id, { valor, unidad }) =>
    api(`/api/laboratorio/ordenes/${encodeURIComponent(id)}/resultado`, {
        metodo: 'POST',
        cuerpo: { valor, unidad },
    });

/** Cierra la orden: deja de bloquear el alta médica (HU-09). Solo LABORATORIO. */
export const cerrarOrden = (id) =>
    api(`/api/laboratorio/ordenes/${encodeURIComponent(id)}/cierre`, { metodo: 'PUT' });

export const consultarPendientes = (pacienteId) =>
    api(`/api/laboratorio/ordenes/pendientes/${encodeURIComponent(pacienteId)}`);
