import { useState, useEffect } from 'react';

const Hero = () => {
  return (
    <section className="hero">
      <div className="floating-logo">
        <img src="/logo.png" alt="Ceilao.Grid Logo" />
      </div>
      <div className="container">
        <div className="hero-content">
          <span className="pre-heading">SDGP CS 82</span>
          <h1>Smart Garden Planning with AR Technology</h1>
          <p className="hero-description">
            Experience the future of gardening with Ceilao.Grid. Our AR-powered platform 
            helps you design, plan, and maintain your perfect garden with real-time 
            guidance and expert recommendations.
          </p>

          <div className="hero-stats">
            <div className="stat">
              <div className="stat-value">Design</div>
              <div className="stat-label">Your Dream Garden</div>
              <div className="stat-icon">🌿</div>
            </div>
            <div className="stat-divider"></div>
            <div className="stat">
              <div className="stat-value">Visualize</div>
              <div className="stat-label">In Augmented Reality</div>
              <div className="stat-icon">✨</div>
            </div>
            <div className="stat-divider"></div>
            <div className="stat">
              <div className="stat-value">Grow</div>
              <div className="stat-label">With Smart Insights</div>
              <div className="stat-icon">🚀</div>
            </div>
          </div>

          <div className="hero-buttons">
            <button className="btn btn-primary">Try Demo</button>
            <button className="btn btn-secondary">Learn More</button>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Hero;
