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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.commands.CommandDescription;
import edu.illinois.cs.cogcomp.core.utilities.commands.InteractiveShell;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLParameters;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.learner.l2_loss_svm.L2LossSSVMLearner;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;

public class CRMainClass {

	public static void crMain(List<ACEDocument> docs) throws Exception {
			String configFilePath = "config/CR.config";
			String modelPath = "models/CRmodel1";
			
			SLModel model = new SLModel();
			model.lm = new Lexiconer();

			SLProblem sp = readStructuredData(docs);

			// Disallow the creation of new features
			model.lm.setAllowNewFeatures(false);

			// initialize the inference solver
			model.infSolver = new InferenceSolver();

			FeatureGenerator fg = new FeatureGenerator();
			SLParameters para = new SLParameters();
			para.loadConfigFile(configFilePath);
			//para.TOTAL_NUMBER_FEATURE = model.lm.getNumOfFeature()
			//		* model.lm.getNumOfLabels() + model.lm.getNumOfLabels()
			//		+ model.lm.getNumOfLabels() * model.lm.getNumOfLabels();
			
			// numLabels*numLabels for transition features
			// numWordsInVocab*numLabels for emission features
			// numLabels for prior on labels
			Learner learner = LearnerFactory.getLearner(model.infSolver, fg,
					para);
			model.wv = learner.train(sp);
			WeightVector.printSparsity(model.wv);
			if(learner instanceof L2LossSSVMLearner)
				System.out.println("Primal objective:" + ((L2LossSSVMLearner)learner).getPrimalObjective(sp, model.wv, model.infSolver, para.C_FOR_STRUCTURE));

			// save the model
			model.saveModel(modelPath);
		
	}
	
	public static void testCRModel(List<ACEDocument> docs, String modelPath) throws Exception {
		SLModel model = SLModel.loadModel(modelPath);
		List<List<SLProblem>> spll = new ArrayList<List<SLProblem>>();
		List<List<String>> parallel_mention_ids = new ArrayList<List<String>>();
		Map<String, Integer> entity_size_map = new HashMap<String, Integer>();
		for(ACEDocument doc: docs) {
			List<String> parallel_mention_id = new ArrayList<String>();
			spll.add(readStructuredDataTest(doc, parallel_mention_id, entity_size_map));
			parallel_mention_ids.add(parallel_mention_id);
		}
		
		/*
		double acc = 0.0;
		double total = 0.0;
		int falses = 0;
		int fp = 0;
		int tp = 0;
		int fn = 0;
		int tn = 0;
		*/
		
		List<List<Set<String>>> all_clusters = new ArrayList<List<Set<String>>>();
		
		for (int i = 0; i < spll.size(); i++) {
			// for each document
			List<SLProblem> splist = spll.get(i);
			List<String> mention_list = parallel_mention_ids.get(i);
			List<Set<String>> clusters = new ArrayList<Set<String>>();
			for (int k = 0; k < splist.size(); k++) {
				// for each mention
				double[] scores = new double[clusters.size()];
				int[] counts = new int[clusters.size()];
				SLProblem sp = splist.get(k);
				String current_mention = mention_list.get(k);
				double best_score = Double.NEGATIVE_INFINITY;
				int best_cluster_index = -1;
				for (int j = 0; j < sp.instanceList.size(); j++) {
					Output prediction = (Output) model.infSolver.getBestStructure(
							model.wv, sp.instanceList.get(j));
					int index = getAssociatedIndex(clusters,prediction.mention1id);
					scores[index] += prediction.trueScore;
					counts[index] += 1;
				}
				for (int j = 0; j < clusters.size(); j++) {
					double average = getAverage(scores[j], counts[j]);
					if (average > best_score) {
						best_score = average;
						best_cluster_index = j;
					}
				}
				// add mention id to correct cluster
				if (best_score <= 0.5) {
					Set<String> h = new HashSet<String>();
					h.add(current_mention);
					clusters.add(h);
				} else {
					clusters.get(best_cluster_index).add(current_mention);
				}
			}
			all_clusters.add(clusters);
		}
		
		evaluate(all_clusters, entity_size_map);
		return;

	}

	private static int getAssociatedIndex(List<Set<String>> clusters,
			String mention1id) {
		for(int i = 0; i < clusters.size(); i++) {
			if (clusters.get(i).contains(mention1id)) {
				return i;
			}
		}
		return -1;
	}

	private static double getAverage(double d, int i) {
		return (d/i);
	}

	private static void evaluate(List<List<Set<String>>> all_clusters, Map<String, Integer> entity_size_map) {
		int total = 0;
		double presum = 0.0;
		double resum = 0.0;
		
		for ( List<Set<String>> clusters : all_clusters ) {
			// for each document
			for ( Set<String> cluster : clusters ) {
				// for each cluster in the document
				for ( String mention : cluster ) {
					// for each mention in the cluster
					double numerator = 1.0;
					String[] parts = mention.split("-");
					for ( String second_mention : cluster ) {
						if (mention.equals(second_mention)) {
							continue;
						} else {
							String[] second_parts = second_mention.split("-");
							if (second_parts[second_parts.length - 1].equals(parts[parts.length-1])) {
								numerator += 1.0;
							}
						}
					}
					presum += numerator / cluster.size();
					
					StringBuilder builder = new StringBuilder();
					for(int i = 0; i < parts.length-1; i++) {
					    builder.append(parts[i]);
					    if (i < parts.length - 2) {
					    	builder.append('-');
					    }
					}
					String entity_id = builder.toString();
					resum += numerator / (entity_size_map.get(entity_id));
					total++;
				}
			}
		}
		double precision = (presum/total);
		System.out.println("Precision: " + precision);
		double recall= (resum/total);
		System.out.println("Recall: " + recall);
		double f1 = 2 * (precision * recall) / (precision + recall);
		System.out.println("F1: " + f1);
		
	}

