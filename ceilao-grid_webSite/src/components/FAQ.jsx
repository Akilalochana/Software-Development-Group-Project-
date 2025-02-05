import { useState } from 'react';

const FAQ = () => {
  const [activeIndex, setActiveIndex] = useState(null);

  const faqs = [
    {
      question: "How does the AR land analysis work?",
      answer: "Our AR technology uses your device's camera to scan and measure your garden space. Simply point your camera at your garden area, and the app will create a detailed 3D map with measurements and suggestions for optimal plant placement."
    },
    {
      question: "What kind of gardening recommendations does Ceilao.Grid provide?",
      answer: "Based on factors like soil type, sunlight exposure, local climate, and your gardening goals, we provide suitable plant suggestions, optimal planting times, and care instructions tailored to your specific garden conditions."
    },
    {
      question: "Is the marketplace feature available worldwide?",
      answer: "The marketplace is currently available in select regions. We're continuously expanding our coverage to connect more gardeners with local buyers and create sustainable local food networks."
    },
    {
      question: "Do I need special equipment to use Ceilao.Grid?",
      answer: "No special equipment needed! Just a smartphone with a camera for the AR features. Our app works with most modern Android and iOS devices."
    }
  ];

  const toggleFAQ = (index) => {
    setActiveIndex(activeIndex === index ? null : index);
  };

  return (
    <section className="faq">
      <div className="container">
        <h2>Frequently Asked Questions</h2>
        <div className="faq-list">
          {faqs.map((faq, index) => (
            <div
              key={index}
              className={`faq-item ${activeIndex === index ? 'active' : ''}`}
              onClick={() => toggleFAQ(index)}
            >
              <div className="faq-question">
                <h3>{faq.question}</h3>
              </div>
              <div className="faq-answer">
                <p>{faq.answer}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default FAQ;
