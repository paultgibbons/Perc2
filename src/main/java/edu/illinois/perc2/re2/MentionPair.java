package edu.illinois.perc2.re2;

import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;

public class MentionPair {
	ACEDocument document;
	Mention m1;
	Mention m2;
	String gold_relation;
	
	public MentionPair(ACEDocument document, Mention m1, Mention m2, String gold_relation) {
		this.document = document;
		this.m1 = m1;
		this.m2 = m2;
		this.gold_relation = gold_relation;
	}
}
