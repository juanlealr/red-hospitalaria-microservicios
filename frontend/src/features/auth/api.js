import { api } from '../../api/cliente';

export const crearUsuario = ({ email, password, rol }) =>
    api('/api/auth/usuarios', { metodo: 'POST', cuerpo: { email, password, rol } });