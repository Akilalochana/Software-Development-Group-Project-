import React from 'react';

const Features: React.FC = () => {
  const features = [
    {
      title: "AR Technology",
      description: "Enhance home gardening through AR technology.",
      image: "/public/AR.jpg"
    },
    {
      title: "Garden Planning",
      description: "Acts as a personal garden planning assistant.",
      image: "/public/GP2.jpg"
    },
    {
      title: "Sustainable Production",
      description: "Promotes sustainable food production.",
      image: "/public/SP.jpg"
    },
    {
      title: "Space Optimization",
      description: "Empowers users to optimize gardening spaces.",
      image: "/public/GP.jpg"
    },
    {
      title: "Community Support",
      description: "Supports community well-being and environmental responsibility.",
      image: "/public/CS.jpg"
    },
    {
      title: "Weather Updates",
      description: "Provides real-time weather updates for optimal gardening.",
      image: "/public/WP.jpg"
    }
  ];

  return (
    <section className="features-section relative bg-cover bg-center text-white py-20" style={{ backgroundImage: 'url(/path/to/background-image.jpg)' }}>
      <div className="bg-black bg-opacity-50 p-8 rounded-lg text-center mb-12">
        <h2 className="text-sm font-semibold text-teal-500 mb-2">FEATURES</h2>
        <h2 className="text-4xl font-bold mb-6">Discover Powerful Features</h2>
        <p className="text-lg text-gray-400 mb-12">Ceilão.Grid empowers gardening with innovative features that support, guide, and make gardening enjoyable.</p>
        <div className="grid md:grid-cols-3 gap-12">
          {features.map((feature, index) => (
            <div key={index} className="p-8 rounded-3xl bg-zinc-900 hover:bg-zinc-800 transition-all duration-300 hover:scale-105 hover:shadow-xl">
              <img src={feature.image} alt={feature.title} className="w-full h-32 object-cover mb-4 rounded-lg" />
              <h3 className="text-2xl font-semibold mb-2">{feature.title}</h3>
              <p>{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Features;
