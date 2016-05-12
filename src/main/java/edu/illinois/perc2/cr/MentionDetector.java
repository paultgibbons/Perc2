package edu.illinois.perc2.cr;

import java.util.List;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;

/**
 * 
 * @author ptgibbo2
 */
public class MentionDetector {
	public MentionDetector() {
		
	}
	/**
	 * Return whether the ith constituent of constituents is a mention
	 * @param constituents
	 * @param i
	 * @return
	 */
	public boolean isMention(List<Constituent> constituents, int i) {
		// TODO Auto-generated method stub
		Constituent c = constituents.get(i);
		if (!c.getLabel().equals("NP")) {
			return false;
		}
		
		return true;
	}
}
