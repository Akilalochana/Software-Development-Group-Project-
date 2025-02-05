const Footer = () => {
  return (
    <footer className="footer">
      <div className="container">
        <div className="footer-content">
          <div className="footer-brand">
            <h3>Ceilao.Grid</h3>
            <p>Smart Garden Planning with AR Technology</p>
          </div>
          
          <div className="footer-links">
            <div className="link-group">
              <h4>Quick Links</h4>
              <a href="#features">Features</a>
              <a href="#team">Team</a>
              <a href="#contact">Contact</a>
            </div>
            
            <div className="link-group">
              <h4>Connect</h4>
              <a href="https://github.com" target="_blank" rel="noopener noreferrer">GitHub</a>
              <a href="https://linkedin.com" target="_blank" rel="noopener noreferrer">LinkedIn</a>
            </div>
          </div>
        </div>
        
        <div className="footer-bottom">
          <p>&copy; {new Date().getFullYear()} Ceilao.Grid. All rights reserved.</p>
          <p>SDGP Project 2024 - IIT</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
