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


import edu.illinois.cs.cogcomp.sl.core.IInstance;

/**
 * An implementation of IInstance for part-of-speech task 
 * @author kchang10
 */
public class Input implements IInstance {

	public final String arg1id;
	public final String arg2id;
	public final String type1;
	public final String type2;
	public final String head1;
	public final String head2;
	public final String extent1;
	public final String extent2;
	public final Boolean isSubstr;
	public final int distance;
	int hashCode = 0;
	
	public Input(String id1, String id2, String type1, String type2, int distance, String head1, String head2, String extent1, String extent2, Boolean isSubstr) {
        this.arg1id = id1;
        this.arg2id = id2;
        this.type1 = type1;
        this.type2 = type2;
        this.distance = distance;
        this.head1 = head1;
        this.head2 = head2;
        this.extent1 = extent1;
        this.extent2 = extent2;
        this.isSubstr = isSubstr;
        
        //hashCode = ... 
	}

	@Override
	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append(arg1id);
		sb.append(' ');
		sb.append(arg1id);
		sb.append(' ');
		sb.append(type1);
		sb.append(' ');
		sb.append(type2);
		sb.append(' ');
		sb.append(distance);	
		sb.append(' ');
		sb.append(head1);
		sb.append(' ');
		sb.append(head2);
		sb.append(' ');
		sb.append(extent1);
		sb.append(' ');
		sb.append(extent2);
		sb.append(' ');
		sb.append(isSubstr);
		

		return sb.toString();
	} 

}