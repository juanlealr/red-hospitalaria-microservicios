export function sedesDe(inventario) {
    return [...new Set(inventario.map((i) => i.sedeId))].sort();
}

export function serviciosDe(inventario, sedeId) {
    return inventario.filter((i) => i.sedeId === sedeId).map((i) => i.servicio).sort();
}

export const libres = (i) => i.camasTotales - i.camasOcupadas;

export function porcentaje(i) {
    return i.camasTotales === 0 ? 0 : Math.round((i.camasOcupadas * 100) / i.camasTotales);
}

// Para pantallas de gestión (capacidad, reporte): mira qué tan LLENO está el servicio.
export function nivelOcupacion(i) {
    if (i.camasTotales === 0) return { clase: 'neutro', texto: 'Sin camas' };
    const p = porcentaje(i);
    if (p >= 90) return { clase: 'critico', texto: 'Crítica' };
    if (p >= 70) return { clase: 'alto', texto: 'Alta' };
    return { clase: 'ok', texto: 'Disponible' };
}

// Para la pantalla de Disponibilidad: mira qué tanto se PUEDE ADMITIR.
// Así nunca se lee "25 % disponible" cuando en realidad hay 75 % libre.
export function estadoDisponibilidad(i) {
    const l = libres(i);
    if (i.camasTotales === 0) return { clase: 'neutro', texto: 'Sin camas registradas' };
    if (l <= 0) return { clase: 'critico', texto: 'Sin camas libres' };
    if (l / i.camasTotales <= 0.3) return { clase: 'alto', texto: l === 1 ? 'Última cama' : 'Pocas camas' };
    return { clase: 'ok', texto: 'Hay camas' };
}

export const porcentajeLibre = (i) =>
    i.camasTotales === 0 ? 0 : Math.round((libres(i) * 100) / i.camasTotales);

export function resumenPorSede(inventario) {
    const mapa = new Map();
    inventario.forEach((i) => {
        const acum = mapa.get(i.sedeId) ?? { sedeId: i.sedeId, camasTotales: 0, camasOcupadas: 0 };
        acum.camasTotales += i.camasTotales;
        acum.camasOcupadas += i.camasOcupadas;
        mapa.set(i.sedeId, acum);
    });
    return [...mapa.values()].sort((a, b) => a.sedeId.localeCompare(b.sedeId));
}