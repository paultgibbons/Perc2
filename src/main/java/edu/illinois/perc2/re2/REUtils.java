package edu.illinois.perc2.re2;

import java.util.List;

import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelation;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelationMention;

public class REUtils {
	public static String getRelationType(String id1, String id2, String mid1, String mid2,
			List<?> relation_list) {
		int found = 0;
		for (int i = 0; i < relation_list.size(); i++) {
			ACERelation current = (ACERelation) relation_list.get(i);
			if (
					(current.relationArgumentList.get(0).id.equals(id1) && current.relationArgumentList.get(1).id.equals(id2))
					||
					(current.relationArgumentList.get(1).id.equals(id1) && current.relationArgumentList.get(0).id.equals(id2))
				) {
//				return current.type; // TODO: do this at the entity mention level
				for (ACERelationMention mention : current.relationMentionList) {
					if ((mention.relationArgumentMentionList.get(0).id.equals(mid1) && mention.relationArgumentMentionList.get(1).id.equals(mid2))
						||
						(mention.relationArgumentMentionList.get(1).id.equals(mid1) && mention.relationArgumentMentionList.get(0).id.equals(mid2))) {
						if (found > 0) System.err.println("We finally found the pair of entity mentions, but not on the first entity pair match.");
						return current.type;
					}
				}
				found += 1;
			}
			
		}
		return "NONE";
	}
}
