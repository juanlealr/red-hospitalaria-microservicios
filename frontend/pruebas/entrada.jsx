// Punto de entrada que esbuild empaqueta para las pruebas: monta la aplicación real.
import { act } from 'react';
import { createRoot } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import App from '../src/App.jsx';
import { AuthProvider } from '../src/features/auth/AuthContext.jsx';

export async function montar(ruta) {
  const contenedor = document.createElement('div');
  document.body.appendChild(contenedor);
  const root = createRoot(contenedor);
  await act(async () => {
    root.render(
      <MemoryRouter initialEntries={[ruta]}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </MemoryRouter>,
    );
  });
  return { contenedor, root };
}

export { act };
