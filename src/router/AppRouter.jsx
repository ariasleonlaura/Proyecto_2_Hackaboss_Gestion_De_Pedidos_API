import { BrowserRouter, Routes, Route } from 'react-router-dom';

// Importamos el Layout (el marco común, como un menú superior)
import Layout from '../components/common/Layout';

// Importamos las páginas
import TerminalPage from '../pages/TerminalPage';
import CocinaPage from '../pages/CocinaPage';
import RecogidaPage from '../pages/RecogidaPage';
import NotFoundPage from '../pages/NotFoundPage';

export default AppRouter;