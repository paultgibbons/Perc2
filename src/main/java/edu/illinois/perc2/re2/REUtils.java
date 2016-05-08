package edu.illinois.perc2.re2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
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

public class REUtils {
	public static FeatureEnum[] features = { FeatureEnum.HM1, FeatureEnum.HM2, FeatureEnum.HM12, FeatureEnum.WM1, 
			FeatureEnum.WM2, FeatureEnum.ET12, FeatureEnum.ML12, FeatureEnum.AM2F, FeatureEnum.AM2L, FeatureEnum.BM1F,
			FeatureEnum.BM1L, FeatureEnum.WBNULL, FeatureEnum.WBFL, FeatureEnum.WBF, FeatureEnum.WBL, FeatureEnum.WBO
	};
	
	static HashMap<String,Integer> labelCounts = new HashMap<String,Integer>();
	
	/**
	 * Initializes an annotator service.
	 * @return initialized annotator service
	 */
	public static AnnotatorService initializeAnnotator() {
		Properties props = new Properties();
        props.setProperty(PipelineConfigurator.USE_POS.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_LEMMA.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_SHALLOW_PARSE.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_NER_CONLL.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_NER_ONTONOTES.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_STANFORD_PARSE.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_STANFORD_DEP.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_SRL_VERB.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_SRL_NOM.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.STFRD_MAX_SENTENCE_LENGTH.key, "10000");
        props.setProperty(PipelineConfigurator.STFRD_TIME_PER_SENTENCE.key, "100000000");

        ResourceManager  rm = new ResourceManager(props);
        AnnotatorService annotator = null;
        try {
            annotator = IllinoisPipelineFactory.buildPipeline(rm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return annotator;
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
		return "NONE";
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
	 * Gets the string feature of feature label+value given an ordered pair of mentions and a requested feature type.
	 * Most features will return a size-1 list of the feature label+value for use in a one-hot feature vector initialization.
	 * In bag-of-words feature cases, this method will return a list of such feature label+values.
	 * @param ta TextAnnotation of the document
	 * @param m1 First mention
	 * @param m2 Second mention
	 * @param feature Feature to be computed
	 * @return String feature label+value to be used to index into a Lexiconer
	 */
	public static List<String> getFeature(TextAnnotation ta, Mention m1, Mention m2, FeatureEnum feature) {
		int m1hs = m1.headStartId;   // m1 head start
		int m1he = m1.headEndId;     // m1 head end
		int m2hs = m2.headStartId;   // m2 head start
		int m2he = m2.headEndId;     // m2 head end
		
		int m1es = m1.extentStartId; // m1 extent start
		int m1ee = m1.extentEndId;   // m1 extent end
		int m2es = m2.extentStartId; // m2 extent start
		int m2ee = m2.extentEndId;   // m2 extent end
		
		List<String> values = new ArrayList<String>();
		
		switch (feature) {
		case HM1:  // head word 1
			return Arrays.asList(Features.HM1+String.join(" ",ta.getTokensInSpan(m1hs,m1he)));
		case HM2:  // head word 2
			return Arrays.asList(Features.HM2+String.join(" ",ta.getTokensInSpan(m2hs,m2he)));
		case HM12: // combination of head words 1 and 2
			return Arrays.asList(Features.HM12+String.join(" ",ta.getTokensInSpan(m1hs,m1he))+" "+String.join(" ",ta.getTokensInSpan(m2hs,m2he)));
		case WM1:  // bag-of-words 1
			for (String s : ta.getTokensInSpan(m1es,m1ee)) values.add(Features.WM1+s);
			return values;
		case WM2:  // bag-of-words 2
			for (String s : ta.getTokensInSpan(m2es,m2ee)) values.add(Features.WM2+s);
			return values;
		case ET12: // combination of entity types
			return Arrays.asList(Features.ET12+m1.entity.type+" "+m2.entity.type);
		case ML12: // combination of mention levels
			return Arrays.asList(Features.ML12+m1.mention.type+" "+m2.mention.type);
		case BM1F: // first word before M1
			if (m1es-1 < 0) return Arrays.asList(Features.BM1F+Features.NULL);
			return Arrays.asList(Features.BM1F+ta.getToken(m1es-1));
		case BM1L:
			if (m1es-2 < 0) return Arrays.asList(Features.BM1L+Features.NULL);
			return Arrays.asList(Features.BM1L+ta.getToken(m1es-2));
		case AM2F:
			if (m2ee+1 > ta.size()-1) return Arrays.asList(Features.AM2F+Features.NULL);
			return Arrays.asList(Features.AM2F+ta.getToken(m2ee+1));
		case AM2L:
			if (m2ee+2 > ta.size()-1) return Arrays.asList(Features.AM2L+Features.NULL);
			return Arrays.asList(Features.AM2L+ta.getToken(m2ee+2));
		case WBNULL:
			if (m1ee+1 == m2es || m2ee+1 == m1es) return Arrays.asList(Features.WBNULL+Features.TRUE);
			return Arrays.asList(Features.WBNULL+Features.FALSE);
		case WBFL:
			if (m1ee+2 == m2es) return Arrays.asList(Features.WBFL+ta.getToken(m1ee+1));
			if (m2ee+2 == m1es) return Arrays.asList(Features.WBFL+ta.getToken(m2ee+1));
			return Arrays.asList(Features.WBFL+Features.NULL);
		case WBF:
			if (m1ee+3 <= m2es) return Arrays.asList(Features.WBF+ta.getToken(m1ee+1));
			if (m2ee+3 <= m1es) return Arrays.asList(Features.WBF+ta.getToken(m2ee+1));
			return Arrays.asList(Features.WBF+Features.NULL);
		case WBL:
			if (m1ee+3 <= m2es) return Arrays.asList(Features.WBL+ta.getToken(m2es-1));
			if (m2ee+3 <= m1es) return Arrays.asList(Features.WBL+ta.getToken(m1es-1));
			return Arrays.asList(Features.WBL+Features.NULL);
		case WBO:
			if (m1ee+4 <= m2es) {
				for (String s : ta.getTokensInSpan(m1ee+2,m2es-1)) values.add(Features.WBO+s);
				return values;
			}
			if (m2ee+4 <= m1es) {
				for (String s : ta.getTokensInSpan(m2ee+2,m1es-1)) values.add(Features.WBO+s);
				return values;
			}
			return Arrays.asList(Features.WBO+Features.NULL);
		default:   // unimplemented
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
	
	public static void addNullFeature(Lexiconer lm, FeatureEnum feature) {
		if (feature == FeatureEnum.BM1F || feature == FeatureEnum.BM1L || feature == FeatureEnum.AM2F || feature == FeatureEnum.AM2L)
			lm.addFeature(Features.getFeatureString(feature)+Features.NULL);
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
		List<String> feature_values = getFeature(ta, m1, m2, feature); // get feature values
		
		// replace unseen feature-value pairs with UNK values
		for (int i = 0; i < feature_values.size(); i++) {
			if (!lm.containFeature(feature_values.get(i))) feature_values.set(i, getUNKFeature(feature));
		}
		
		switch (feature) {
		// one-hot features
		case HM1:
		case HM2:
		case HM12:
		case ET12:
		case ML12:
		case BM1F:
		case BM1L:
		case AM2F:
		case AM2L:
		case WBNULL:
		case WBFL:
		case WBF:
		case WBL:
			for (String fv : feature_values) {
				idxList.add(lm.getFeatureId(fv));
				valList.add(1.0);
			}
			return;
			
		// bag-of-words features
		case WM1:
		case WM2:
		case WBO:
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
			
		default:
			System.err.println("Unimplemented feature id:"+feature.name());
			return;
		}
	}

	/**
	 * Traverses through given list of (training) documents and adds all instances of feature label+values into the
	 * given Lexiconer.
	 * @param docs Training documents
	 * @param lm Lexiconer to be used
	 * @param annotator Initialized annotator service
	 */
	public static void addFeaturesToLexiconer(List<ACEDocument> docs, Lexiconer lm, AnnotatorService annotator) {
		if (lm.isAllowNewFeatures()) {
			
			// add UNK tokens for all features
			for (FeatureEnum feature : features) {
				lm.addFeature(getUNKFeature(feature));
				addNullFeature(lm, feature);
			}
			
			int totalRel = 0, nonNONERel = 0;
			
			for(ACEDocument doc : docs) {
				try{
					if (doc == null) continue;
					TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create the TextAnnotation
							
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
									
									Mention m1 = new Mention(e1, am1, ta); // first mention
									Mention m2 = new Mention(e2, am2, ta); // second mention
									
									// add features and label if mention pair occurs within the same sentence(s)
									if (mentionsInSameSentence(ta,m1,m2)) {										
										for (FeatureEnum feature : features) {
											for (String s : getFeature(ta,m1,m2,feature)) {
												lm.addFeature(s);
//												System.out.println("Added feature "+s);
											}
										}

										// relation label
										String relation = REUtils.getRelationType(m1, m2, relations);
										lm.addLabel(Features.REL+relation);
										
										if (!relation.equals("NONE")) nonNONERel++;
										totalRel++;
										
										if (!labelCounts.containsKey(relation)) labelCounts.put(relation, 0);
										labelCounts.put(relation,labelCounts.get(relation)+1);
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
	 * Traverses through given list of documents and initializes instances of feature vectors with associated output label
	 * (using indices provided by the given Lexiconer), adding them to the given SLProblem.
	 * @param docs Documents to be read
	 * @param lm Initialized Lexiconer to be used
	 * @param annotator Initialized annotator service to be used
	 * @param sp SLProblem to add instances to
	 */
	public static void createInstances(List<ACEDocument> docs, Lexiconer lm, AnnotatorService annotator, SLProblem sp) {
		for(ACEDocument doc : docs) {
			try{
				if (doc == null) continue;
				TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create the TextAnnotation

				List<?> entities  = doc.aceAnnotation.entityList;	// entities
				List<?> relations = doc.aceAnnotation.relationList; // relations

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

								Mention m1 = new Mention(e1, am1, ta); // first mention
								Mention m2 = new Mention(e2, am2, ta); // second mention

								// add features and label if mention pair occurs within the same sentence(s)
								if (mentionsInSameSentence(ta,m1,m2)) {
									List<Integer> idxList = new ArrayList<Integer>();
									List<Double>  valList = new ArrayList<Double>();
									
									// add feature values to sparse feature vector representation
									for (FeatureEnum feature : features) addFeatureToSparseVector(ta, lm, idxList, valList, feature, m1, m2);
									
									// add bias term
									idxList.add(lm.getNumOfFeature()+1);
									valList.add(1.0);
									
									// create instance
									IFeatureVector     fv = new FeatureVectorBuffer(idxList, valList).toFeatureVector();
									MultiClassInstance mi = new MultiClassInstance(lm.getNumOfFeature()+1, lm.getNumOfLabels(), fv); 

									// create label
									MultiClassLabel    ml = new MultiClassLabel(lm.getLabelId(Features.REL+REUtils.getRelationType(m1, m2, relations)));
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
}