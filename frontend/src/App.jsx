import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import EnConstruccion from './components/EnConstruccion';
import RutaProtegida from './features/auth/RutaProtegida';
import LoginPage from './features/auth/LoginPage';
import { rutaInicial, useAuth } from './features/auth/AuthContext';
import CamasPage from './features/admisiones/CamasPage';

function Inicio() {
  const { usuario } = useAuth();
  return <Navigate to={rutaInicial(usuario.rol)} replace />;
}

const TODOS_ADMISION = ['ADMISIONES', 'MEDICO', 'ADMINISTRADOR'];

function protegida(roles, titulo) {
  return (
    <RutaProtegida roles={roles}>
      <EnConstruccion titulo={titulo} />
    </RutaProtegida>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<RutaProtegida><Layout /></RutaProtegida>}>
        <Route index element={<Inicio />} />
        <Route path="/camas" element={<RutaProtegida roles={TODOS_ADMISION}><CamasPage /></RutaProtegida>} />
        <Route path="/admisiones" element={protegida(TODOS_ADMISION, 'Admisiones')} />
        <Route path="/admisiones/nueva" element={protegida(['ADMISIONES'], 'Nueva admisión')} />
        <Route path="/paciente" element={protegida(['MEDICO'], 'Paciente consolidado')} />
        <Route path="/capacidad" element={protegida(['ADMISIONES', 'ADMINISTRADOR'], 'Capacidad de camas')} />
        <Route path="/reporte" element={protegida(['ADMINISTRADOR'], 'Reporte de ocupación')} />
        <Route path="/usuarios" element={protegida(['ADMINISTRADOR'], 'Usuarios')} />
        <Route path="/sin-permiso" element={<EnConstruccion titulo="No tienes permiso para ver esta pantalla" />} />
        <Route path="/sin-modulo" element={<EnConstruccion titulo="Tu módulo aún no está disponible" />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
