import React from "react";
import { Link } from "react-router-dom";
import "./Navbar.css";

const Navbar = () => (
  <nav className="navbar">
    <div className="navbar-links">
      <Link to="/">Home</Link>
      <Link to="/login">Login</Link>
      <Link to="/register">Cadastro</Link>
    </div>
  </nav>
);

export default Navbar; 