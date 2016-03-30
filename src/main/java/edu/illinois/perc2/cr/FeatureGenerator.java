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
package edu.illinois.perc2.cr;

import edu.illinois.cs.cogcomp.lbjava.classify.FeatureVector;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.FeatureVectorBuffer;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.SparseFeatureVector;

public class FeatureGenerator extends AbstractFeatureGenerator {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * This function returns a feature vector \Phi(x,y) based on an instance-structure pair.
	 * 
	 * @return Feature Vector \Phi(x,y), where x is the input instance and y is the
	 *         output structure
	 */
   
	public FeatureGenerator() {		

	}

	
	@Override
	public IFeatureVector getFeatureVector(IInstance x, IStructure y) {
		Output o = (Output) y;
		Input ins = (Input) x;
		float fmatch = (float) ((ins.type1.equals(ins.type2)) ? 1.0 : -1.0);
		float yval = o.areCoReferencing?1.0f:-1.0f;
		
		FeatureVectorBuffer fvb = new FeatureVectorBuffer();
		
		if (o.areCoReferencing) {
			fvb.addFeature(0,fmatch);
			fvb.addFeature(1,0.0f);
			fvb.addFeature(2,ins.distance);
			fvb.addFeature(3,0.0f);
		} else {
			fvb.addFeature(0,0.0f);
			fvb.addFeature(1,fmatch);
			fvb.addFeature(2,0.0f);
			fvb.addFeature(3,ins.distance);
		}
		
		return fvb.toFeatureVector(false);
	}

}
