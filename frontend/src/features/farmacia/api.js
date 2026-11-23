import { api } from '../../api/cliente';

const RUTA = '/api/farmacia/solicitudes';

export const listarSolicitudes = () => api(RUTA);

export const obtenerSolicitud = (id) => api(`${RUTA}/${encodeURIComponent(id)}`);

export const recibirSolicitud = ({ pacienteId, medicoId, medicamento }) =>
    api(RUTA, {
        metodo: 'POST',
        cuerpo: { pacienteId, medicoId, medicamento },
    });

export const verificarCobertura = (id) =>
    api(`${RUTA}/${encodeURIComponent(id)}/verificar-cobertura`, { metodo: 'POST' });

export const despacharSolicitud = (id) =>
    api(`${RUTA}/${encodeURIComponent(id)}/despachar`, { metodo: 'POST' });
