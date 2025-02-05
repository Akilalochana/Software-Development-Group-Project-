const Features = () => {
  const features = [
    {
      title: "AR-Based Land Analysis",
      description: "Scan and measure your garden space with augmented reality technology",
      icon: "🔍"
    },
    {
      title: "Smart Garden Planning",
      description: "Get personalized crop and seed suggestions based on your garden conditions",
      icon: "🌱"
    },
    {
      title: "Weather-Based Insights",
      description: "Smart irrigation and climate data to optimize your garden care",
      icon: "☀️"
    },
    {
      title: "Garden Health Monitoring",
      description: "Proactive gardening tips and yield predictions for better results",
      icon: "🌿"
    },
    {
      title: "Marketplace",
      description: "Connect with local buyers to sell your surplus produce",
      icon: "🏪"
    }
  ];

  return (
    <section className="features" id="features">
      <div className="container">
        <h2>Key Features</h2>
        <p className="section-desc">
          Discover how our AR technology revolutionizes home gardening
        </p>

        <div className="features-grid">
          {features.map((feature, index) => (
            <div key={index} className="feature-card" style={{ border: "1px solid #ddd", padding: "20px", borderRadius: "10px", boxShadow: "0 0 10px rgba(0, 0, 0, 0.1)" }}>
              <div className="feature-icon">{feature.icon}</div>
              <h3>{feature.title}</h3>
              <p>{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Features;
