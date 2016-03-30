/*******************************************************************************
 * University of Illinois/NCSA Open Source License
 * Copyright (c) 2010, 
 *
 * Developed by:
 * The Cognitive Computations Group
 * University of Illinois at Urbana-Champaign
 * http://cogcomp.cs.illinois.edu/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal with the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimers in the documentation and/or other materials provided with the distribution.
 * Neither the names of the Cognitive Computations Group, nor the University of Illinois at Urbana-Champaign, nor the names of its contributors may be used to endorse or promote products derived from this Software without specific prior written permission.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE SOFTWARE.
 *     
 *******************************************************************************/
package edu.illinois.perc2.re;

import edu.illinois.cs.cogcomp.sl.core.AbstractInferenceSolver;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;

/**
 * An implementation of the Viterbi algorithm
 * @author kchang10
 */
public class InferenceSolver extends
		AbstractInferenceSolver {

	private static final long serialVersionUID = 1L;	
	protected Lexiconer lm = null;
	
	public InferenceSolver(Lexiconer lm) {
		this.lm = lm;
	}
	
	@Override
	public IStructure getLossAugmentedBestStructure(
			WeightVector wv, IInstance input, IStructure gold)
			throws Exception {
		//Output goldLabeledSeq = (Output) gold;
		Input pair = (Input) input;
		
		float bestScore = Integer.MIN_VALUE;
		for(int i = 0; i < lm.getNumOfLabels(); i++) {
			float score = wv.get(i);
		}
		
		//if (wv.dotProduct(pair.))
		
		if (wv.get(0) == 0.0) {
			// types do not match
			return new Output(lm.getFeatureId("NONE"));
		}
		if (wv.get(1) > 50.0) {
			// if words are too far apart
			return new Output(lm.getFeatureId("NONE"));
		}
		
		return new Output(lm.getFeatureId("NONE"));

	}
	
	@Override
	public float getLoss(IInstance ins, IStructure goldStructure,  IStructure structure){
		Output goldLabeledSeq = (Output) goldStructure;
		Output predictedLabeledSeq = (Output) structure;
		if (goldLabeledSeq.relationType == (predictedLabeledSeq.relationType)) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public IStructure getBestStructure(WeightVector wv,
			IInstance input) throws Exception {
		return getLossAugmentedBestStructure(wv, input, null);
	}

	@Override
	public Object clone(){
		return new InferenceSolver(lm);
	}
}
