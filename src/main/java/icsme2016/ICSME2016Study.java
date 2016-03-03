package icsme2016;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import org.smellycat.analysis.smells.SmellDescription;

import br.com.metricminer2.MetricMiner2;
import br.com.metricminer2.RepositoryMining;
import br.com.metricminer2.Study;
import br.com.metricminer2.scm.GitRepository;
import br.com.metricminer2.scm.commitrange.Commits;

public class ICSME2016Study implements Study {

	private String path;
	private PrintStream ps;

	public ICSME2016Study(String path, PrintStream ps) {
		this.path = path;
		this.ps = ps;
	}

	public static void main(String[] args) throws FileNotFoundException {
		new MetricMiner2().start(new ICSME2016Study(
			"/Users/mauricioaniche/Desktop/icsme2016/SSP",
			new PrintStream("/Users/mauricioaniche/Desktop/icsme2016/icsme2016.csv")
		));
	}
	
	@Override
	public void execute() {
		Collector collector = new Collector();
		new RepositoryMining()
			.in(GitRepository.singleProject(path))
			.through(Commits.all())
			.startingFromTheBeginning()
			.process(collector)
			.mine();
		
		collector.storeResultsFromPreviousRelease();
		
		List<ClassInfo> result = collector.getFullResult();
		printResults(result);
	}

	private void printResults(List<ClassInfo> result) {
		for(ClassInfo ci : result) {
			
			if(!ci.hasSmells()) printLine(ci, "no", "no");
			else {
				for(SmellDescription smell : ci.getSmells()) {
					printLine(ci, smell.getName(), smell.getDescription());
				}
			}
		}
	}

	private void printLine(ClassInfo ci, String smell, String description) {
		ps.println(
			ci.getPath() + "," +
			ci.getRole() + "," +
			ci.getRelease() + "," +
			ci.getLoc() + "," +
			ci.getCk().getCbo() + "," +
			ci.getCk().getWmc() + "," +
			ci.getCk().getNom() + "," +
			ci.getCk().getRfc() + "," +
			ci.getCk().getLcom() + "," +
			ci.getModifications() + "," +
			ci.getRefactorings() + "," +
			ci.getBugs() + "," +
			smell + "," +
			description
		);
	}

}
