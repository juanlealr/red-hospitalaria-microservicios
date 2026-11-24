// Prepara un navegador de mentira (jsdom) y empaqueta la app con esbuild,
// para poder hacer clic en la interfaz real desde Node.
import { JSDOM } from 'jsdom';
import { build } from 'esbuild';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const AQUI = dirname(fileURLToPath(import.meta.url));
const FRONT = join(AQUI, '..');

export async function prepararApp(fetchFalso) {
  const paquete = join(AQUI, '.paquete-pruebas.mjs');
  await build({
    entryPoints: [join(AQUI, 'entrada.jsx')],
    bundle: true,
    format: 'esm',
    platform: 'browser',
    outfile: paquete,
    nodePaths: [join(FRONT, 'node_modules')],
    jsx: 'automatic',
    define: {
      'import.meta.env.VITE_API_URL': '"http://localhost:8000"',
      'process.env.NODE_ENV': '"development"',
    },
    loader: { '.js': 'jsx', '.jsx': 'jsx' },
    logLevel: 'warning',
  });

  const dom = new JSDOM('<!doctype html><html><body></body></html>', { url: 'http://localhost:5173/' });
  const almacen = new Map();
  Object.defineProperty(dom.window, 'sessionStorage', {
    value: {
      getItem: (k) => (almacen.has(k) ? almacen.get(k) : null),
      setItem: (k, v) => almacen.set(k, String(v)),
      removeItem: (k) => almacen.delete(k),
    },
    configurable: true,
  });

  global.window = dom.window;
  global.document = dom.window.document;
  Object.defineProperty(global, 'navigator', { value: dom.window.navigator, configurable: true });
  global.sessionStorage = dom.window.sessionStorage;
  global.HTMLElement = dom.window.HTMLElement;
  global.Node = dom.window.Node;
  global.Event = dom.window.Event;
  global.MouseEvent = dom.window.MouseEvent;
  global.getComputedStyle = dom.window.getComputedStyle;
  global.IS_REACT_ACT_ENVIRONMENT = true;
  dom.window.IS_REACT_ACT_ENVIRONMENT = true;
  global.fetch = (url, opciones) => fetchFalso(url, opciones);
  dom.window.fetch = global.fetch;

  const { montar, act } = await import(`file://${paquete}?t=${Date.now()}`);

  const texto = (c) => c.textContent.replace(/\s+/g, ' ');
  const porTexto = (c, selector, etiqueta) =>
    [...c.querySelectorAll(selector)].find((e) => e.textContent.replace(/\s+/g, ' ').includes(etiqueta));

  async function click(elemento) {
    await act(async () => {
      elemento.dispatchEvent(new dom.window.MouseEvent('click', { bubbles: true, cancelable: true }));
    });
  }

  async function escribir(input, valor) {
    const setter = Object.getOwnPropertyDescriptor(dom.window.HTMLInputElement.prototype, 'value').set;
    await act(async () => {
      setter.call(input, valor);
      input.dispatchEvent(new dom.window.Event('input', { bubbles: true }));
    });
  }

  async function entrarComo(rolToken, ruta) {
    almacen.set('redsalud.token', rolToken);
    return montar(ruta);
  }

  return { montar, act, texto, porTexto, click, escribir, entrarComo };
}

export function crearMarcador() {
  const estado = { ok: 0, fallos: 0 };
  const comprobar = (condicion, mensaje) => {
    if (condicion) { estado.ok++; console.log(`  ok    ${mensaje}`); }
    else { estado.fallos++; console.log(`  FALLA ${mensaje}`); }
  };
  return { estado, comprobar };
}
