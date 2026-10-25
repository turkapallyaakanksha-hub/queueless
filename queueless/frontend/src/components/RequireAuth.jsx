import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';

export default function RequireAuth({ children, admin = false }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  if (admin && user.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }
  return children;
}
