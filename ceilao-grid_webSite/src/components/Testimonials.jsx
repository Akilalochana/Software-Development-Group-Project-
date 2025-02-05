const Testimonials = () => {
  const testimonials = [
    {
      name: "Sarah Johnson",
      role: "Home Gardener",
      text: "Ceilao.Grid transformed my small backyard into a thriving garden. The AI recommendations are spot-on!"
    },
    {
      name: "Michael Chen",
      role: "Urban Farmer",
      text: "The AR land analysis feature saved me hours of planning. Now I'm growing more produce than ever before."
    },
    {
      name: "Emma Rodriguez",
      role: "Community Garden Leader",
      text: "The marketplace feature has connected our community garden with local restaurants. It's a win-win!"
    }
  ];

  return (
    <section className="testimonials">
      <div className="container">
        <h2>What Our Users Say</h2>
        <div className="testimonials-grid">
          {testimonials.map((testimonial, index) => (
            <div key={index} className="testimonial-card">
              <p className="testimonial-text">{testimonial.text}</p>
              <div className="testimonial-author">
                <h4>{testimonial.name}</h4>
                <p>{testimonial.role}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Testimonials;
