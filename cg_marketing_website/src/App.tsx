import React, { useEffect, useRef } from 'react';
import { Grid } from 'lucide-react';
import Header from './Header';
import Features from './Features';


function useIntersectionObserver(ref: React.RefObject<HTMLElement>, options = {}) {
  useEffect(() => {
    if (!ref.current) return;

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
        }
      });
    }, { threshold: 0.1, ...options });

    observer.observe(ref.current);

    return () => {
      if (ref.current) {
        observer.unobserve(ref.current);
      }
    };
  }, [ref, options]);
}

function useParallax() {
  useEffect(() => {
    const handleScroll = () => {
      document.querySelectorAll('.parallax').forEach((element) => {
        const speed = element.getAttribute('data-speed') || '0.5';
        const yPos = -(window.scrollY * Number(speed));
        (element as HTMLElement).style.transform = `translateY(${yPos}px)`;
      });
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);
}

function App() {
  const heroRef = useRef<HTMLDivElement>(null);
  const featuresRef = useRef<HTMLDivElement>(null);
  const statsRef = useRef<HTMLDivElement>(null);
  const ctaRef = useRef<HTMLDivElement>(null);

  useIntersectionObserver(heroRef);
  useIntersectionObserver(featuresRef);
  useIntersectionObserver(statsRef);
  useIntersectionObserver(ctaRef);
  useParallax();

  return (
    <div className="min-h-screen bg-black text-white">
      <Header />
      {/* Hero Section */}
      <section className="relative h-screen flex items-center justify-center overflow-hidden">
        <video autoPlay loop muted className="absolute inset-0 w-full h-full object-cover">
          <source src="/public/bgvideo.mp4" type="video/mp4" />
          Your browser does not support the video tag.
        </video>
        <div className="absolute inset-0">
          <img 
            src="https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&q=80"
            alt="Grid Background"
            className="w-full h-full object-cover opacity-50 parallax"
            data-speed="0.5"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black to-transparent"></div>
        </div>
        <div ref={heroRef} className="relative z-10 text-center space-y-6 max-w-4xl mx-auto px-4 fade-in">
          <h1 className="text-6xl md:text-8xl font-bold tracking-tight">
            Ceilão.Grid
          </h1>
          <p className="text-xl md:text-2xl text-gray-300">
          Grow the Future at Home!
          </p>
          <button className="bg-white text-black px-8 py-4 rounded-full text-lg font-medium hover:bg-gray-200 transition-all duration-300 hover:scale-105">
            Learn More
          </button>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-32 bg-black">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <Features />
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-32 bg-zinc-900">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div ref={statsRef} className="grid md:grid-cols-3 gap-12 text-center stagger-in">
            {[
              { number: "99+.99%", label: "Uptime" },
              { number: "10x", label: "Faster Processing" },
              { number: "24/7", label: "Support" }
            ].map((stat, index) => (
              <div key={index} className="transform transition-all duration-300 hover:scale-105">
                <div className="text-5xl font-bold mb-2">{stat.number}</div>
                <div className="text-gray-400 text-lg">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-32 bg-black relative overflow-hidden">
        <div className="absolute inset-0">
          <img 
            src="https://images.unsplash.com/photo-1639322537228-f710d846310a?auto=format&fit=crop&q=80"
            alt="Abstract Grid"
            className="w-full h-full object-cover opacity-20 parallax"
            data-speed="0.3"
          />
        </div>
        <div ref={ctaRef} className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center scale-in">
          <h2 className="text-5xl font-bold mb-8">Ready to Transform Your Gardening Experience? </h2>
          <p className="text-xl text-gray-400 mb-12 max-w-2xl mx-auto">
            Join the next generation of distributed gardening with Ceilão.Grid.
          </p>
          <button className="bg-white text-black px-8 py-4 rounded-full text-lg font-medium hover:bg-gray-200 transition-all duration-300 hover:scale-105">
            Get Started Now
          </button>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-zinc-900 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row justify-between items-center">
          <img src="/public/ceilão.grid.png" alt="Ceilao.Grid Logo" className="logo" />
            <div className="flex space-x-8 text-sm text-gray-400">
              <a href="#" className="hover:text-white transition-colors duration-300">Privacy</a>
              <a href="#" className="hover:text-white transition-colors duration-300">Terms</a>
              <a href="#" className="hover:text-white transition-colors duration-300">Contact</a>
            </div>
          </div>
          <div className="mt-8 pt-8 border-t border-zinc-800 text-center text-sm text-gray-400">
            &copy; {new Date().getFullYear()} Ceilão.Grid. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;