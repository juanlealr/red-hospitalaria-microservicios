const CLAVE_TOKEN = 'redsalud.token';
const BASE = import.meta.env.VITE_API_URL ?? '';

export const almacenToken = {
    leer() { try { return sessionStorage.getItem(CLAVE_TOKEN); } catch { return null; } },
    guardar(t) { try { sessionStorage.setItem(CLAVE_TOKEN, t); } catch { /* sin storage */ } },
    borrar() { try { sessionStorage.removeItem(CLAVE_TOKEN); } catch { /* sin storage */ } },
};

export class ErrorApi extends Error {
    constructor(status, mensaje) {
        super(mensaje);
        this.status = status;
    }
}

let alExpirarSesion = () => { };
export function registrarAlExpirarSesion(fn) { alExpirarSesion = fn; }

export async function api(ruta, { metodo = 'GET', cuerpo, params } = {}) {
    const url = new URL(BASE + ruta, window.location.origin);
    if (params) {
        Object.entries(params).forEach(([k, v]) => {
            if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v);
        });
    }

    const headers = {};
    const token = almacenToken.leer();
    if (token) headers.Authorization = `Bearer ${token}`;
    if (cuerpo !== undefined) headers['Content-Type'] = 'application/json';

    let respuesta;
    try {
        respuesta = await fetch(url, {
            method: metodo,
            headers,
            body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
        });
    } catch {
        throw new ErrorApi(0, 'No se pudo conectar con el servidor. Verifica que el gateway esté encendido.');
    }

    if (respuesta.status === 204) return null;

    let datos = null;
    try { datos = await respuesta.json(); } catch { /* respuesta sin cuerpo */ }

    if (!respuesta.ok) {
        if (respuesta.status === 401 && token) alExpirarSesion();
        // admisiones responde { mensaje }; farmacia y otros usan ProblemDetail { detail }
        const detalle = datos?.mensaje ?? datos?.detail ?? datos?.title;
        throw new ErrorApi(respuesta.status, detalle ?? `Error ${respuesta.status}`);
    }
    return datos;
}