// Recorre el flujo clínico completo de laboratorio sobre la interfaz REAL, haciendo
// clic como lo haría una persona, contra un gateway simulado que aplica las mismas
// reglas de roles y los mismos códigos de error que el backend.
//
//   HU-04  el médico solicita un examen
//   HU-09  el alta se rechaza mientras la orden siga pendiente
//   HU-05  el laboratorio registra el resultado y cierra la orden
//   HU-09  cerrada la orden, el alta pasa
//
// Ejecutar con:  npm run test:flujo
import { crearGateway, tokenDe } from './gateway-falso.mjs';
import { crearMarcador, prepararApp } from './entorno.mjs';

const gateway = crearGateway();
const { texto, porTexto, click, escribir, entrarComo } = await prepararApp(gateway.fetchFalso);
const { estado: marcador, comprobar } = crearMarcador();

const entrar = (rol, ruta) => entrarComo(tokenDe(rol), ruta);

// ===================== HU-04: el médico solicita un examen =====================
console.log('\n[1] HU-04 · el médico solicita un examen');
{
  const { contenedor } = await entrar('MEDICO', '/laboratorio/nueva');
  comprobar(texto(contenedor).includes('Solicitar examen'), 'la pantalla de solicitud carga para el MÉDICO');

  const elegir = porTexto(contenedor, 'ul.lista-pacientes button', 'PAC-1001');
  comprobar(Boolean(elegir), 'lista el paciente admitido PAC-1001 tomándolo de admisiones');
  await click(elegir);

  await escribir(contenedor.querySelector('#examen'), 'Hemograma');

  const enviar = porTexto(contenedor, 'button', 'Solicitar examen');
  comprobar(!enviar.disabled, 'el botón se habilita al completar paciente y examen');
  await click(enviar);

  comprobar(texto(contenedor).includes('Examen solicitado'), 'muestra la confirmación de la orden creada');
  comprobar(gateway.estado.ordenes.length === 1, 'se creó exactamente 1 orden');
  comprobar(gateway.estado.ordenes[0].tipoExamen === 'Hemograma', 'la orden lleva el examen indicado');
  comprobar(gateway.estado.ordenes[0].pacienteId === 'PAC-1001', 'la orden lleva el paciente elegido');
  comprobar(gateway.estado.ordenes[0].estado === 'SOLICITADA', 'la orden nace en estado SOLICITADA');
  comprobar(texto(contenedor).includes('el alta médica de este paciente será rechazada'),
    'advierte que la orden bloqueará el alta');
}

// ============ HU-09: el alta se rechaza con la orden pendiente ============
console.log('\n[2] HU-09 · el alta se rechaza mientras la orden sigue pendiente');
{
  const { contenedor } = await entrar('MEDICO', '/admisiones/adm-1');
  comprobar(texto(contenedor).includes('Exámenes de laboratorio'),
    'el detalle de la admisión incluye los exámenes del paciente');
  comprobar(texto(contenedor).includes('Hemograma'), 'aparece el examen recién solicitado');

  await click(porTexto(contenedor, 'button', 'Dar de alta'));
  await click(porTexto(contenedor, 'button', 'Confirmar alta'));

  comprobar(texto(contenedor).includes('órdenes de laboratorio pendientes'),
    'muestra el error 409 devuelto por admisiones');
  comprobar(texto(contenedor).includes('Órdenes de laboratorio pendientes (1)'),
    'detalla cuál es la orden que bloquea el alta');
  comprobar(gateway.estado.admisiones[0].estado === 'ACTIVA', 'la admisión sigue ACTIVA');
}

