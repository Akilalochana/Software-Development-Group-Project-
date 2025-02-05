const TechStack = () => {
  return (
    <section className="tech-stack" id="tech-stack">
      <div className="container">
        <h2>Technology Stack</h2>
        <p className="section-desc">
          Built with cutting-edge technologies for optimal performance
        </p>

        <div className="tech-grid">
          <div className="tech-card">
            <div className="tech-icon">🔮</div>
            <h3>AR Technology</h3>
            <p>Unity AR Foundation</p>
            <p>Vuforia Engine</p>
            <p>ARCore & ARKit</p>
          </div>

          <div className="tech-card">
            <div className="tech-icon">⚛️</div>
            <h3>Frontend</h3>
            <p>React.js</p>
            <p>Three.js</p>
            <p>TailwindCSS</p>
          </div>

          <div className="tech-card">
            <div className="tech-icon">🛠️</div>
            <h3>Backend</h3>
            <p>Node.js</p>
            <p>Express</p>
            <p>MongoDB</p>
          </div>

          <div className="tech-card">
            <div className="tech-icon">🤖</div>
            <h3>AI/ML</h3>
            <p>TensorFlow</p>
            <p>Computer Vision</p>
            <p>Plant Recognition</p>
          </div>
        </div>
      </div>
    </section>
  );
};

export default TechStack;
