import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  function signOut() {
    logout();
    navigate('/');
  }

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <Link to="/" className="brand" aria-label="QueueLess home">
          <svg className="brand-mark" viewBox="0 0 32 32" aria-hidden="true">
            <path d="M5 8h22v6a2.5 2.5 0 0 0 0 5v5H5v-5a2.5 2.5 0 0 0 0-5z" fill="currentColor" />
          </svg>
          QueueLess
        </Link>
        <nav className="nav" aria-label="Main">
          <NavLink to="/" end>Home</NavLink>
          {user && <NavLink to="/take-token">Take a token</NavLink>}
          {user && <NavLink to="/my-tokens">My tokens</NavLink>}
          {isAdmin && <NavLink to="/admin">Admin</NavLink>}
        </nav>
        <div className="account">
          {user ? (
            <>
              <span className="account-name">{user.name}</span>
              <button type="button" className="btn btn-quiet btn-small" onClick={signOut}>Sign out</button>
            </>
          ) : (
            <>
              <Link className="btn btn-quiet btn-small" to="/login">Sign in</Link>
              <Link className="btn btn-primary btn-small" to="/register">Create account</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
