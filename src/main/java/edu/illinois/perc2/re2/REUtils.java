package edu.illinois.perc2.re2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelation;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelationArgumentMention;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelationMention;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.util.FeatureVectorBuffer;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.perc2.re2.Features.FeatureEnum;
import edu.illinois.perc2.re2.Features.FeatureSetEnum;

public class REUtils {
	public static FeatureSetEnum[] feature_sets = { FeatureSetEnum.WORD, FeatureSetEnum.ENTITY_TYPE, FeatureSetEnum.MENTION_LEVEL,
			FeatureSetEnum.CHUNKING };
	
	// full list of implemented features
	public static FeatureEnum[] features = { FeatureEnum.WM1, FeatureEnum.HM1, FeatureEnum.WM2, FeatureEnum.HM2,   
			FeatureEnum.HM12, FeatureEnum.WBNULL, FeatureEnum.WBFL, FeatureEnum.WBF, FeatureEnum.WBL, FeatureEnum.WBO, 
			FeatureEnum.BM1F, FeatureEnum.BM1L, FeatureEnum.AM2F, FeatureEnum.AM2L, 
			FeatureEnum.ET12, FeatureEnum.ML12,
			FeatureEnum.MB, FeatureEnum.WB, FeatureEnum.M1gtM2, FeatureEnum.M1ltM2, FeatureEnum.ETM1gtM2, FeatureEnum.ETM1ltM2,
			FeatureEnum.HMM1gtM2, FeatureEnum.HMM1ltM2,
			FeatureEnum.CPHBNULL, FeatureEnum.CPHBFL, FeatureEnum.CPHBF, FeatureEnum.CPHBL, FeatureEnum.CPHBO,
			FeatureEnum.CPHBM1F, FeatureEnum.CPHBM1L, FeatureEnum.CPHAM2F, FeatureEnum.CPHAM2L, FeatureEnum.CPP, FeatureEnum.CPPH
	};
	
	// NER features
	public static FeatureEnum[] features_ner = { FeatureEnum.WM1, FeatureEnum.WM2, 
			FeatureEnum.WBNULL, FeatureEnum.WBFL, FeatureEnum.WBF, FeatureEnum.WBL, FeatureEnum.WBO,
			FeatureEnum.BM1F, FeatureEnum.BM1L, FeatureEnum.AM2F, FeatureEnum.AM2L,
			FeatureEnum.ET12, 
			FeatureEnum.MB, FeatureEnum.WB, FeatureEnum.M1gtM2, FeatureEnum.M1ltM2, FeatureEnum.ETM1gtM2, FeatureEnum.ETM1ltM2,
			FeatureEnum.HMM1gtM2, FeatureEnum.HMM1ltM2,
			FeatureEnum.CPHBNULL, FeatureEnum.CPHBFL, FeatureEnum.CPHBF, FeatureEnum.CPHBL, FeatureEnum.CPHBO
	};
	
	// word features
	public static FeatureEnum[] features_words = { FeatureEnum.WM1, FeatureEnum.HM1, FeatureEnum.WM2, FeatureEnum.HM2,
			FeatureEnum.HM12, FeatureEnum.WBNULL, FeatureEnum.WBFL, FeatureEnum.WBF, FeatureEnum.WBL, FeatureEnum.WBO,
			FeatureEnum.BM1F, FeatureEnum.BM1L, FeatureEnum.AM2F, FeatureEnum.AM2L
	};
	
	// entity type and mention level features
	public static FeatureEnum[] features_et = { FeatureEnum.ET12 };
	public static FeatureEnum[] features_ml = { FeatureEnum.ML12 };
	
	// overlap features
	public static FeatureEnum[] features_overlap = { FeatureEnum.MB, FeatureEnum.WB, FeatureEnum.M1gtM2, FeatureEnum.M1ltM2, 
			 FeatureEnum.ETM1gtM2, FeatureEnum.ETM1ltM2, FeatureEnum.HMM1gtM2, FeatureEnum.HMM1ltM2};
	
	// chunking features
	public static FeatureEnum[] features_chunking = { FeatureEnum.CPHBNULL, FeatureEnum.CPHBFL, 
			FeatureEnum.CPHBM1F, FeatureEnum.CPHBM1L, FeatureEnum.CPHAM2F, FeatureEnum.CPHAM2L,
			FeatureEnum.CPP, FeatureEnum.CPPH };
	
	// dependency features
	public static FeatureEnum[] features_dependency = {};
	
	// parse features
	public static FeatureEnum[] features_parse = {};
	
	// bag-of-words features
	public static FeatureEnum[] bow_features = { FeatureEnum.WM1, FeatureEnum.WM2, FeatureEnum.WBO, FeatureEnum.CPHBO };
	
	// possible null-valued features
	public static FeatureEnum[] null_features = { FeatureEnum.WBFL, FeatureEnum.WBF, FeatureEnum.WBL, FeatureEnum.WBO, 
			FeatureEnum.BM1F, FeatureEnum.BM1L, FeatureEnum.AM2F, FeatureEnum.AM2L,
			FeatureEnum.CPHBFL, FeatureEnum.CPHBF, FeatureEnum.CPHBL, FeatureEnum.CPHBO, FeatureEnum.CPP, FeatureEnum.CPPH };
	
	static HashMap<String,Integer> labelCounts;
	static HashMap<ACEDocument, List<Mention>> mentions;
	
