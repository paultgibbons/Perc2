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
package edu.illinois.perc2.re2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TokenLabelView;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ACEReader;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.SparseFeatureVector;

public class MultiClassIOManager {
	
	private static final String BASE_DIR="data/train/ACE05_English/";
    private static final String TEST_DIR="data/train/ACE05_English/nw";
    
    public static void main(String[] args) {
    	List<TextAnnotation> docs = readCorpus(false);
    	for (TextAnnotation ta : docs) {
    		System.out.println("CorpusID: "+ta.getCorpusId());
    		System.out.println("ID: "+ta.getId()+"\n");
    		System.out.println(ta.getAvailableViews());
    		
    		List<Constituent> sencs = ta.getView(ViewNames.SENTENCE).getConstituents();
    		for (Constituent c : sencs) {
    			System.out.println(c);
    			for (String k : c.getAttributeKeys()) System.out.print(k+", ");
    			System.out.println();
    		}
    		
    		List<Constituent> encs = ta.getView(ACEReader.ENTITYVIEW).getConstituents();
    		for (Constituent c : encs) {
//    			for (String s : c.getAttributeKeys()) System.out.print(s+", ");
    			String[] tokens = ta.getTokensInSpan(ta.getTokenIdFromCharacterOffset(Integer.parseInt(c.getAttribute("EntityHeadStartCharOffset"))), ta.getTokenIdFromCharacterOffset(Integer.parseInt(c.getAttribute("EntityHeadEndCharOffset"))));
    			System.out.print("ENTITY HEAD: ");
    			for (String tok : tokens) System.out.print(tok+" ");
    			System.out.println("\n");
    		}
    		break;
    	}
    }
    
    public static List<TextAnnotation> readCorpus(boolean is2004) {
    	String[] sections = {"bn","nw"};
    	List<TextAnnotation> documents = new ArrayList<TextAnnotation>();
    	try {
    		ACEReader reader = new ACEReader(BASE_DIR, sections, is2004);
    		for (TextAnnotation ta : reader) documents.add(ta);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return documents;
    }
	
	public static List<ACEDocument> readFolder (AceFileProcessor afp, String inputFolderStr) {
		File inputFolder = new File (inputFolderStr);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return fileName.endsWith(".apf.xml");
			}
		};
		File[] fileList = inputFolder.listFiles(filter);
		
		List<ACEDocument> docs = new ArrayList<ACEDocument>(); 
		
		PrintStream out = System.out;
    	PrintStream err = System.err;
    	PrintStream devnull = new PrintStream(new OutputStream() {
            public void write(int b) {
                //DO NOTHING
            }
        });
			
