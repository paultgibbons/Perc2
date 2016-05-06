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
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TokenLabelView;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class MultiClassIOManager {
	
	private static final String BASE_DIR="data/train/ACE05_English/";
    
    public static List<ACEDocument> train;
    public static List<ACEDocument> test;
    
    public static List<ACEDocument> getTrainingDocuments()  { return train; }
    public static List<ACEDocument> getTestDocuments()		{ return test; 	}
    
	public static void readCorpus(AceFileProcessor afp) {
		train = new ArrayList<ACEDocument>();
		test  = new ArrayList<ACEDocument>();
		
//		for(String dir : new String[]{"bc","bn","cts","nw","un","wl"}) {
		for(String dir : new String[]{"bn","nw"}) {
			String folder = BASE_DIR + dir;
			readFolder(afp,folder);
		}
	}
	
	public static void readFolder (AceFileProcessor afp, String inputFolderStr) {
		File inputFolder = new File (inputFolderStr);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return fileName.endsWith(".apf.xml");
			}
		};
		
		File[] fileList = inputFolder.listFiles(filter);
		Arrays.sort(fileList);
		
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

		for (int i = 0; i < (int) (docs.size() * 0.2); i++) 			  test.add(docs.get(i));
		for (int i = (int) (docs.size() * 0.2) + 1; i < docs.size(); i++) train.add(docs.get(i));
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
	
	public static SLProblem readStructuredData(List<ACEDocument> docs, Lexiconer lm, AnnotatorService annotator) {
		SLProblem sp = new SLProblem();
        
        if (lm.isAllowNewFeatures()) REUtils.addFeaturesToLexiconer(docs, lm, annotator);
//        REUtils.createTrainingInstances(docs, lm, annotator, sp);
		
		return sp;
	}
	
//	public static void addTrainingInstances(List<ACEDocument> docs, Lexiconer lm, AnnotatorService annotator, SLProblem sp) {
//		for(ACEDocument doc : docs) {
//			try{
//				if (doc == null) continue;
//				
//				List<?> entities  = doc.aceAnnotation.entityList;
//				List<?> relations = doc.aceAnnotation.relationList;
//
//				List<ACEEntityMention> entity_mention_list= new ArrayList<ACEEntityMention>();
//
//				for (int i = 0; i < entities.size(); i++) {
//					ACEEntity current = (ACEEntity) entities.get(i);
//
//					for (int j = 0; j < current.entityMentionList.size(); j++ ) {
//						ACEEntityMention current_entity_mention = current.entityMentionList.get(j);
//						entity_mention_list.add(current_entity_mention);
//					}	
//				}
//
//				// for each pair of entities
//				for (int i = 0; i < entity_size; i++) {
//					for (int j = 0; j < entity_size; j++) {
//						// get features
//						ACEEntity e1 = ((ACEEntity) entities.get(i));
//						ACEEntity e2 = ((ACEEntity) entities.get(j));
//						String id1 = e1.id;
//						String id2 = e2.id;
//
//						String type1 = e1.type;
//						String type2 = e2.type;
//
//						ACEEntityMention first = entity_mention_list.get(i);
//						ACEEntityMention second = entity_mention_list.get(j);
//
//						String mid1 = first.id;
//						String mid2 = second.id; 
//						
//						String head1 = first.head;
//						String head2 = second.head;
//
//						String mtype1 = first.type;
//						String mtype2 = second.type;
//
//						int type1i = (lm.containFeature("t1:" + type1)) ? lm.getFeatureId("t1:"+type1) : lm.getFeatureId("t1:unknowntype");
//						int type2i = (lm.containFeature("t2:" + type2)) ? lm.getFeatureId("t2:"+type2) : lm.getFeatureId("t2:unknowntype");
//
//						int head1i = (lm.containFeature("w1:" + head1)) ? lm.getFeatureId("w1:"+head1) : lm.getFeatureId("w1:unknownword");
//						int head2i = (lm.containFeature("w2:" + head2)) ? lm.getFeatureId("w2:"+head2) : lm.getFeatureId("w2:unknownword");
//						
//						int mtypei = (lm.containFeature("ct:"+mtype1+mtype2)) ? lm.getFeatureId("ct:"+mtype1+mtype2) : lm.getFeatureId("ct:unknowntypecomb");
//
//						int distance1 = Math.abs(start_map.get(id1) - start_map.get(id2));
//						int distance2 = Math.abs(start_map.get(id2) - start_map.get(id1));
//						int distance = Math.min(distance1, distance2);
//
//
////						Input x = new Input(type1i, type2i, head1i, head2i, mtypei, distance);
//						// if there is a relation, add that type as output
//						// else add null/none as output
//						String relation_type = REUtils.getRelationType(id1, id2, mid1, mid2, relations);
//						//				if (lm.isAllowNewFeatures()) {
////						lm.addLabel(relation_type);
//						//				}
//
//						int relation_typei = lm.getLabelId(relation_type);
//
////						sp.addExample(x, new Output(relation_typei));
//					}
//				}
//			} catch (Exception e) {
//				docErrorCount++;
//			} finally {
//				docCount++;
//			}
//		}
//		System.out.println("doc Error Count:"+docErrorCount);
//		System.out.println("doc Count:" + docCount);
//	}
}
