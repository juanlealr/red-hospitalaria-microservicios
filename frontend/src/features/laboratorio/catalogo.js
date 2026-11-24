// Los tres estados del dominio (EstadoOrdenLaboratorio en laboratorio-service).
// Ojo: CON_RESULTADO TODAVÍA bloquea el alta médica; solo CERRADA la libera.
export const ESTADOS = {
    SOLICITADA: {
        texto: 'Solicitada',
        clase: 'badge-alto',
        ayuda: 'El laboratorio aún no registra el resultado.',
    },
    CON_RESULTADO: {
        texto: 'Con resultado',
        clase: 'badge-proceso',
        ayuda: 'El resultado ya está registrado; falta que el laboratorio cierre la orden.',
    },
    CERRADA: {
        texto: 'Cerrada',
        clase: 'badge-ok',
        ayuda: 'Orden terminada. Ya no bloquea el alta médica.',
    },
};

export function estadoDe(orden) {
    return ESTADOS[orden?.estado] ?? { texto: orden?.estado ?? '—', clase: 'badge-neutro', ayuda: '' };
}

export const esPendiente = (orden) => orden?.pendiente === true;

export function resumen(ordenes) {
    return {
        total: ordenes.length,
        solicitadas: ordenes.filter((o) => o.estado === 'SOLICITADA').length,
        conResultado: ordenes.filter((o) => o.estado === 'CON_RESULTADO').length,
        cerradas: ordenes.filter((o) => o.estado === 'CERRADA').length,
        pendientes: ordenes.filter(esPendiente).length,
    };
}

export const resultadoTexto = (orden) =>
    orden?.resultado ? `${orden.resultado.valor} ${orden.resultado.unidad}` : '—';

// Sugerencias para el médico; puede escribir cualquier otro examen.
export const EXAMENES_FRECUENTES = [
    'Hemograma',
    'Glucosa',
    'Perfil lipídico',
    'Creatinina',
    'Uroanálisis',
    'Proteína C reactiva',
    'TSH',
    'Electrolitos',
    'Tiempos de coagulación',
];

// Sugerencias para el laboratorio al registrar el resultado.
export const UNIDADES_FRECUENTES = ['g/L', 'mg/dL', 'mmol/L', 'U/L', '%', 'mg/L', 'mUI/L', 'seg'];
