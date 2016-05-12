package edu.illinois.perc2.re2;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;

/**
 * Wrapper class for information contained in a mention.
 * At the very least, it should contain information for the start and end token IDs of the head word
 * of the mention for use in lookup in the mention's associated TextAnnotation.
 * These token IDs are initialized by looking up character offset values of the mention head word
 * using the associated TextAnnotation for that mention.
 * @author Anjali
 *
 */
public class Mention implements Comparable {
	public ACEDocument document;        // associated ACEDocument
	public ACEEntity entity;			// reader-provided ACEEntity object
	public ACEEntityMention mention;	// reader-provided ACEEntityMention object
	public int headStartId;				// token ID for start of mention head
	public int headEndId;				// token ID for end of mention head
	public int extentStartId;			// token ID for start of mention extent
	public int extentEndId;				// token ID for end of mention extent
	
	/**
	 * Constructs a Mention wrapper given a reader-provided ACEEntity and ACEEntityMention,
	 * and uses the associated (given) TextAnnotation for the mention to compute token IDs
	 * for start/end of head word and extent using reader-provided character offsets.
	 * @param e	reader-provided ACEEntity
	 * @param m	reader-provided ACEEntityMention
	 * @param ta TextAnnotation associated with the given mention
	 */
	public Mention(ACEDocument doc, ACEEntity e, ACEEntityMention m, TextAnnotation ta) {
		document      = doc;
		entity		  = e;
		mention       = m;
		headStartId   = ta.getTokenIdFromCharacterOffset(m.headStart);
		headEndId     = ta.getTokenIdFromCharacterOffset(m.headEnd)+1;
		extentStartId = ta.getTokenIdFromCharacterOffset(m.extentStart);
		extentEndId   = ta.getTokenIdFromCharacterOffset(m.extentEnd)+1;
	}

	@Override
	public int compareTo(Object o) {
		if (o instanceof Mention) {
			Mention om = (Mention) o;
			if (extentStartId < om.extentStartId) return -1;
			if (extentStartId > om.extentStartId) return  1;
			return 0;
		}
		return -1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + extentEndId;
		result = prime * result + extentStartId;
		result = prime * result + headEndId;
		result = prime * result + headStartId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mention other = (Mention) obj;
		if (extentEndId != other.extentEndId)
			return false;
		if (extentStartId != other.extentStartId)
			return false;
		if (headEndId != other.headEndId)
			return false;
		if (headStartId != other.headStartId)
			return false;
		return true;
	}
}
