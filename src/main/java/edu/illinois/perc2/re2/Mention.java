package edu.illinois.perc2.re2;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;

public class Mention {
	public ACEEntity entity;
	public ACEEntityMention mention;
	public int headStartId;
	public int headEndId;
	public int extentStartId;
	public int extentEndId;
	
	public Mention(ACEEntity e, ACEEntityMention m, TextAnnotation ta) {
		entity		  = e;
		mention       = m;
		headStartId   = ta.getTokenIdFromCharacterOffset(m.headStart);
		headEndId     = ta.getTokenIdFromCharacterOffset(m.headEnd)+1;
		extentStartId = ta.getTokenIdFromCharacterOffset(m.extentStart);
		extentEndId   = ta.getTokenIdFromCharacterOffset(m.extentEnd)+1;
	}
}
