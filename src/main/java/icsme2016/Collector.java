package icsme2016;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.smellycat.analysis.smells.ArchitecturalRoleRequestor;
import org.smellycat.analysis.smells.Parser;
import org.smellycat.analysis.smells.SmellDescription;
import org.smellycat.analysis.smells.SmellsRequestor;
import org.smellycat.analysis.smells.springmvc.controller.PromiscuousController;
import org.smellycat.analysis.smells.springmvc.controller.SmartController;
import org.smellycat.analysis.smells.springmvc.repository.FatRepository;
import org.smellycat.analysis.smells.springmvc.repository.MultipleQueries;
import org.smellycat.analysis.smells.springmvc.repository.SmartRepository;
import org.smellycat.analysis.smells.springmvc.service.DBQueryingService;
import org.smellycat.architecture.springmvc.SpringMVCArchitecturalRoleVisitor;
import org.smellycat.architecture.springmvc.SpringMVCArchitecture;
import org.smellycat.domain.Repository;
import org.smellycat.domain.SmellyClass;

import br.com.aniche.ck.CK;
import br.com.aniche.ck.CKNumber;
import br.com.aniche.ck.CKReport;
import br.com.metricminer2.domain.Commit;
import br.com.metricminer2.domain.Modification;
import br.com.metricminer2.domain.ModificationType;
import br.com.metricminer2.persistence.PersistenceMechanism;
import br.com.metricminer2.scm.CommitVisitor;
import br.com.metricminer2.scm.SCMRepository;

public class Collector implements CommitVisitor {

	private static final int INITIAL_PHASE = 500;
	private static final int CHUNK_SIZE = 500;
	private int commitCount;
	private List<ClassInfo> currentClasses;
	private List<ClassInfo> fullResult;
	
	private static Logger log = Logger.getLogger(Collector.class);
	
	public Collector() {
		this.commitCount = 0;
		this.fullResult = new ArrayList<>();
		this.currentClasses = new ArrayList<>();
	}
	

	
	@Override
	public void process(SCMRepository repo, Commit commit, PersistenceMechanism writer) {
		this.commitCount++;
		if(stilBeginningPhase()) return;
		
		if(releaseStarts()) {
			log.info("New release starts at #" + commitCount + "(" + commit.getMsg() + ")");
			
			storeResultsFromPreviousRelease();
			processClasses(repo, commit);
		} else {
			processChangeMetrics(repo, commit);
		}

	}

	private String fullPath(String repo, String file) {
		return repo + (repo.endsWith("/")?"":"/") + file;
	}
	
	private void processChangeMetrics(SCMRepository repo, Commit commit) {
		for(Modification m : commit.getModifications()) {
			if(!m.fileNameEndsWith(".java")) continue;
			
			String fileName = m.getType() == ModificationType.ADD ? m.getNewPath() : m.getOldPath();
			Optional<ClassInfo> found = 
					currentClasses.stream().filter(c -> c.getPath().equals(fullPath(repo.getPath(), fileName))).findFirst();
			
			if(!found.isPresent()) {
				log.info("class was not present in the release started: " + fileName);
				log.info(m);
				continue;
			}
			
			ClassInfo theClass = found.get();
			if(m.getType() == ModificationType.RENAME) {
				log.info("Rename: " + m.getOldPath() + " to " + m.getNewPath());
				theClass.setPath(m.getNewPath());
			}

			theClass.wasModified();
			if(fixADefect(commit)) theClass.hadABug();
			if(refactoring(commit)) theClass.wasRefactored();
			
			log.info(theClass);
		}
		
	}

	private boolean refactoring(Commit commit) {
		return commit.getMsg().contains("refactor");
	}

	private boolean fixADefect(Commit commit) {
		return (commit.getMsg().contains("fix") && commit.getMsg().contains("postfix") && commit.getMsg().contains("prefix")) 
			|| commit.getMsg().contains("bug");
	}

	private void processClasses(SCMRepository repo, Commit commit) {
		try {
			repo.getScm().checkout(commit.getHash());
			
			CKReport ckReport = new CK()
				.plug(() -> new SpringMVCArchitecturalRoleVisitor())
				.calculate(repo.getPath());
			
			this.currentClasses = convert(commit.getHash(), ckReport);
			calculateLOC();
			detectSmells(repo, commit);
			
		} catch(Exception e) {
			log.error("Something went bad with git", e);
		} finally {
			repo.getScm().reset();
		}
	}

	public void storeResultsFromPreviousRelease() {
		fullResult.addAll(currentClasses);
		this.currentClasses = new ArrayList<>();
	}

	private void detectSmells(SCMRepository repo, Commit commit) {
		log.info("Searching for code smells in " + commit.getHash());
		Repository smellRepo = runSmellyCat(repo);

		for(SmellyClass smelly : smellRepo.all()) {
			if(smelly.hasAnySmell()) {
				log.info(smelly.getFile() + " has smells");
				
				Optional<ClassInfo> found = 
						currentClasses.stream().filter(c -> c.getPath().equals(smelly.getFile())).findFirst();
				
				if(!found.isPresent()) {
					log.error("class is smelly, but not stored: " + smelly.getFile());
					continue;
				}
				
				ClassInfo theClass = found.get();
				for(SmellDescription smell : smelly.getSmells()) {
					theClass.smells(smell);
				}
			}
		}
		
	}

	private Repository runSmellyCat(SCMRepository repo) {
		Parser parser = new Parser(repo.getPath());
		Repository smellRepo = new Repository();
		parser.execute(new ArchitecturalRoleRequestor(new SpringMVCArchitecture(), smellRepo));
		parser.execute(new SmellsRequestor(smellRepo, 
				new PromiscuousController(), new SmartController(), new SmartRepository(), new FatRepository(), new DBQueryingService(), new MultipleQueries()));
		return smellRepo;
	}

	private void calculateLOC() {
		for(ClassInfo ci : currentClasses) {
			int loc = LOCCounter.count(ci.getPath());
			ci.setLoc(loc);
		}
	}

	private List<ClassInfo> convert(String release, CKReport ckReport) {
		ArrayList<ClassInfo> convertedList = new ArrayList<>();
		for(CKNumber ck : ckReport.all()) {
			convertedList.add(new ClassInfo(ck, release));
		}
		return convertedList;
	}

	private boolean releaseStarts() {
		if(commitCount == INITIAL_PHASE) return true;
		int commitsUpToNow = commitCount - INITIAL_PHASE; 
		return commitsUpToNow % CHUNK_SIZE == 0;
	}

	private boolean stilBeginningPhase() {
		return commitCount < INITIAL_PHASE;
	}

	@Override
	public String name() {
		return "icsme2016";
	}

	public List<ClassInfo> getFullResult() {
		return fullResult;
	}
}
