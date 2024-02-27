/* AIGenVersion(4a62e38e, actdsl.prompt-5309f1ad, greeterexample.txt-775cd036) */

const Act = () => {
  const [currentAudience, setCurrentAudience] = useState("exuberant tech wizards");

  return (
    <div>
      <h1 id="mainHeadline" className="primary blue big">Hello you {currentAudience}!</h1>
      <p id="joke" className="green">Why did the AI dev tool break up with the debugging tool? It had too many issues.</p>
      <section style={{ fontWeight: "bold" }}>
        <input value={currentAudience} onChange={(e) => setCurrentAudience(e.target.value)} />
      </section>
    </div>
  );
};

export default Act;
