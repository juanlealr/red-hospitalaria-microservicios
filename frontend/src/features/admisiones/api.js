import { api } from '../../api/cliente';

export const listarInventario = () => api('/api/admisiones/inventario-camas/todas');

export const registrarIngreso = ({ pacienteId, sedeId, servicio, medicoTratante, motivoIngreso }) =>
    api('/api/admisiones/ingreso', {
        metodo: 'POST',
        params: { pacienteId, sedeId, servicio, medicoTratante, motivoIngreso },
    });

export const consultarCoberturaPaciente = (pacienteId) =>
    api(`/api/admisiones/cobertura/${encodeURIComponent(pacienteId)}`);

export const verificarCoberturaAdmision = (id) =>
    api(`/api/admisiones/${encodeURIComponent(id)}/verificar-cobertura`, { metodo: 'POST' });

export const listarAdmisiones = () => api('/api/admisiones');

export const obtenerAdmision = (id) => api(`/api/admisiones/${encodeURIComponent(id)}`);

export const trasladarAdmision = (id, { sedeDestinoId, servicioDestino }) =>
    api(`/api/admisiones/${encodeURIComponent(id)}/traslado`, {
        metodo: 'PUT',
        params: { sedeDestinoId, servicioDestino },
    });

export const darDeAlta = (id) =>
    api(`/api/admisiones/${encodeURIComponent(id)}/alta`, { metodo: 'PUT' });

export const reporteOcupacion = (sedeId) =>
    api('/api/admisiones/inventario-camas/reporte-ocupacion', { params: { sedeId } });

export const registrarCapacidad = ({ sedeId, servicio, camasTotales }) =>
    api('/api/admisiones/inventario-camas', {
        metodo: 'POST',
        cuerpo: { sedeId, servicio, camasTotales },
    });