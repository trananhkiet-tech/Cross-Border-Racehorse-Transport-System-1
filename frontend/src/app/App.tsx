import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './AuthContext';
import { ToastProvider } from './ToastContext';
import { AppRoutes } from '../routes/AppRoutes';

export function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </ToastProvider>
  );
}