	public static SLProblem readStructuredData(List<ACEDocument> docs)
			throws IOException, DataFormatException {
		SLProblem sp = new SLProblem();

		// w:unknownword indicates out-of-vocabulary words in test phase
		//if (lm.isAllowNewFeatures())
		//	lm.addFeature("w:unknownword");
		
		int docErrorCount = 0;
		int docCount = 0;
		
		for(ACEDocument doc: docs) {
			try{
			List<?> entity_list = doc.aceAnnotation.entityList;
			int entity_size = entity_list.size();
			
			List<ACEEntityMention> entity_mention_list= new ArrayList<ACEEntityMention>();
			List<String> parallel_type_list = new ArrayList<String>();
			List<String> parallel_entity_list = new ArrayList<String>();
	
			for (int i = 0; i < entity_size; i++) {
				ACEEntity current = (ACEEntity) entity_list.get(i);
				
				for (int j = 0; j < current.entityMentionList.size(); j++ ) {
					ACEEntityMention current_entity_mention = current.entityMentionList.get(j);

					entity_mention_list.add(current_entity_mention);
					parallel_type_list.add(current.type);
					parallel_entity_list.add(current.id);
				}
				
			}
			
			int entity_mention_size = entity_mention_list.size();
			
			for (int i = 0; i < entity_mention_size; i++) {
				for (int j = i+1; j < entity_mention_size; j++) {
					//make entity pair into input format - input is two entity ids, types, and their distance from each other
					ACEEntityMention first = entity_mention_list.get(i);
					ACEEntityMention second = entity_mention_list.get(j);

					boolean match = ((parallel_entity_list.get(i)).equals(parallel_entity_list.get(j))); 
					
					Input x = getInput(first, second, parallel_type_list, parallel_entity_list, i, j);
					sp.addExample(x, new Output(match));
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

	public static List<SLProblem> readStructuredDataTest(
			ACEDocument doc, List<String> mention_id_list, Map<String, Integer> entity_size_map) {
		List<SLProblem> splist = new ArrayList<SLProblem>();
		
		List<?> entity_list = doc.aceAnnotation.entityList;
		int entity_size = entity_list.size();
		
		List<ACEEntityMention> entity_mention_list= new ArrayList<ACEEntityMention>();
		List<String> parallel_type_list = new ArrayList<String>();
		List<String> parallel_entity_list = new ArrayList<String>();

		for (int i = 0; i < entity_size; i++) {
			ACEEntity current = (ACEEntity) entity_list.get(i);
			entity_size_map.put(current.id, current.entityMentionList.size());
			
			for (int j = 0; j < current.entityMentionList.size(); j++ ) {
				ACEEntityMention current_entity_mention = current.entityMentionList.get(j);

				entity_mention_list.add(current_entity_mention);
				parallel_type_list.add(current.type);
				parallel_entity_list.add(current.id);
			}
			
		}
		
		int entity_mention_size = entity_mention_list.size();
		
		for (int i = 0; i < entity_mention_size; i++) {
			SLProblem sp = new SLProblem();
			ACEEntityMention second = entity_mention_list.get(i);
			mention_id_list.add(second.id);
			for (int j = 0; j < i; j++) {
				ACEEntityMention first = entity_mention_list.get(j);
				boolean match = ((parallel_entity_list.get(i)).equals(parallel_entity_list.get(j))); 
				
				Input x = getInput(first, second, parallel_type_list, parallel_entity_list, i, j);
				sp.addExample(x, new Output(match));
			}
			splist.add(sp);
		}
		
		return splist;
	}
	
	private static Input getInput(ACEEntityMention first, ACEEntityMention second, List<String> parallel_type_list, List<String> parallel_entity_list, int i, int j) {
		String id1 = first.id;
		String id2 = second.id;
		
		String type1 = parallel_type_list.get(i);
		String type2 = parallel_type_list.get(j);
		
		int distance1 = Math.abs(first.headEnd - second.headStart);
		int distance2 = Math.abs(first.headStart - second.headEnd);
		int distance = Math.min(distance1, distance2);
		
		String head1 = first.head;
		String head2 = second.head;
		
		String extent1 = first.extent;
		String extent2 = second.extent;
		
		String prevHead = first.extentStart < second.extentStart ? first.head : second.head;
		String secondHead = first.extentStart >= second.extentStart ? first.head : second.head;
		
		boolean substr = (prevHead.toLowerCase().contains(secondHead.toLowerCase()) );
		boolean match = ((parallel_entity_list.get(i)).equals(parallel_entity_list.get(j))); 
		
		Input x = new Input(id1, id2, type1, type2, distance, head1, head2, extent1, extent2, substr);
		return x;
	}
}
