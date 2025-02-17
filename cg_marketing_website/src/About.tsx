import React from 'react';

const About = () => {
  return (
    <section className="py-32 bg-gray-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 className="text-5xl font-bold mb-8">About Ceilão.Grid</h2>
        <p className="text-xl text-gray-700 mb-12 max-w-2xl mx-auto">
          Ceilão.Grid is a cutting-edge distributed computing platform designed to empower individuals and businesses to harness the power of collaborative computing. Our mission is to provide a seamless and efficient environment for processing data, enabling faster and more reliable results.
        </p>
        <p className="text-xl text-gray-700 mb-12 max-w-2xl mx-auto">
          With our innovative technology, users can connect their devices and contribute to a shared network, optimizing performance and reducing costs. Join us in transforming the future of computing.
        </p>
        <button className="bg-black text-white px-8 py-4 rounded-full text-lg font-medium hover:bg-gray-800 transition-all duration-300">
          Learn More
        </button>
      </div>
    </section>
  );
};

export default About;