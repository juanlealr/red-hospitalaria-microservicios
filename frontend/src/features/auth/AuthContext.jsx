import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, almacenToken, registrarAlExpirarSesion } from '../../api/cliente';

const AuthContext = createContext(null);

function decodificar(token) {
    try {
        const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const json = decodeURIComponent(
            atob(base64).split('').map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0')).join(''),
        );
        return JSON.parse(json);
    } catch {
        return null;
    }
}

function usuarioDesdeToken(token) {
    if (!token) return null;
    const payload = decodificar(token);
    if (!payload || !payload.rol) return null;
    const expira = payload.exp ? payload.exp * 1000 : null;
    if (expira && expira < Date.now()) return null;
    return { email: payload.sub, rol: payload.rol, expira };
}

export function AuthProvider({ children }) {
    const [usuario, setUsuario] = useState(() => {
        const u = usuarioDesdeToken(almacenToken.leer());
        if (!u) almacenToken.borrar();
        return u;
    });

    const logout = useCallback(() => {
        almacenToken.borrar();
        setUsuario(null);
    }, []);

    const login = useCallback(async (email, password) => {
        const r = await api('/api/auth/login', { metodo: 'POST', cuerpo: { email, password } });
        almacenToken.guardar(r.token);
        const u = usuarioDesdeToken(r.token);
        setUsuario(u);
        return u;
    }, []);

    // Si el backend responde 401 con token, la sesión murió.
    useEffect(() => { registrarAlExpirarSesion(logout); }, [logout]);

    // Cierra sesión sola cuando el token vence.
    useEffect(() => {
        if (!usuario?.expira) return undefined;
        const espera = Math.max(usuario.expira - Date.now(), 0);
        const id = setTimeout(logout, espera);
        return () => clearTimeout(id);
    }, [usuario, logout]);

    const valor = useMemo(() => ({ usuario, login, logout }), [usuario, login, logout]);
    return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
    return ctx;
}

export function rutaInicial(rol) {
    return {
        ADMISIONES: '/camas',
        MEDICO: '/admisiones',
        ADMINISTRADOR: '/reporte',
        LABORATORIO: '/laboratorio',
        FARMACEUTICO: '/farmacia',
    }[rol] ?? '/sin-modulo';
}