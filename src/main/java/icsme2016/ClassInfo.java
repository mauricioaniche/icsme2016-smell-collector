package icsme2016;

import java.util.HashSet;
import java.util.Set;

import org.smellycat.analysis.smells.SmellDescription;

import br.com.aniche.ck.CKNumber;

public class ClassInfo {

	private String path;
	private int loc;
	private Set<SmellDescription> smells;
	private int modifications;
	private int bugs;
	private int refactorings;
	private int role;
	private String release;
	private CKNumber ck;
	
	public ClassInfo(CKNumber ck, String release) {
		this.ck = ck;
		this.path = ck.getFile();
		this.release = release;
		this.role = ck.getSpecific("role");
		this.smells = new HashSet<>();
	}
	
	public CKNumber getCk() {
		return ck;
	}

	public String getPath() {
		return path;
	}

	public int getLoc() {
		return loc;
	}

	public void setLoc(int loc) {
		this.loc = loc;
	}

	public void smells(SmellDescription smell) {
		smells.add(smell);
	}

	public String getRelease() {
		return release;
	}

	public Set<SmellDescription> getSmells() {
		return smells;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void wasModified() {
		this.modifications++;
		
	}

	public void hadABug() {
		this.bugs++;
	}

	public void wasRefactored() {
		this.refactorings++;
	}

	public int getModifications() {
		return modifications;
	}

	public int getBugs() {
		return bugs;
	}

	public int getRefactorings() {
		return refactorings;
	}


	public int getRole() {
		return role;
	}

	

	@Override
	public String toString() {
		return "ClassInfo [path=" + path + ", loc=" + loc + ", smells=" + smells + ", modifications=" + modifications
				+ ", bugs=" + bugs + ", refactorings=" + refactorings + ", role=" + role + ", release=" + release + "]";
	}

	public boolean hasSmells() {
		return !smells.isEmpty();
	}
	
}