		for (int fileID = 0; fileID < fileList.length; ++fileID) {

            String annotationFile = fileList[fileID].getAbsolutePath();
            
            try{
            	
            	System.setOut(devnull);
            	System.setErr(devnull);
            	ACEDocument doc = afp.processAceEntry(new File(inputFolderStr), annotationFile);
            	System.setOut(out);
            	System.setErr(err);
            	docs.add(doc);
            } catch (Exception e) {
            	System.setOut(out);
            	System.setErr(err);
            	System.err.println("Error on reading file: "+annotationFile);
            	System.err.println(e.toString());
            }
		}
		return docs;
	}
	
	public static List<ACEDocument> trainDocs(List<ACEDocument> docs) {
		List<ACEDocument> train = new ArrayList<ACEDocument>();
		for (int i = 0; i < (int) (docs.size() * 0.8); i++) {
			train.add(docs.get(i));
		}
		return train;
	}
	
	public static List<ACEDocument> testDocs(List<ACEDocument> docs) {
		List<ACEDocument> train = new ArrayList<ACEDocument>();
		for (int i = (int) (docs.size() * 0.8) + 1; i < docs.size(); i++) {
			train.add(docs.get(i));
		}
		return train;
	}
	
	public static List<ACEDocument> readAll(AceFileProcessor afp) {
		List<ACEDocument> docs = new ArrayList<ACEDocument>();
		for(String dir : new String[]{"bc","bn","cts","nw","un","wl"}) {
			String folder = BASE_DIR + dir;
			docs.addAll(readFolder(afp,folder));
		}
		return docs;
	}

	public static float[][] getCostMatrix(Map<String, Integer> labelMapping,String fname) throws Exception{
		int numLabels = labelMapping.size();
		
		float[][] res = new float[numLabels][numLabels];
		for(int i=0;i  < numLabels ;i ++){
			for(int j=0; j < numLabels; j ++){
				if (i==j)
					res[i][j] = 0;
				else
					res[i][j] = 1.0f;
			}
		}
		ArrayList<String> lines = LineIO.read(fname);
		
		for(String line : lines){
			
			if (line.trim().charAt(0) == '#')
				continue;
			String[] tokens = line.split("\\s+");
			if (tokens.length != 3)
				throw new Exception("Format error in the cost matrix file.");
			if (!labelMapping.containsKey(tokens[0]))
				throw new Exception("Format error in the cost matrix file. Label (" + tokens[0] +") does not exist!"); 
			if (!labelMapping.containsKey(tokens[1]))
				throw new Exception("Format error in the cost matrix file. Label (" + tokens[1] +") does not exist!");
						
			int i = labelMapping.get(tokens[0]);
			int j = labelMapping.get(tokens[1]);
			float cost = -1;
			
			try{
				cost = Float.parseFloat(tokens[2]);				
			} catch(NumberFormatException e){
				throw new Exception("Format error in the cost matrix file. The cost should be a number!");
			}
			
			if (i ==j && cost !=0 )
				throw new Exception("The cost should be zero when pred == gold.");
			
			if (cost < 0)
				throw new Exception("The cost cannot be negative.");
			res[i][j] = cost;
		}		
		System.out.println("Done!");
		return res;
	}
	
	
	private static int checkNumOfFeaturesAndBuildClassMapping(String fileName,
			Map<String, Integer> labelMapping) throws Exception {
		int numFeatures = 0;
		
		ArrayList<String> lines = LineIO.read(fileName);

		for (String line : lines) {			
			String[] tokens = line.split("\\s+");
			String lab = tokens[0];

			// put the lab names into labels_mapping
			if (!labelMapping.containsKey(lab)) {
				int lab_size = labelMapping.size();
				labelMapping.put(lab, lab_size);
			}

			for (int i = 1; i < tokens.length; i++) {
				String[] featureTokens = tokens[i].split(":");
				
				if (featureTokens.length != 2){
					throw new Exception("Format error in the input file! in >" + line +"<");
				}
				
				int idx = Integer.parseInt(featureTokens[0]);

				if (idx <= 0) {
					throw new Exception(
							"The feature index must >= 1 !");
				}

				if (idx > numFeatures) {
					numFeatures = idx;
				}
			}
		}

		numFeatures ++; //allocate for zero 
		numFeatures ++; //allocate for the bias term		
		
		System.out.println("Label Mapping: "
				+ labelMapping.toString().replace("=", "==>"));
		System.out.println("number of features:" + numFeatures);

		return numFeatures;
	}

	/**
	 * Read training data
	 * 
	 * @param fname
	 *            The filename contains the training data
	 * @return A LabeledMulticlasssData
	 * @throws Exception
	 */
	public static LabeledMultiClassData readTrainingData(String fname)
			throws Exception {
		Map<String, Integer> labelMapping = new HashMap<String, Integer>();
		int numFeatures = checkNumOfFeaturesAndBuildClassMapping(fname,
				labelMapping);
		int numClasses = labelMapping.size();

		LabeledMultiClassData res = new LabeledMultiClassData(labelMapping,
				numFeatures);
		readMultiClassDataAndAddBiasTerm(fname, labelMapping, numFeatures, numClasses, res);
		return res;
	}

	/**
	 * Read testing data.
	 * 
	 * @param fname
	 *            The filename contains the testing data
	 * @return A LabeledMulticlasssData
	 * @throws Exception
	 */
	public static LabeledMultiClassData readTestingData(String fname,
			Map<String, Integer> labelsMapping, int numFeatures) throws Exception {
		int numClasses = labelsMapping.size();
		LabeledMultiClassData res = new LabeledMultiClassData(labelsMapping,
				numFeatures);
		readMultiClassDataAndAddBiasTerm(fname, labelsMapping, numFeatures, numClasses, res);
		return res;
	}

	private static void readMultiClassDataAndAddBiasTerm(String fname,
			Map<String, Integer> labelMapping, int numFeatures, int numClasses,
			LabeledMultiClassData res) throws FileNotFoundException {
		ArrayList<String> lines = LineIO.read(fname);
		for (String line : lines) {
			String[] tokens = line.split("\\s+");

			int activeLen = 1;

			// ignore the features > n_features
			for (int i = 1; i < tokens.length; i++) {

				String[] featureTokens = tokens[i].split(":");
				int idx = Integer.parseInt(featureTokens[0]); 
				if (idx <= numFeatures) 
					activeLen++;
			}

			int[] idxList = new int[activeLen];
			float[] valueList = new float[activeLen];

			for (int i = 1; i < tokens.length; i++) {
				String[] feaureTokens = tokens[i].split(":");
				int idx = Integer.parseInt(feaureTokens[0]); 
				if (idx <= numFeatures) { 
					idxList[i - 1] = idx;
					valueList[i - 1] = Float.parseFloat(feaureTokens[1]);
				}
			}
			// append the bias term
			idxList[activeLen-1] = numFeatures-1;
			valueList[activeLen-1] = 1;

			IFeatureVector fv = new SparseFeatureVector(idxList, valueList);
			MultiClassInstance mi = new MultiClassInstance(numFeatures, numClasses,
					fv);
			res.instanceList.add(mi);

			String lab = tokens[0];
			if (labelMapping.containsKey(lab)) {
				res.goldStructureList.add(new MultiClassLabel(labelMapping.get(lab)));
			} else {
				// only design for unknown classes in the test data
				res.goldStructureList.add(new MultiClassLabel(-1));
			}
		}
	}
	
	public static SLProblem readStructuredData(List<ACEDocument> docs, Lexiconer lm) {
		SLProblem sp = new SLProblem();
		
		Properties props = new Properties();
        props.setProperty(PipelineConfigurator.USE_POS.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_LEMMA.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_SHALLOW_PARSE.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_NER_CONLL.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_NER_ONTONOTES.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_STANFORD_PARSE.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_STANFORD_DEP.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_SRL_VERB.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_SRL_NOM.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.STFRD_MAX_SENTENCE_LENGTH.key, "10000");
        props.setProperty(PipelineConfigurator.STFRD_TIME_PER_SENTENCE.key, "100000000");

        ResourceManager rm = new ResourceManager(props);
        AnnotatorService annotator = null;
        try {
            annotator = IllinoisPipelineFactory.buildPipeline(rm);
        } catch (Exception e) {
            e.printStackTrace();
        }

		if (lm.isAllowNewFeatures()) {
			lm.addFeature("hm1:UNK");
			lm.addFeature("hm2:UNK");
			lm.addFeature("hm12:UNK");
			lm.addLabel("label:UNK");
		}

		if (lm.isAllowNewFeatures()) {
			for(ACEDocument doc : docs) {
				try{
					TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags);
					annotator.addView(ta, ViewNames.POS);

					System.out.println(ta.getAvailableViews());
					TokenLabelView posView = (TokenLabelView) ta.getView(ViewNames.POS);
					for (int i = 0; i < ta.size(); i++) System.out.println(i+": "+posView.getLabel(i));
					
					if (true) return null;
							
					List<?> entity_list = doc.aceAnnotation.entityList;
					int entity_size = entity_list.size();

					List<?> relation_list = doc.aceAnnotation.relationList;
					List<ACEEntityMention> entity_mention_list= new ArrayList<ACEEntityMention>();

					for (int i = 0; i < entity_list.size(); i++) {
						ACEEntity current = (ACEEntity) entity_list.get(i);

						for (int j = 0; j < current.entityMentionList.size(); j++ ) {
							ACEEntityMention current_entity_mention = current.entityMentionList.get(j);
							entity_mention_list.add(current_entity_mention);
						}	
					}

					// for each pair of entities
					for (int i = 0; i < entity_size; i++) {
						for (int j = 0; j < entity_size; j++) {
							// get features
							ACEEntity e1 = ((ACEEntity) entity_list.get(i));
							ACEEntity e2 = ((ACEEntity) entity_list.get(j));
							String id1 = e1.id;
							String id2 = e2.id;

							String type1 = e1.type;
							String type2 = e2.type;

							ACEEntityMention first = entity_mention_list.get(i);
							ACEEntityMention second = entity_mention_list.get(j);

							String mid1 = first.id;
							String mid2 = second.id; 

							String head1 = first.head;
							String head2 = second.head;
							
							String mtype1 = first.type;
							String mtype2 = second.type;

							lm.addFeature("t1:" + type1);
							lm.addFeature("t2:" + type2);
							lm.addFeature("w1:" + head1);
							lm.addFeature("w2:" + head2);
							lm.addFeature("ct:"+ mtype1+mtype2);

							String relation_type = REUtils.getRelationType(id1, id2, mid1, mid2, relation_list);
							lm.addLabel(relation_type);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		int docErrorCount = 0;
		int docCount = 0;

		for(ACEDocument doc : docs) {
			try{
				List<?> entity_list = doc.aceAnnotation.entityList;
				int entity_size = entity_list.size();

				//map entity id to entity type and character position of first entity mention
				Map<String, Integer> start_map = new HashMap<String, Integer>();

				for (int i = 0; i < entity_size; i++) {
					ACEEntity current = (ACEEntity) entity_list.get(i);
					int min_start = Integer.MAX_VALUE;
					for (int j = 0; j < current.entityMentionList.size(); j++ ) {
						min_start = Math.min(min_start, current.entityMentionList.get(j).headStart);
					}
					start_map.put(current.id, min_start);
				}

				List<?> relation_list = doc.aceAnnotation.relationList;

				List<ACEEntityMention> entity_mention_list= new ArrayList<ACEEntityMention>();

				for (int i = 0; i < entity_list.size(); i++) {
					ACEEntity current = (ACEEntity) entity_list.get(i);

					for (int j = 0; j < current.entityMentionList.size(); j++ ) {
						ACEEntityMention current_entity_mention = current.entityMentionList.get(j);
						entity_mention_list.add(current_entity_mention);
					}	
				}

				// for each pair of entities
				for (int i = 0; i < entity_size; i++) {
					for (int j = 0; j < entity_size; j++) {
						// get features
						ACEEntity e1 = ((ACEEntity) entity_list.get(i));
						ACEEntity e2 = ((ACEEntity) entity_list.get(j));
						String id1 = e1.id;
						String id2 = e2.id;

						String type1 = e1.type;
						String type2 = e2.type;

						ACEEntityMention first = entity_mention_list.get(i);
						ACEEntityMention second = entity_mention_list.get(j);

						String mid1 = first.id;
						String mid2 = second.id; 
						
						String head1 = first.head;
						String head2 = second.head;

						String mtype1 = first.type;
						String mtype2 = second.type;

						int type1i = (lm.containFeature("t1:" + type1)) ? lm.getFeatureId("t1:"+type1) : lm.getFeatureId("t1:unknowntype");
						int type2i = (lm.containFeature("t2:" + type2)) ? lm.getFeatureId("t2:"+type2) : lm.getFeatureId("t2:unknowntype");

						int head1i = (lm.containFeature("w1:" + head1)) ? lm.getFeatureId("w1:"+head1) : lm.getFeatureId("w1:unknownword");
						int head2i = (lm.containFeature("w2:" + head2)) ? lm.getFeatureId("w2:"+head2) : lm.getFeatureId("w2:unknownword");
						
						int mtypei = (lm.containFeature("ct:"+mtype1+mtype2)) ? lm.getFeatureId("ct:"+mtype1+mtype2) : lm.getFeatureId("ct:unknowntypecomb");

						int distance1 = Math.abs(start_map.get(id1) - start_map.get(id2));
						int distance2 = Math.abs(start_map.get(id2) - start_map.get(id1));
						int distance = Math.min(distance1, distance2);


//						Input x = new Input(type1i, type2i, head1i, head2i, mtypei, distance);
						// if there is a relation, add that type as output
						// else add null/none as output
						String relation_type = REUtils.getRelationType(id1, id2, mid1, mid2, relation_list);
						//				if (lm.isAllowNewFeatures()) {
//						lm.addLabel(relation_type);
						//				}

						int relation_typei = lm.getLabelId(relation_type);

//						sp.addExample(x, new Output(relation_typei));
					}
				}
			} catch (Exception e) {
				docErrorCount++;
			} finally {
				docCount++;
			}
		}
		System.out.println("doc Error Count:"+docErrorCount);
		System.out.println("doc Count:" + docCount);
		return sp;
	}
}