	static int unmapped_mentions = 0; // count of mentions unable to be mapped to gold mentions
	
	
	/**
	 * Gets the list of FeatureEnums as specified by the set types given as input arguments.
	 * If no arguments are provided, this returns the manually-defined set of features ({@link REUtils.features}).
	 * @param types Types of feature sets to include
	 * @return List of FeatureEnums to be used as features
	 */
	public static List<FeatureEnum> getFeatureSet(FeatureSetEnum... types) {
		List<FeatureEnum> fts = new ArrayList<FeatureEnum>();
		if (types.length == 0) {
			fts.addAll(Arrays.asList(features));
			return fts;
		}
		
		for (FeatureSetEnum type : types) {
			switch (type) {
			case WORD:
				fts.addAll(Arrays.asList(features_words));
				break;
			case ENTITY_TYPE:
				fts.addAll(Arrays.asList(features_et));
				break;
			case MENTION_LEVEL:
				fts.addAll(Arrays.asList(features_ml));
				break;
			case OVERLAP:
				fts.addAll(Arrays.asList(features_overlap));
				break;
			case CHUNKING:
				fts.addAll(Arrays.asList(features_chunking));
				break;
			case DEPENDENCY:
				fts.addAll(Arrays.asList(features_dependency));
				break;
			case PARSE:
				fts.addAll(Arrays.asList(features_parse));
				break;
			default:
				System.err.println("Unimplemented feature set: "+type.name());
				break;
			}
		}
		
		return fts;
	}

	/**
	 * Gets relation type between two entity mentions, respecting Arg-1 Arg-2 argument order (and ignoring
	 * other role types).
	 * @param m1 First mention
	 * @param m2 Second mention
	 * @param relations List of all relations in the document
	 * @return relation between m1 and m2; "NONE" if one does not exist
	 */
	public static String getRelationType(Mention m1, Mention m2, List<?> relations) {
		String eid1 = m1.entity.id;
		String eid2 = m2.entity.id;
		String mid1 = m1.mention.id;
		String mid2 = m2.mention.id;

		for (int i = 0; i < relations.size(); i++) {		
			ACERelation relation = (ACERelation) relations.get(i);
			
			// ignore ordering at the entity level
			if ((relation.relationArgumentList.get(0).id.equals(eid1) && relation.relationArgumentList.get(1).id.equals(eid2)) ||
				(relation.relationArgumentList.get(1).id.equals(eid1) && relation.relationArgumentList.get(0).id.equals(eid2))) {
				
				for (ACERelationMention mention : relation.relationMentionList) {
					List<ACERelationArgumentMention> argument_mentions = mention.relationArgumentMentionList;
					
					// find indices of Arg-1 and Arg-2 in the relation argument mention list
					int arg1i = -1, arg2i = -1;
					for (int j = 0; j < argument_mentions.size(); j++) {
						ACERelationArgumentMention ram = argument_mentions.get(j);
						if (ram.role.equals("Arg-1")) arg1i = j;
						if (ram.role.equals("Arg-2")) arg2i = j;
					}
					
					// safety check if Arg-1 or Arg-2 wasn't found
					if (arg1i == -1 || arg2i == -1) continue;
					
					String arg1 = argument_mentions.get(arg1i).id;
					String arg2 = argument_mentions.get(arg2i).id;
					
					// check that first mention's id matches Arg-1's and second mention's id matches Arg-2's
					if ((arg1.equals(mid1) && arg2.equals(mid2))) {
//						System.err.println("Match!");
						return relation.type;
					}
				}
			}
			
		}
		return Features.NONE;
	}
	
	/**
	 * Gets the mapped relation by mapping predicted mention pair to gold mentions, then querying for
	 * the gold relation between the mapped mentions.
	 * @param ri MultiClassInstance with prediced mentions
	 * @return String representation of gold label; "NONE" if mapping fails
	 */
	public static String getMappedRelation(MultiClassInstance ri) {
		ACEDocument ner_doc  = ri.document;
		ACEDocument gold_doc = MultiClassIOManager.filenames_gold.get(MultiClassIOManager.ner_filenames.get(ner_doc));
		
		Mention m1 = ri.m1; // predicted mention 1
		Mention m2 = ri.m2; // predicted mention 2
		
		Mention gold_m1 = getMappedMention(m1, gold_doc); // mapped mention 1
		Mention gold_m2 = getMappedMention(m2, gold_doc); // mapped mention 2
		
		// successful map
		if (gold_m1 != null && gold_m2 != null) return Features.REL+getRelationType(gold_m1, gold_m2, gold_doc.aceAnnotation.relationList);
		else { // map failure
			if (gold_m1 == null) unmapped_mentions++;
			if (gold_m2 == null) unmapped_mentions++;
			return Features.REL+Features.NONE;
		}
	}
	
	/**
	 * Gets the mapped gold mention from the given predicted mention. Mention boundaries must be contained within
	 * each other.
	 * @param ner_mention Predicted mention
	 * @param gold_doc Gold document containing this mention
	 * @return Mapped gold mention, if one exists
	 */
	public static Mention getMappedMention(Mention ner_mention, ACEDocument gold_doc) {
		List<Mention> gold_mentions = mentions.get(gold_doc);
		
		for (Mention gold_mention : gold_mentions) {
			if ((ner_mention.extentStartId >= gold_mention.extentStartId && ner_mention.extentEndId <= gold_mention.extentEndId) ||
					(gold_mention.extentStartId >= ner_mention.extentStartId && gold_mention.extentEndId <= gold_mention.extentEndId)) {
						return gold_mention;
					}
		}
		
		return null;
	}
	
