import { Navigate, Route, Routes } from 'react-router-dom';
import Aviso from './components/Aviso';
import Layout from './components/Layout';
import AdmisionDetallePage from './features/admisiones/AdmisionDetallePage';
import AdmisionesPage from './features/admisiones/AdmisionesPage';
import CamasPage from './features/admisiones/CamasPage';
import CapacidadPage from './features/admisiones/CapacidadPage';
import NuevaAdmisionPage from './features/admisiones/NuevaAdmisionPage';
import ReportePage from './features/admisiones/ReportePage';
import { rutaInicial, useAuth } from './features/auth/AuthContext';
import LoginPage from './features/auth/LoginPage';
import RutaProtegida from './features/auth/RutaProtegida';
import UsuariosPage from './features/auth/UsuariosPage';
import PacienteConsolidadoPage from './features/paciente/PacienteConsolidadoPage';

function Inicio() {
  const { usuario } = useAuth();
  return <Navigate to={rutaInicial(usuario.rol)} replace />;
}

const TODOS_ADMISION = ['ADMISIONES', 'MEDICO', 'ADMINISTRADOR'];

function protegida(roles, pagina) {
  return <RutaProtegida roles={roles}>{pagina}</RutaProtegida>;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      <Route element={<RutaProtegida><Layout /></RutaProtegida>}>
        <Route index element={<Inicio />} />

        <Route path="/camas" element={protegida(TODOS_ADMISION, <CamasPage />)} />
        <Route path="/admisiones" element={protegida(TODOS_ADMISION, <AdmisionesPage />)} />
        <Route path="/admisiones/nueva" element={protegida(['ADMISIONES'], <NuevaAdmisionPage />)} />
        <Route path="/admisiones/:id" element={protegida(TODOS_ADMISION, <AdmisionDetallePage />)} />
        <Route path="/capacidad" element={protegida(['ADMISIONES', 'ADMINISTRADOR'], <CapacidadPage />)} />

        <Route path="/paciente" element={protegida(['MEDICO'], <PacienteConsolidadoPage />)} />
        <Route path="/paciente/:pacienteId" element={protegida(['MEDICO'], <PacienteConsolidadoPage />)} />

        <Route path="/reporte" element={protegida(['ADMINISTRADOR'], <ReportePage />)} />
        <Route path="/usuarios" element={protegida(['ADMINISTRADOR'], <UsuariosPage />)} />

        <Route path="/sin-permiso" element={<Aviso titulo="No tienes permiso para ver esta pantalla" />} />
        <Route path="/sin-modulo" element={<Aviso titulo="Tu módulo aún no está disponible" texto="Las pantallas de tu rol las construye otro integrante del equipo." />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
