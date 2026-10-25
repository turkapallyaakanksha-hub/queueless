import { Link, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import RequireAuth from './components/RequireAuth.jsx';
import Admin from './pages/Admin.jsx';
import Home from './pages/Home.jsx';
import Login from './pages/Login.jsx';
import MyTokens from './pages/MyTokens.jsx';
import Register from './pages/Register.jsx';
import TakeToken from './pages/TakeToken.jsx';

export default function App() {
  return (
    <>
      <Navbar />
      <main className="page">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/take-token" element={<RequireAuth><TakeToken /></RequireAuth>} />
          <Route path="/my-tokens" element={<RequireAuth><MyTokens /></RequireAuth>} />
          <Route path="/admin" element={<RequireAuth admin><Admin /></RequireAuth>} />
          <Route
            path="*"
            element={
              <section className="page-section">
                <h1>Page not found</h1>
                <p>That page does not exist. <Link to="/">Go to the home page</Link>.</p>
              </section>
            }
          />
        </Routes>
      </main>
    </>
  );
}