	/**
	 * Helper method which checks if two mentions are found in the same sentence(s) according to automatic sentence boundaries.
	 * @param ta TextAnnotation of the document
	 * @param m1 First mention
	 * @param m2 Second mention
	 * @return true if mentions occur in same sentence(s); false otherwise
	 */
	private static boolean mentionsInSameSentence(TextAnnotation ta, Mention m1, Mention m2) {
		int m1hs = m1.headStartId;  // m1 head start
		int m1he = m1.headEndId;    // m1 head end
		int m2hs = m2.headStartId;  // m2 head start
		int m2he = m2.headEndId;    // m2 head end
		
		// get token indices within the range of head start/end
		Set<Integer> m1tokens = new HashSet<Integer>();
		Set<Integer> m2tokens = new HashSet<Integer>();
		
		for (int k = m1hs; k < m1he; k++) m1tokens.add(k);
		for (int k = m2hs; k < m2he; k++) m2tokens.add(k);
		
		List<Sentence> m1sens = ta.getSentenceFromTokens(m1tokens); // sentence(s) containing m1 head
		List<Sentence> m2sens = ta.getSentenceFromTokens(m2tokens); // sentence(s) containing m2 head
		
		// check that the mention pair occurs within the same sentence(s)
		for (Sentence m1sen : m1sens) {
			for (Sentence m2sen : m2sens) {
				if (m1sen.getSentenceId() == m2sen.getSentenceId()) return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Helper method which computes the number of mentions in between two given mentions.
	 * @param m1 First mention
	 * @param m2 Second mention
	 * @return Number of mentions between the first and second mentions
	 */
	private static int getMentionsBetween(Mention m1, Mention m2) {
		int idx1 = mentions.get(m1.document).indexOf(m1);
		int idx2 = mentions.get(m2.document).indexOf(m2);
		
		int between = 0;
		for (int i = idx1+1; i < idx2; i++) {
			Mention m = mentions.get(m1.document).get(i);
			if (m.extentStartId > m1.extentEndId && m.extentEndId < m2.extentStartId) between++;
		}
		
		return between;
	}
	
	/**
	 * Helper method which finds the list of chunks between two given mentions.
	 * @param chunk Shallow parse view
	 * @param m1 First mention
	 * @param m2 Second mention
	 * @return List of chunks between the two mentions
	 */
	private static List<Constituent> getChunksBetween(SpanLabelView chunk, Mention m1, Mention m2) {
		int m1es = m1.extentStartId; // m1 extent start
		int m1ee = m1.extentEndId;   // m1 extent end
		int m2es = m2.extentStartId; // m2 extent start
		int m2ee = m2.extentEndId;   // m2 extent end
		
		List<Constituent> constituents = new ArrayList<Constituent>();
		if (m1ee < m2es)  constituents = chunk.getConstituentsCoveringSpan(m1ee+1, m2es);
		if (m2ee < m1es)  constituents = chunk.getConstituentsCoveringSpan(m2ee+1, m1es);
		
		return constituents;
	}
	
	private static List<Constituent> getChunksBefore(TextAnnotation ta, SpanLabelView chunk, Mention m) {
		int mes = m.extentStartId;   // mention extent start
		int sID = mes-1;
		List<Constituent> constituents = new ArrayList<Constituent>();
		
		while (constituents.size() < 2 || sID > 0) {
			constituents = chunk.getConstituentsCoveringSpan(sID, mes);
			sID--;
		}
		return constituents;
	}
	
	private static List<Constituent> getChunksAfter(TextAnnotation ta, SpanLabelView chunk, Mention m) {
		int mee = m.extentEndId;     // mention extent end
		int eID = mee+1;
		List<Constituent> constituents = new ArrayList<Constituent>();
		
		while (mee+1 < ta.size() && (constituents.size() < 2 || eID < ta.size())) {
			constituents = chunk.getConstituentsCoveringSpan(mee+1, eID);
			eID++;
		}
		
		return constituents;
	}
	
	/**
	 * Computes the string feature of feature label+value given an ordered pair of mentions and a requested feature type.
	 * Most features will return a size-1 list of the feature label+value for use in a one-hot feature vector initialization.
	 * In bag-of-words feature cases, this method will return a list of such feature label+values.
	 * @param ta TextAnnotation of the document
	 * @param m1 First mention
	 * @param m2 Second mention
	 * @param feature Feature to be computed
	 * @return String feature label+value to be used to index into a Lexiconer
	 */
	public static List<String> computeFeature(TextAnnotation ta, Mention m1, Mention m2, FeatureEnum feature) {
		int m1hs = m1.headStartId;   // m1 head start
		int m1he = m1.headEndId;     // m1 head end
		int m2hs = m2.headStartId;   // m2 head start
		int m2he = m2.headEndId;     // m2 head end
		
		int m1es = m1.extentStartId; // m1 extent start
		int m1ee = m1.extentEndId;   // m1 extent end
		int m2es = m2.extentStartId; // m2 extent start
		int m2ee = m2.extentEndId;   // m2 extent end
		
		List<String> values = new ArrayList<String>();
		SpanLabelView chunk = (SpanLabelView) ta.getView(ViewNames.SHALLOW_PARSE);
		List<Constituent> constituents = null;
		String path = "";
		
		switch (feature) {
		case HM1:      // head word 1
			return Arrays.asList(Features.HM1+String.join(" ",ta.getTokensInSpan(m1hs,m1he)));
			
		case HM2:      // head word 2
			return Arrays.asList(Features.HM2+String.join(" ",ta.getTokensInSpan(m2hs,m2he)));
			
		case HM12:     // combination of head words 1 and 2
			return Arrays.asList(Features.HM12+String.join(" ",ta.getTokensInSpan(m1hs,m1he))+" "+String.join(" ",ta.getTokensInSpan(m2hs,m2he)));
		
		case WM1:      // bag-of-words 1
			for (String s : ta.getTokensInSpan(m1es,m1ee)) values.add(Features.WM1+s);
			return values;
			
		case WM2:      // bag-of-words 2
			try {
				ta.getTokensInSpan(m2es, m2ee);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("m2es:"+m2es);
				System.err.println("m2ee:"+m2ee);
				System.err.println(ta.getToken(m2es));
				System.err.println(ta.getToken(m2ee));
			}
			for (String s : ta.getTokensInSpan(m2es,m2ee)) values.add(Features.WM2+s);
			return values;
		
		case ET12:     // combination of entity types
			return Arrays.asList(Features.ET12+m1.entity.type+" "+m2.entity.type);
		
		case ML12:     // combination of mention levels
			return Arrays.asList(Features.ML12+m1.mention.type+" "+m2.mention.type);
		
		case BM1F:     // first word before M1
			if (m1es-1 < 0) return Arrays.asList(Features.BM1F+Features.NULL);
			return Arrays.asList(Features.BM1F+ta.getToken(m1es-1));
		
		case BM1L:     // second word before M1
			if (m1es-2 < 0) return Arrays.asList(Features.BM1L+Features.NULL);
			return Arrays.asList(Features.BM1L+ta.getToken(m1es-2));
		
		case AM2F:     // first word after M2
			if (m2ee+1 > ta.size()-1) return Arrays.asList(Features.AM2F+Features.NULL);
			return Arrays.asList(Features.AM2F+ta.getToken(m2ee+1));
		
		case AM2L:     // second word after M2
			if (m2ee+2 > ta.size()-1) return Arrays.asList(Features.AM2L+Features.NULL);
			return Arrays.asList(Features.AM2L+ta.getToken(m2ee+2));
		
		case WBNULL:   // when no word in between
			if (m1ee+1 == m2es || m2ee+1 == m1es) return Arrays.asList(Features.WBNULL+Features.TRUE);
			return Arrays.asList(Features.WBNULL+Features.FALSE);
		
		case WBFL:     // the only word in between when only one word in between
			if (m1ee+2 == m2es) return Arrays.asList(Features.WBFL+ta.getToken(m1ee+1));
			if (m2ee+2 == m1es) return Arrays.asList(Features.WBFL+ta.getToken(m2ee+1));
			return Arrays.asList(Features.WBFL+Features.NULL);
		
		case WBF:       // first word in between when at least two words in between
			if (m1ee+3 <= m2es) return Arrays.asList(Features.WBF+ta.getToken(m1ee+1));
			if (m2ee+3 <= m1es) return Arrays.asList(Features.WBF+ta.getToken(m2ee+1));
			return Arrays.asList(Features.WBF+Features.NULL);
		
		case WBL:      // last word in between when at least two words in between
			if (m1ee+3 <= m2es) return Arrays.asList(Features.WBL+ta.getToken(m2es-1));
			if (m2ee+3 <= m1es) return Arrays.asList(Features.WBL+ta.getToken(m1es-1));
			return Arrays.asList(Features.WBL+Features.NULL);
		
		case WBO:      // other words in between when at least three words in between
			if (m1ee+4 <= m2es) {
				for (String s : ta.getTokensInSpan(m1ee+2,m2es-1)) values.add(Features.WBO+s);
				return values;
			}
			if (m2ee+4 <= m1es) {
				for (String s : ta.getTokensInSpan(m2ee+2,m1es-1)) values.add(Features.WBO+s);
				return values;
			}
			return Arrays.asList(Features.WBO+Features.NULL);
		
		case MB:       // number of mentions in between
			return Arrays.asList(Features.MB+Math.max(getMentionsBetween(m1,m2), getMentionsBetween(m2,m1)));
					
		case WB:	   // number of words in between
			if (m1ee < m2es) return Arrays.asList(Features.WB+ta.getTokensInSpan(m1ee+1,m2es).length);
			if (m2ee < m1es) return Arrays.asList(Features.WB+ta.getTokensInSpan(m2ee+1,m1es).length);
			return Arrays.asList(Features.WB+"0");
			
		case M1gtM2:   // M2 is included in M1
			if (m2es > m1es && m2ee < m1ee) return Arrays.asList(Features.M1gtM2+Features.TRUE);
			return Arrays.asList(Features.M1gtM2+Features.FALSE);
			
		case M1ltM2:   // M1 is included in M2
			if (m1es > m2es && m1ee < m2ee) return Arrays.asList(Features.M1ltM2+Features.TRUE);
			return Arrays.asList(Features.M1ltM2+Features.FALSE);
			
		case ETM1gtM2: // ET12+M1>M2
			if (m2es > m1es && m2ee < m1ee) return Arrays.asList(Features.ETM1gtM2+m1.entity.type+" "+m2.entity.type+" "+Features.TRUE);
			return Arrays.asList(Features.ETM1gtM2+m1.entity.type+" "+m2.entity.type+" "+Features.FALSE);
			
		case ETM1ltM2: // ET12+M1<M2
			if (m1es > m2es && m1ee < m2ee) return Arrays.asList(Features.ETM1ltM2+m1.entity.type+" "+m2.entity.type+" "+Features.TRUE);
			return Arrays.asList(Features.ETM1ltM2+m1.entity.type+" "+m2.entity.type+" "+Features.FALSE);
			
		case HMM1gtM2: // HM12+M1>M2
			if (m2es > m1es && m2ee < m1ee) return Arrays.asList(Features.HMM1gtM2+String.join(" ",ta.getTokensInSpan(m1hs,m1he))+" "+String.join(" ",ta.getTokensInSpan(m2hs,m2he))+" 1");
			return Arrays.asList(Features.HMM1gtM2+String.join(" ",ta.getTokensInSpan(m1hs,m1he))+" "+String.join(" ",ta.getTokensInSpan(m2hs,m2he))+" 0");
			
		case HMM1ltM2: // HM12+M1<M2
			if (m1es > m2es && m1ee < m2ee) return Arrays.asList(Features.HMM1ltM2+String.join(" ",ta.getTokensInSpan(m1hs,m1he))+" "+String.join(" ",ta.getTokensInSpan(m2hs,m2he))+" 1");
			return Arrays.asList(Features.HMM1ltM2+String.join(" ",ta.getTokensInSpan(m1hs,m1he))+" "+String.join(" ",ta.getTokensInSpan(m2hs,m2he))+" 0");
			
		case CPHBNULL: // no phrase in between
			constituents = getChunksBetween(chunk, m1, m2);
			if (constituents.size() == 0) return Arrays.asList(Features.CPHBNULL+Features.TRUE);
			return Arrays.asList(Features.CPHBNULL+Features.FALSE);
			
		case CPHBFL:   // only phrase head when only one phrase in between
			constituents = getChunksBetween(chunk, m1, m2);
			if (constituents.size() != 1) return Arrays.asList(Features.CPHBFL+Features.NULL);
			Constituent phrase = constituents.get(0);
			return Arrays.asList(Features.CPHBFL+phrase.toString());
			
		case CPHBF:   // first phrase head in between when at least two phrases in between
			constituents = getChunksBetween(chunk, m1, m2);
			if (constituents.size() < 2) return Arrays.asList(Features.CPHBF+Features.NULL);
			Constituent first = constituents.get(0);
			return Arrays.asList(Features.CPHBF+first.toString());
			
		case CPHBL:   // last phrase head in between when at least two phrases in between
			constituents = getChunksBetween(chunk, m1, m2);
			if (constituents.size() < 2) return Arrays.asList(Features.CPHBL+Features.NULL);
			Constituent last = constituents.get(constituents.size()-1);
			return Arrays.asList(Features.CPHBL+last.toString());
			
		case CPHBO:   // other phrase heads in between when at least three phrases in between
			constituents = getChunksBetween(chunk, m1, m2);
			if (constituents.size() < 3) return Arrays.asList(Features.CPHBO+Features.NULL);
			for (int i = 1; i < constituents.size()-1; i++) values.add(Features.CPHBO+constituents.get(i).toString());
			return values;
			
		case CPHBM1F: // first phrase head before M1
			constituents = getChunksBefore(ta, chunk, m1);
			if (constituents.size() == 0) return Arrays.asList(Features.CPHBM1F+Features.NULL);
			return Arrays.asList(Features.CPHBM1F+constituents.get(constituents.size()-1).toString());
			
		case CPHBM1L: // second phrase head before M1
			constituents = getChunksBefore(ta, chunk, m1);
			if (constituents.size() < 2) return Arrays.asList(Features.CPHBM1L+Features.NULL);
			return Arrays.asList(Features.CPHBM1L+constituents.get(constituents.size()-2).toString());
			
		case CPHAM2F: // first phrase head after M2
			constituents = getChunksAfter(ta, chunk, m2);
			if (constituents.size() == 0) return Arrays.asList(Features.CPHAM2F+Features.NULL);
			return Arrays.asList(Features.CPHAM2F+constituents.get(0).toString());
			
		case CPHAM2L: // second phrase head after M2
			constituents = getChunksAfter(ta, chunk, m2);
			if (constituents.size() < 2) return Arrays.asList(Features.CPHAM2L+Features.NULL);
			return Arrays.asList(Features.CPHAM2L+constituents.get(1).toString());
			
		case CPP:     // path of phrase labels connecting the two mentions
			constituents = getChunksBetween(chunk, m1, m2);
			if (constituents.isEmpty()) return Arrays.asList(Features.CPP+Features.NULL);
			for (Constituent c : constituents) path += c.getLabel()+" ";
			return Arrays.asList(Features.CPP+path);
			
		case CPPH:    // path of phrase labels augmented with head words, if at most two phrases in between
			constituents = getChunksBetween(chunk, m1, m2);
			if (constituents.isEmpty() || constituents.size() > 2) return Arrays.asList(Features.CPPH+Features.NULL);
			for (Constituent c : constituents) path += c.getLabel()+" "+c.toString()+" ";
			return Arrays.asList(Features.CPPH+path);
			
		default:       // unimplemented
			System.err.println("Unimplemented feature id:"+feature.name());
			return null;
		}
	}
	
	/**
	 * Gets the UNK feature label+value for a requested feature.
	 * @param feature Requested feature type
	 * @return Feature label+value with UNK token
	 */
	public static String getUNKFeature(FeatureEnum feature) {
		return Features.getFeatureString(feature)+Features.UNK;
	}
	
	/**
	 * Adds the NULL feature label+value for a requested feature to the given Lexiconer.
	 * NULL feature values are only valid for a subset of feature types.
	 * @param lm Lexiconer to be used
	 * @param feature Feature to be used
	 */
	public static String getNULLFeature(FeatureEnum feature) {
		return Features.getFeatureString(feature)+Features.NULL;
	}
	
	/**
	 * Adds computed feature values for a given pair of mentions to an id-value representation for use in constructing
	 * a sparse feature vector.
	 * @param ta TextAnnotation to be used
	 * @param lm Initialized Lexiconer for use in indexing
	 * @param idxList Running ID list for sparse feature vector representation
	 * @param valList Running value list for sparse feature vector representation
	 * @param feature Feature to compute
	 * @param m1 First mention
	 * @param m2 Second mention
	 */
	public static void addFeatureToSparseVector(TextAnnotation ta, Lexiconer lm, List<Integer> idxList, List<Double> valList, FeatureEnum feature, Mention m1, Mention m2) {
		List<String> feature_values = computeFeature(ta, m1, m2, feature); // get feature values
		
		// replace unseen feature-value pairs with UNK values
		for (int i = 0; i < feature_values.size(); i++) {
			if (!lm.containFeature(feature_values.get(i))) feature_values.set(i, getUNKFeature(feature));
		}
		
		// one-hot features
		if (!Arrays.asList(bow_features).contains(feature)) {
			for (String fv : feature_values) {
				idxList.add(lm.getFeatureId(fv));
				valList.add(1.0);
			}
			return;
		}
			
		// bag-of-words features
		List<Integer> tempidx = new ArrayList<Integer>();
		List<Double>  tempval = new ArrayList<Double>();

		for (String fv : feature_values) {
			int id = lm.getFeatureId(fv);

			if (!tempidx.contains(id)) {
				tempidx.add(id);
				tempval.add(0.0);
			}

			int idx = tempidx.indexOf(id);
			tempval.set(idx, tempval.get(idx)+1.0);
		}

		idxList.addAll(tempidx);
		valList.addAll(tempval);
		return;
	}
	
	/**
	 * Initializes a per-document sorted mention list, sorted by start index of the mention's extent.
	 * @param docs List of documents to create sorted mention lists for
	 * @param annotator Initialized annotator service
	 */
	public static void initializeSortedMentionList(List<ACEDocument> docs, AnnotatorService annotator) {
		System.out.println("["+(new Date(System.currentTimeMillis()))+"] Initializing sorted mention list ...");
		mentions = new HashMap<ACEDocument, List<Mention>>();
		
		for(ACEDocument doc : docs) {
			try{
				if (doc == null) continue;
				TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create the TextAnnotation
//				System.out.println(doc.contentRemovingTags+"\n\n");
				mentions.put(doc, new ArrayList<Mention>());		
				
				List<?> entities  = doc.aceAnnotation.entityList;	  // entities

				// for each entity
				for (int i = 0; i < entities.size(); i++) {

					// entities
					ACEEntity e = ((ACEEntity) entities.get(i));     // entity
					List<ACEEntityMention> em = e.entityMentionList; // entity mention list

					// for each mention
					for (ACEEntityMention am : em) {

						Mention m = new Mention(doc, e, am, ta); // mention

						// System.out.println(MultiClassIOManager.ner_filenames.get(doc));

						if (!mentions.get(doc).contains(m)) mentions.get(doc).add(m);
					}
				}
				
				Collections.sort(mentions.get(doc));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Initializes a per-document sorted mention list, sorted by start index of the mention's extent.
	 * @param docs List of mention pairs to create sorted mention lists for
	 * @param annotator Initialized annotator service
	 */
	public static void initializeSortedMentionListFromMentionPairs(List<MentionPair> mention_pairs, AnnotatorService annotator) {
		System.out.println("["+(new Date(System.currentTimeMillis()))+"] Initializing sorted mention list from mention pairs ...");
		mentions = new HashMap<ACEDocument, List<Mention>>();
		
		for (MentionPair mention_pair : mention_pairs) {
			try {
				ACEDocument doc = mention_pair.document;
				
				if (doc == null) continue;
				
				TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create the TextAnnotation
//				System.out.println(doc.contentRemovingTags+"\n\n");
				if (!mentions.containsKey(doc)) mentions.put(doc, new ArrayList<Mention>());		
				
				List<?> entities  = doc.aceAnnotation.entityList;	  // entities

				// for each entity
				for (int i = 0; i < entities.size(); i++) {

					// entities
					ACEEntity e = ((ACEEntity) entities.get(i));     // entity
					List<ACEEntityMention> em = e.entityMentionList; // entity mention list

					// for each mention
					for (ACEEntityMention am : em) {

						Mention m = new Mention(doc, e, am, ta); // mention

						// System.out.println(MultiClassIOManager.ner_filenames.get(doc));

						if (!mentions.get(doc).contains(m)) mentions.get(doc).add(m);
					}
				}
				
				Collections.sort(mentions.get(doc));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Traverses through given list of (training) documents and adds all instances of feature label+values into the
	 * given Lexiconer.
	 * @param docs Training documents
	 * @param lm Lexiconer to be used
	 * @param annotator Initialized annotator service
	 */
	public static void addFeaturesToLexiconer(List<ACEDocument> docs, Lexiconer lm, AnnotatorService annotator, boolean ner_features, boolean binary_labels) {
		System.out.println("["+(new Date(System.currentTimeMillis()))+"] Adding features to lexicon ...");
		labelCounts = new HashMap<String,Integer>();
		
		if (lm.isAllowNewFeatures()) {
			List<FeatureEnum>  fts = null;
			if (!ner_features) fts = getFeatureSet(feature_sets);
			else               fts = Arrays.asList(features_ner);
			
			System.out.print("Features to be used: \n\t");
			for (FeatureEnum feature : fts)	System.out.print(feature.name()+"  ");
			System.out.println();
			
			// add UNK tokens for all features
			for (FeatureEnum feature : fts) lm.addFeature(getUNKFeature(feature));
			for (FeatureEnum feature : null_features) lm.addFeature(getNULLFeature(feature));
			
			int totalRel = 0, nonNONERel = 0;
			
			for(ACEDocument doc : docs) {
				try{
					if (doc == null) continue;
					TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create the TextAnnotation
					
					annotator.addView(ta, ViewNames.SHALLOW_PARSE);
							
					List<?> entities  = doc.aceAnnotation.entityList;	  // entities
					List<?> relations = doc.aceAnnotation.relationList;	  // relations

					// for each pair of entities
					for (int i = 0; i < entities.size(); i++) {
						for (int j = 0; j < entities.size(); j++) {
							// entities
							ACEEntity e1 = ((ACEEntity) entities.get(i)); // first entity
							ACEEntity e2 = ((ACEEntity) entities.get(j)); // second entity

							List<ACEEntityMention> e1m = e1.entityMentionList; // mention list of first entity
							List<ACEEntityMention> e2m = e2.entityMentionList; // mention list of second entity
							
							// for each combination of mention pairs
							for (ACEEntityMention am1 : e1m) {
								for (ACEEntityMention am2 : e2m) {
									
									Mention m1 = new Mention(doc, e1, am1, ta); // first mention
									Mention m2 = new Mention(doc, e2, am2, ta); // second mention
									
									// add features and label if mention pair occurs within the same sentence(s)
									if (mentionsInSameSentence(ta,m1,m2)) {										
										for (FeatureEnum feature : fts) {
											for (String s : computeFeature(ta,m1,m2,feature)) {
												lm.addFeature(s);
//												System.out.println("Added feature "+s);
											}
										}

										// relation label
										String relation = REUtils.getRelationType(m1, m2, relations);
										if (!binary_labels) lm.addLabel(Features.REL+relation);
										else {
											if (!relation.equals(Features.NONE)) lm.addLabel(Features.REL+Features.HASREL);
											else lm.addLabel(Features.REL+Features.NONE);
										}
										
										if (!relation.equals(Features.NONE)) nonNONERel++;
										totalRel++;
										
										if (!binary_labels) {
											if (!labelCounts.containsKey(relation)) labelCounts.put(relation, 0);
											labelCounts.put(relation,labelCounts.get(relation)+1);
										}
										else {
											String r = (!relation.equals(Features.NONE) ? Features.HASREL : Features.NONE);
											if (!labelCounts.containsKey(r)) labelCounts.put(r, 0);
											labelCounts.put(r,labelCounts.get(r)+1);
										}
									}
								}
							}
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			System.out.println(nonNONERel+" non-NONE relations of "+totalRel+" total relations.");
			System.out.println("Relation counts: ");
			for (String k : labelCounts.keySet()) {
				System.out.println("\t"+k+": "+labelCounts.get(k)+"\t"+labelCounts.get(k)/((double) totalRel)+"\t"+1/(labelCounts.get(k)/((double) totalRel)));
			}
			
			System.out.println();
		}
	}

	/**
	 * Traverses through given list of (training) mention pairs and adds all instances of feature label+values into the
	 * given Lexiconer.
	 * @param docs Training documents
	 * @param lm Lexiconer to be used
	 * @param annotator Initialized annotator service
	 */
	public static void addFeaturesToLexiconerFromMentionPairs(List<MentionPair> mention_pairs, Lexiconer lm, AnnotatorService annotator, boolean ner_features) {
		System.out.println("["+(new Date(System.currentTimeMillis()))+"] Adding features to lexicon from mention pairs ...");
		labelCounts = new HashMap<String,Integer>();
		
		if (lm.isAllowNewFeatures()) {
			List<FeatureEnum>  fts = null;
			if (!ner_features) fts = getFeatureSet(feature_sets);
			else               fts = Arrays.asList(features_ner);
			
			System.out.print("Features to be used: \n\t");
			for (FeatureEnum feature : fts)	System.out.print(feature.name()+"  ");
			System.out.println();
			
			// add UNK tokens for all features
			for (FeatureEnum feature : fts) lm.addFeature(getUNKFeature(feature));
			for (FeatureEnum feature : null_features) lm.addFeature(getNULLFeature(feature));
			
			int totalRel = 0, nonNONERel = 0;
			
			for (MentionPair mention_pair : mention_pairs) {
				try {
					ACEDocument doc = mention_pair.document;
					
					if (doc == null) continue;
					TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create the TextAnnotation
					
					annotator.addView(ta, ViewNames.SHALLOW_PARSE);
					
					List<?> relations = doc.aceAnnotation.relationList;	  // relations
										
					Mention m1 = mention_pair.m1; // first mention
					Mention m2 = mention_pair.m2; // second mention

					for (FeatureEnum feature : fts) {
						for (String s : computeFeature(ta,m1,m2,feature)) {
							lm.addFeature(s);
//							System.out.println("Added feature "+s);
						}
					}

					// relation label
					String relation = REUtils.getRelationType(m1, m2, relations);
					lm.addLabel(Features.REL+relation);
										
					if (!relation.equals(Features.NONE)) nonNONERel++;
					totalRel++;

					if (!labelCounts.containsKey(relation)) labelCounts.put(relation, 0);
					labelCounts.put(relation,labelCounts.get(relation)+1);
					
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}

			
			System.out.println(nonNONERel+" non-NONE relations of "+totalRel+" total relations.");
			System.out.println("Relation counts: ");
			for (String k : labelCounts.keySet()) {
				System.out.println("\t"+k+": "+labelCounts.get(k)+"\t"+labelCounts.get(k)/((double) totalRel)+"\t"+1/(labelCounts.get(k)/((double) totalRel)));
			}
			
			System.out.println();
		}
	}

	/**
	 * Traverses through given list of documents and initializes instances of feature vectors with associated output label
	 * (using indices provided by the given Lexiconer), adding them to the given SLProblem.
	 * @param docs Documents to be read
	 * @param lm Initialized Lexiconer to be used
	 * @param annotator Initialized annotator service to be used
	 * @param sp SLProblem to add instances to
	 */
	public static void createInstances(List<ACEDocument> docs, Lexiconer lm, AnnotatorService annotator, SLProblem sp, boolean ner_data, boolean ner_features, boolean binary_labels) {
		System.out.println("["+(new Date(System.currentTimeMillis()))+"] Creating instances ...");
		List<FeatureEnum>  fts = null;
		if (!ner_features) fts = getFeatureSet(feature_sets);
		else      		   fts = Arrays.asList(features_ner);
		
		for(ACEDocument doc : docs) {
			try{
				if (doc == null) continue;
				TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create the TextAnnotation

				annotator.addView(ta, ViewNames.SHALLOW_PARSE);

				List<?> entities  = doc.aceAnnotation.entityList;	// entities
				
				List<?> relations = null;
				if (!ner_data) relations = doc.aceAnnotation.relationList; // relations

				// for each pair of entities
				for (int i = 0; i < entities.size(); i++) {
					for (int j = 0; j < entities.size(); j++) {
						// entities
						ACEEntity e1 = ((ACEEntity) entities.get(i)); // first entity
						ACEEntity e2 = ((ACEEntity) entities.get(j)); // second entity

						List<ACEEntityMention> e1m = e1.entityMentionList; // mention list of first entity
						List<ACEEntityMention> e2m = e2.entityMentionList; // mention list of second entity

						// for each combination of mention pairs
						for (ACEEntityMention am1 : e1m) {
							for (ACEEntityMention am2 : e2m) {

								Mention m1 = new Mention(doc, e1, am1, ta); // first mention
								Mention m2 = new Mention(doc, e2, am2, ta); // second mention

								// add features and label if mention pair occurs within the same sentence(s)
								if (mentionsInSameSentence(ta,m1,m2)) {
									List<Integer> idxList = new ArrayList<Integer>();
									List<Double>  valList = new ArrayList<Double>();
									
									// add feature values to sparse feature vector representation
									for (FeatureEnum feature : fts) addFeatureToSparseVector(ta, lm, idxList, valList, feature, m1, m2);
									
									// add bias term
									idxList.add(lm.getNumOfFeature()+1);
									valList.add(1.0);
									
									// create instance
									IFeatureVector     fv = new FeatureVectorBuffer(idxList, valList).toFeatureVector();
									MultiClassInstance mi = new MultiClassInstance(lm.getNumOfFeature()+1, lm.getNumOfLabels(), fv, m1, m2, doc); 

									// create label
									MultiClassLabel    ml = null;
									if (!ner_data) {
										String relation = REUtils.getRelationType(m1, m2, relations);
										if (!binary_labels) ml = new MultiClassLabel(lm.getLabelId(Features.REL+relation));
										else {
											if (!relation.equals(Features.NONE)) ml = new MultiClassLabel(lm.getLabelId(Features.REL+Features.HASREL));
											else ml = new MultiClassLabel(lm.getLabelId(Features.REL+Features.NONE));
										}
									}
//									
									// add instance-output pair to SLProblem
									sp.addExample(mi, ml);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Traverses through given list of mention pairs and initializes instances of feature vectors with associated output label
	 * (using indices provided by the given Lexiconer), adding them to the given SLProblem.
	 * @param docs Documents to be read
	 * @param lm Initialized Lexiconer to be used
	 * @param annotator Initialized annotator service to be used
	 * @param sp SLProblem to add instances to
	 */
	public static void createInstancesFromMentionPairs(List<MentionPair> mention_pairs, Lexiconer lm, AnnotatorService annotator, SLProblem sp, boolean ner_data, boolean ner_features) {
		System.out.println("["+(new Date(System.currentTimeMillis()))+"] Creating instances from mention pairs ...");
		List<FeatureEnum>  fts = null;
		if (!ner_features) fts = getFeatureSet(feature_sets);
		else      		   fts = Arrays.asList(features_ner);
		
		for (MentionPair mention_pair : mention_pairs) {
			try {
				ACEDocument doc = mention_pair.document;
				
				if (doc == null) continue;
				TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create the TextAnnotation

				annotator.addView(ta, ViewNames.SHALLOW_PARSE);

				List<?> relations = null;
				if (!ner_data) relations = doc.aceAnnotation.relationList; // relations

				Mention m1 = mention_pair.m1; // first mention
				Mention m2 = mention_pair.m2; // second mention

				List<Integer> idxList = new ArrayList<Integer>();
				List<Double>  valList = new ArrayList<Double>();

				// add feature values to sparse feature vector representation
				for (FeatureEnum feature : fts) addFeatureToSparseVector(ta, lm, idxList, valList, feature, m1, m2);

				// add bias term
				idxList.add(lm.getNumOfFeature()+1);
				valList.add(1.0);

				// create instance
				IFeatureVector     fv = new FeatureVectorBuffer(idxList, valList).toFeatureVector();
				MultiClassInstance mi = new MultiClassInstance(lm.getNumOfFeature()+1, lm.getNumOfLabels(), fv, m1, m2, doc); 

				// create label
				MultiClassLabel    ml = null;
				if (!ner_data) {
					String relation = REUtils.getRelationType(m1, m2, relations);
					ml = new MultiClassLabel(lm.getLabelId(Features.REL+relation));
				}

				// add instance-output pair to SLProblem
				sp.addExample(mi, ml);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static List<MentionPair> getMentionPairsWithRelations(List<ACEDocument> docs, AnnotatorService annotator) throws Exception {
		System.out.println("["+(new Date(System.currentTimeMillis()))+"] Getting gold mention pairs with relations ...");
		List<MentionPair> candidates = new ArrayList<MentionPair>();
		
		for(ACEDocument doc : docs) {
			try{
				if (doc == null) continue;
				TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create the TextAnnotation
				
				annotator.addView(ta, ViewNames.SHALLOW_PARSE);
						
				List<?> entities  = doc.aceAnnotation.entityList;	  // entities
				List<?> relations = doc.aceAnnotation.relationList;	  // relations

				// for each pair of entities
				for (int i = 0; i < entities.size(); i++) {
					for (int j = 0; j < entities.size(); j++) {
						// entities
						ACEEntity e1 = ((ACEEntity) entities.get(i)); // first entity
						ACEEntity e2 = ((ACEEntity) entities.get(j)); // second entity

						List<ACEEntityMention> e1m = e1.entityMentionList; // mention list of first entity
						List<ACEEntityMention> e2m = e2.entityMentionList; // mention list of second entity
						
						// for each combination of mention pairs
						for (ACEEntityMention am1 : e1m) {
							for (ACEEntityMention am2 : e2m) {
								
								Mention m1 = new Mention(doc, e1, am1, ta); // first mention
								Mention m2 = new Mention(doc, e2, am2, ta); // second mention
								
								// add features and label if mention pair occurs within the same sentence(s)
								if (mentionsInSameSentence(ta,m1,m2)) {										
									// relation label
									String relation = REUtils.getRelationType(m1, m2, relations);
									if (!relation.contains(Features.NONE)) candidates.add(new MentionPair(doc, m1, m2, Features.REL+relation));
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return candidates;
	}
}