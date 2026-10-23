import { BrowserRouter, Routes, Route } from 'react-router-dom';

import Layout from '../components/common/Layout';
import TerminalPage from '../pages/TerminalPage';
import CocinaPage from '../pages/CocinaPage';
import RecogidaPage from '../pages/RecogidaPage';
import NotFoundPage from '../pages/NotFoundPage';

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<TerminalPage />} />
          <Route path="terminal" element={<TerminalPage />} />
          <Route path="cocina" element={<CocinaPage />} />
          <Route path="recogida" element={<RecogidaPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default AppRouter;