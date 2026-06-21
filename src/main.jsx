import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import './styles/globals.css'
import { Toaster } from 'sonner'

createRoot(document.getElementById('root')).render(
  <>
    <Toaster position="top-right" richColors />
    <App />
  </>
)