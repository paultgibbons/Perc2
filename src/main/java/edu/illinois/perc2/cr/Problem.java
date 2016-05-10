package edu.illinois.perc2.cr;

import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;

/**
 * 
 * @author ptgibbo2
 */
public class Problem {
	public ACEEntityMention mention;
	public SLProblem sp;
	public Problem(ACEEntityMention mention, SLProblem sp) {
		this.sp = sp;
		this.mention = mention;
	}
}