// ============ HU-05: el laboratorio registra resultado y cierra ============
console.log('\n[3] HU-05 · el laboratorio registra el resultado y cierra la orden');
const ordenId = gateway.estado.ordenes[0].id;
{
  const { contenedor } = await entrar('LABORATORIO', '/laboratorio');
  comprobar(texto(contenedor).includes('Órdenes de laboratorio'), 'el rol LABORATORIO ve su listado');
  comprobar(!texto(contenedor).includes('Solicitar examen'),
    'el laboratorio NO ve el botón de solicitar examen');
  comprobar(Boolean(porTexto(contenedor, 'a', 'Atender')), 'la orden pendiente aparece como «Atender»');
}
{
  const { contenedor } = await entrar('LABORATORIO', `/laboratorio/${ordenId}`);
  comprobar(texto(contenedor).includes('Registrar resultado'), 've el panel para registrar el resultado');
  comprobar(texto(contenedor).includes('Bloqueada por esta orden'), 'indica que el alta está bloqueada');

  comprobar(porTexto(contenedor, 'button', 'Cerrar orden').disabled,
    'no permite cerrar una orden que aún no tiene resultado');

  await escribir(contenedor.querySelector('#valor'), '120');
  await escribir(contenedor.querySelector('#unidad'), 'g/L');
  await click(porTexto(contenedor, 'button', 'Guardar resultado'));

  comprobar(gateway.estado.ordenes[0].estado === 'CON_RESULTADO', 'la orden pasa a CON_RESULTADO');
  comprobar(gateway.estado.ordenes[0].pendiente === true,
    'con resultado SIGUE pendiente: aún bloquea el alta');
  comprobar(texto(contenedor).includes('Falta cerrar la orden'), 'avisa que todavía falta cerrarla');
  comprobar(texto(contenedor).includes('120 g/L'), 'muestra el resultado registrado');

  await click(porTexto(contenedor, 'button', 'Cerrar orden'));
  await click(porTexto(contenedor, 'button', 'Confirmar cierre'));

  comprobar(gateway.estado.ordenes[0].estado === 'CERRADA', 'la orden queda CERRADA');
  comprobar(texto(contenedor).includes('ya puede recibir el alta'), 'avisa que el alta queda libre');
  comprobar(texto(contenedor).includes('No bloqueada por esta orden'), 'ya no bloquea el alta');
}

// ===================== HU-09: ahora el alta sí pasa =====================
console.log('\n[4] HU-09 · con la orden cerrada, el alta se concede');
{
  const { contenedor } = await entrar('MEDICO', '/admisiones/adm-1');
  await click(porTexto(contenedor, 'button', 'Dar de alta'));
  await click(porTexto(contenedor, 'button', 'Confirmar alta'));

  comprobar(gateway.estado.admisiones[0].estado === 'DADA_DE_ALTA', 'la admisión queda DADA_DE_ALTA');
  comprobar(texto(contenedor).includes('Alta registrada'), 'muestra la confirmación del alta');
}

// ===================== Permisos por rol =====================
console.log('\n[5] Permisos por rol (deben coincidir con el gateway)');
{
  const { contenedor } = await entrar('LABORATORIO', '/laboratorio/nueva');
  comprobar(texto(contenedor).includes('No tienes permiso'),
    'LABORATORIO no entra a solicitar exámenes: es del MÉDICO');
}
{
  const { contenedor } = await entrar('ADMISIONES', '/laboratorio');
  comprobar(texto(contenedor).includes('No tienes permiso'), 'ADMISIONES no entra al módulo de laboratorio');
}
{
  const { contenedor } = await entrar('FARMACEUTICO', '/laboratorio');
  comprobar(texto(contenedor).includes('No tienes permiso'), 'FARMACEUTICO tampoco entra');
}
{
  const { contenedor } = await entrar('LABORATORIO', '/');
  comprobar(texto(contenedor).includes('Órdenes de laboratorio'),
    'al iniciar sesión, LABORATORIO aterriza en su pantalla');
  comprobar(!texto(contenedor).includes('Tu módulo aún no está disponible'),
    'el rol LABORATORIO ya no cae en «módulo no disponible»');
}

// ===================== Estado consolidado =====================
console.log('\n[6] Estado consolidado del paciente');
{
  const { contenedor } = await entrar('MEDICO', '/paciente/PAC-1001');
  comprobar(texto(contenedor).includes('Exámenes de laboratorio'), 'incluye la sección de laboratorio');
  comprobar(!texto(contenedor).includes('Pendiente de integración'),
    'ya no muestra el texto de «pendiente de integración»');
  comprobar(texto(contenedor).includes('Hemograma'), 'lista el examen del paciente');
  comprobar(texto(contenedor).includes('120 g/L'), 'muestra su resultado');
  comprobar(texto(contenedor).includes('Ningún examen bloquea el alta'), 'resume el impacto sobre el alta');
}

// ===================== Los demás módulos siguen intactos =====================
console.log('\n[7] Farmacia y admisiones siguen funcionando');
{
  const { contenedor } = await entrar('FARMACEUTICO', '/');
  comprobar(!texto(contenedor).includes('Tu módulo aún no está disponible'),
    'el FARMACEUTICO aterriza en su módulo');
  comprobar(texto(contenedor).includes('Bandeja de farmacia'), 'conserva su enlace de bandeja');
}
{
  const { contenedor } = await entrar('MEDICO', '/farmacia/nueva');
  comprobar(!texto(contenedor).includes('No tienes permiso'), 'el MÉDICO entra a solicitar medicamento');
  comprobar(texto(contenedor).includes('Laboratorio') && texto(contenedor).includes('Farmacia'),
    'el menú del médico conserva Farmacia y suma Laboratorio');
}

console.log(`\n==================== ${marcador.ok} comprobaciones ok, ${marcador.fallos} fallos`);
process.exit(marcador.fallos === 0 ? 0 : 1);
