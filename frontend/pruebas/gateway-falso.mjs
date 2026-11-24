// Gateway de mentira para las pruebas de interfaz.
//
// Replica las reglas de roles y los códigos de respuesta del backend real
// (gateway/SecurityConfig, laboratorio-service y admisiones-service), de modo que la
// prueba ejercite la interfaz contra el MISMO contrato que expone el sistema.
// No sustituye a las pruebas de los microservicios: comprueba que el frontend
// reacciona bien a lo que ellos devuelven.

const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');

/** Token con la misma forma que emite auth_service: sub, claim "rol" y exp. */
export const tokenDe = (rol) =>
  `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64({
    sub: `${rol.toLowerCase()}@hospital.test`,
    rol,
    exp: Math.floor(Date.now() / 1000) + 3600,
  })}.firma-de-prueba`;

export function crearGateway() {
  const estado = {
    ordenes: [],
    admisiones: [
      {
        id: 'adm-1', pacienteId: 'PAC-1001', sedeActualId: 'SEDE1', servicioActual: 'URGENCIAS',
        estado: 'ACTIVA', fechaIngreso: '2026-11-22T09:00:00', fechaAlta: null,
        medicoTratante: 'Dr. House', motivoIngreso: 'Dolor torácico',
        coberturaPorcentaje: 80, traslados: [],
      },
    ],
    llamadas: [],
  };
  let secuencia = 0;

  const json = (status, cuerpo) => ({
    ok: status >= 200 && status < 300,
    status,
    json: async () => cuerpo,
  });
  const error = (status, mensaje) =>
    json(status, { timestamp: new Date().toISOString(), status, error: 'Error', mensaje });

  const rolDe = (headers) => {
    const auth = headers?.Authorization;
    if (!auth) return null;
    try {
      return JSON.parse(Buffer.from(auth.split(' ')[1].split('.')[1], 'base64url').toString()).rol;
    } catch { return null; }
  };

  const pendientesDe = (pacienteId) =>
    estado.ordenes.filter((o) => o.pacienteId === pacienteId && o.pendiente).length;

  const comoDto = (o) => ({
    id: o.id,
    pacienteId: o.pacienteId,
    tipoExamen: o.tipoExamen,
    estado: o.estado,
    pendiente: o.pendiente,
    fechaSolicitud: o.fechaSolicitud,
    resultado: o.resultado,
  });

  async function fetchFalso(url, opciones = {}) {
    const u = new URL(url);
    const ruta = u.pathname;
    const metodo = opciones.method ?? 'GET';
    const rol = rolDe(opciones.headers);
    const cuerpo = opciones.body ? JSON.parse(opciones.body) : null;
    estado.llamadas.push(`${metodo} ${ruta}`);

    if (ruta === '/api/auth/login' && metodo === 'POST') {
      return json(200, { token: tokenDe(cuerpo.rolSolicitado ?? 'MEDICO') });
    }

    // ---- Reglas de roles del gateway
    if (ruta.startsWith('/api/laboratorio/') && !['MEDICO', 'LABORATORIO'].includes(rol)) {
      return rol ? error(403, 'Sin permiso') : error(401, 'Token ausente');
    }
    if (ruta.startsWith('/api/admisiones') && !['MEDICO', 'ADMISIONES', 'ADMINISTRADOR'].includes(rol)) {
      return rol ? error(403, 'Sin permiso') : error(401, 'Token ausente');
    }

    // ---- Laboratorio
    if (ruta === '/api/laboratorio/ordenes' && metodo === 'GET') {
      const filtro = u.searchParams.get('pacienteId');
      return json(200, estado.ordenes.filter((o) => !filtro || o.pacienteId === filtro).map(comoDto));
    }
    if (ruta === '/api/laboratorio/ordenes' && metodo === 'POST') {
      if (rol !== 'MEDICO') return error(403, 'Solo el médico solicita exámenes');
      if (!cuerpo.pacienteId?.trim()) return error(400, 'El id del paciente es obligatorio');
      if (!cuerpo.tipoExamen?.trim()) return error(400, 'El tipo de examen es obligatorio');
      const orden = {
        id: `orden-${++secuencia}`,
        pacienteId: cuerpo.pacienteId,
        tipoExamen: cuerpo.tipoExamen,
        estado: 'SOLICITADA',
        pendiente: true,
        fechaSolicitud: '2026-11-22T10:15:30.123',
        resultado: null,
      };
      estado.ordenes.push(orden);
      return json(201, comoDto(orden));
    }

    const mResultado = ruta.match(/^\/api\/laboratorio\/ordenes\/([^/]+)\/resultado$/);
    if (mResultado && metodo === 'POST') {
      if (rol !== 'LABORATORIO') return error(403, 'Solo el laboratorio registra resultados');
      const orden = estado.ordenes.find((o) => o.id === mResultado[1]);
      if (!orden) return error(404, 'Orden de laboratorio no encontrada');
      if (orden.estado === 'CERRADA') return error(409, 'No se puede registrar un resultado en una orden cerrada.');
      orden.resultado = { valor: cuerpo.valor, unidad: cuerpo.unidad };
      orden.estado = 'CON_RESULTADO';
      orden.pendiente = true; // sigue bloqueando el alta hasta el cierre
      return json(200, comoDto(orden));
    }

    const mCierre = ruta.match(/^\/api\/laboratorio\/ordenes\/([^/]+)\/cierre$/);
    if (mCierre && metodo === 'PUT') {
      if (rol !== 'LABORATORIO') return error(403, 'Solo el laboratorio cierra órdenes');
      const orden = estado.ordenes.find((o) => o.id === mCierre[1]);
      if (!orden) return error(404, 'Orden de laboratorio no encontrada');
      if (!orden.resultado) return error(409, 'No se puede cerrar una orden sin resultado registrado.');
      orden.estado = 'CERRADA';
      orden.pendiente = false;
      return json(204, null);
    }

    const mPendientes = ruta.match(/^\/api\/laboratorio\/ordenes\/pendientes\/([^/]+)$/);
    if (mPendientes && metodo === 'GET') {
      return json(200, { pacienteId: mPendientes[1], tienePendientes: pendientesDe(mPendientes[1]) > 0 });
    }

    const mOrden = ruta.match(/^\/api\/laboratorio\/ordenes\/([^/]+)$/);
    if (mOrden && metodo === 'GET') {
      const orden = estado.ordenes.find((o) => o.id === mOrden[1]);
      return orden ? json(200, comoDto(orden)) : error(404, 'Orden de laboratorio no encontrada');
    }

    // ---- Admisiones
    if (ruta === '/api/admisiones' && metodo === 'GET') return json(200, estado.admisiones);
    if (ruta === '/api/admisiones/inventario-camas/todas') {
      return json(200, [{ sedeId: 'SEDE1', servicio: 'URGENCIAS', camasTotales: 2, camasOcupadas: 1 }]);
    }

    const mAlta = ruta.match(/^\/api\/admisiones\/([^/]+)\/alta$/);
    if (mAlta && metodo === 'PUT') {
      if (rol !== 'MEDICO') return error(403, 'Solo el médico da el alta');
      const adm = estado.admisiones.find((a) => a.id === mAlta[1]);
      if (!adm) return error(404, 'Admisión no encontrada');
      // HU-09: admisiones-service pregunta al laboratorio por gRPC antes de permitir el alta.
      if (pendientesDe(adm.pacienteId) > 0) {
        return error(409, 'El paciente tiene órdenes de laboratorio pendientes.');
      }
      adm.estado = 'DADA_DE_ALTA';
      adm.fechaAlta = '2026-11-22T12:00:00';
      return json(204, null);
    }

    const mAdm = ruta.match(/^\/api\/admisiones\/([^/]+)$/);
    if (mAdm && metodo === 'GET') {
      const adm = estado.admisiones.find((a) => a.id === mAdm[1]);
      return adm ? json(200, adm) : error(404, 'Admisión no encontrada');
    }

    const mConsolidado = ruta.match(/^\/api\/consultas\/admisiones\/paciente\/([^/]+)\/consolidado$/);
    if (mConsolidado) {
      const pac = decodeURIComponent(mConsolidado[1]);
      return json(200, {
        pacienteId: pac,
        admisiones: estado.admisiones.filter((a) => a.pacienteId === pac).map((a) => ({
          admisionId: a.id, sedeActualId: a.sedeActualId, estado: a.estado,
          fechaIngreso: a.fechaIngreso, fechaAlta: a.fechaAlta, totalTraslados: 0, traslados: [],
        })),
        medicaciones: [],
      });
    }

    return error(404, `Ruta no simulada: ${metodo} ${ruta}`);
  }

  return { estado, fetchFalso };
}
