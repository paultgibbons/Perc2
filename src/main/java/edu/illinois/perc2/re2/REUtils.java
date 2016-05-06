package edu.illinois.perc2.re2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
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
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TokenLabelView;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelation;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelationArgument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelationArgumentMention;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelationMention;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.SparseFeatureVector;
import edu.illinois.perc2.re2.Features.FeatureEnum;

public class REUtils {
	public static FeatureEnum[] features = { FeatureEnum.HM1, FeatureEnum.HM2, FeatureEnum.HM12, FeatureEnum.WM1, 
			FeatureEnum.WM2, FeatureEnum.ET12, FeatureEnum.ML12
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
			for (FeatureEnum feature : features) lm.addFeature(getUNKFeature(feature));
			
			int totalRel = 0, nonNONERel = 0;
			
			for(ACEDocument doc : docs) {
				try{
					if (doc == null) continue;
					TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags);
//					annotator.addView(ta, ViewNames.POS);
//					System.out.println(ta.getAvailableViews());
//					TokenLabelView posView = (TokenLabelView) ta.getView(ViewNames.POS);
//					for (int i = 0; i < ta.size(); i++) System.out.println(i+": "+posView.getLabel(i));
					
					annotator.addView(ta,  ViewNames.SHALLOW_PARSE);
					SpanLabelView v = (SpanLabelView) ta.getView(ViewNames.SHALLOW_PARSE);
					for (Constituent c : v.getConstituents()) {
						System.out.println(c+": "+c.getLabel());
					}
							
					List<?> entities  = doc.aceAnnotation.entityList;
					List<?> relations = doc.aceAnnotation.relationList;

					// for each pair of entities
					for (int i = 0; i < entities.size(); i++) {
						for (int j = 0; j < entities.size(); j++) {
							// entities
							ACEEntity e1 = ((ACEEntity) entities.get(i));
							ACEEntity e2 = ((ACEEntity) entities.get(j));

							List<ACEEntityMention> e1m = e1.entityMentionList;
							List<ACEEntityMention> e2m = e2.entityMentionList;
							
							// mention pairs
							for (ACEEntityMention am1 : e1m) {
								for (ACEEntityMention am2 : e2m) {
									
									Mention m1 = new Mention(e1, am1, ta);
									Mention m2 = new Mention(e2, am2, ta);
									
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
		}
	}

	public static void createTrainingInstances(List<ACEDocument> docs, Lexiconer lm, AnnotatorService annotator, SLProblem sp) {
		for(ACEDocument doc : docs) {
			try{
				if (doc == null) continue;
				TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags);
//				annotator.addView(ta, ViewNames.POS);
//				System.out.println(ta.getAvailableViews());
//				TokenLabelView posView = (TokenLabelView) ta.getView(ViewNames.POS);
//				for (int i = 0; i < ta.size(); i++) System.out.println(i+": "+posView.getLabel(i));

				List<?> entities  = doc.aceAnnotation.entityList;
				List<?> relations = doc.aceAnnotation.relationList;

				// for each pair of entities
				for (int i = 0; i < entities.size(); i++) {
					for (int j = 0; j < entities.size(); j++) {
						// entities
						ACEEntity e1 = ((ACEEntity) entities.get(i));
						ACEEntity e2 = ((ACEEntity) entities.get(j));

						List<ACEEntityMention> e1m = e1.entityMentionList;
						List<ACEEntityMention> e2m = e2.entityMentionList;

						// mention pairs
						for (ACEEntityMention am1 : e1m) {
							for (ACEEntityMention am2 : e2m) {

								Mention m1 = new Mention(e1, am1, ta);
								Mention m2 = new Mention(e2, am2, ta);

								// add features and label if mention pair occurs within the same sentence(s)
								if (mentionsInSameSentence(ta,m1,m2)) {
									List<Integer> idxList = new ArrayList<Integer>();
									List<Integer> valList = new ArrayList<Integer>();
									
//									for (FeatureEnum feature : features) addFeatureToSparseVector(idxList, valList, feature, m1, m2);

									// relation label
//									lm.addLabel(Features.REL+REUtils.getRelationType(m1, m2, relations));
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