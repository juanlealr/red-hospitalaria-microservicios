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

export function nivelOcupacion(i) {
    if (i.camasTotales === 0) return { clase: 'neutro', texto: 'Sin camas' };
    const p = porcentaje(i);
    if (p >= 90) return { clase: 'critico', texto: 'Crítica' };
    if (p >= 70) return { clase: 'alto', texto: 'Alta' };
    return { clase: 'ok', texto: 'Disponible' };
}

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