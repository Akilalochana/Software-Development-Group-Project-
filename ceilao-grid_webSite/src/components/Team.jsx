const Team = () => {
  const teamMembers = [
    {
      name: "Team Member 1",
      role: "Team Leader / Full Stack Developer",
      github: "#",
      linkedin: "#"
    },
    {
      name: "Team Member 2",
      role: "Frontend Developer / UI Designer",
      github: "#",
      linkedin: "#"
    },
    {
      name: "Team Member 3",
      role: "Backend Developer / Database",
      github: "#",
      linkedin: "#"
    },
    {
      name: "Team Member 4",
      role: "AR Developer / Mobile",
      github: "#",
      linkedin: "#"
    },
    {
      name: "Team Member 5",
      role: "ML Engineer / Computer Vision",
      github: "#",
      linkedin: "#"
    },
    {
      name: "Team Member 6",
      role: "QA Engineer / Documentation",
      github: "#",
      linkedin: "#"
    }
  ];

  return (
    <section className="team" id="team">
      <div className="container">
        <h2>Our Team</h2>
        <p className="section-desc">
          The talented developers behind Ceilao.Grid
        </p>

        <div className="team-grid">
          {teamMembers.map((member, index) => (
            <div key={index} className="team-card">
              <div className="team-image">👤</div>
              <h3>{member.name}</h3>
              <p className="role">{member.role}</p>
              <div className="team-links">
                <a href={member.github} target="_blank" rel="noopener noreferrer">GitHub</a>
                <a href={member.linkedin} target="_blank" rel="noopener noreferrer">LinkedIn</a>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Team;
