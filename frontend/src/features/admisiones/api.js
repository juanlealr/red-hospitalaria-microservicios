import { api } from '../../api/cliente';

export const listarInventario = () => api('/api/admisiones/inventario-camas/todas');