import React from 'react';
import './index.css';

const Header: React.FC = () => {
  return (
    <header className="header">
      <img src="/public/ceilão.grid.png" alt="Ceilao.Grid Logo" className="logo" />
      <nav>
        <ul>
          <li><a href="#home">Home</a></li>
          <li><a href="#features">Features</a></li>
          <li><a href="#about">About</a></li>
          <li><a href="#contact">Contact us</a></li>
        </ul>
      </nav>
      <a href="#join" className="join-button">Join Us</a>
    </header>
  );
};

export default Header;
