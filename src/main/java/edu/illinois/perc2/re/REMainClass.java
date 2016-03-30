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
package edu.illinois.perc2.re;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.commands.CommandDescription;
import edu.illinois.cs.cogcomp.core.utilities.commands.InteractiveShell;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelation;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLParameters;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.learner.l2_loss_svm.L2LossSSVMLearner;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;

public class REMainClass {

	public static void reMain(List<ACEDocument> docs) throws Exception {
			String configFilePath = "config/CR.config";
			String modelPath = "models/REmodel1";
			
			SLModel model = new SLModel();
			model.lm = new Lexiconer();

			SLProblem sp = readStructuredData(docs, model.lm);

			// Disallow the creation of new features
			model.lm.setAllowNewFeatures(false);

			// initialize the inference solver
			model.infSolver = new InferenceSolver(model.lm);

			FeatureGenerator fg = new FeatureGenerator(model.lm);
			SLParameters para = new SLParameters();
			para.loadConfigFile(configFilePath);
			para.TOTAL_NUMBER_FEATURE = model.lm.getNumOfFeature()
					* model.lm.getNumOfLabels() + model.lm.getNumOfLabels()
					+ model.lm.getNumOfLabels() * model.lm.getNumOfLabels();
			
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
	
	public static void testREModel(List<ACEDocument> docs, String modelPath) throws Exception {
		SLModel model = SLModel.loadModel(modelPath);
		SLProblem sp = readStructuredData(docs, model.lm);

		double acc = 0.0;
		double total = 0.0;
		int falses = 0;
		for (int i = 0; i < sp.instanceList.size(); i++) {

			Output gold = (Output) sp.goldStructureList.get(i);
			Output prediction = (Output) model.infSolver.getBestStructure(
					model.wv, sp.instanceList.get(i));

			if (gold.relationType == prediction.relationType) {
				acc += 1.0;
			}
			if (prediction.relationType == model.lm.getFeatureId("l:unknownlabel")) {
				falses += 1;
			}
			total += 1.0;
		}
		System.out.println("falses:" +falses+ ", total:" +total);
		System.out.println("Acc = " + acc / total);
	}

	public static SLProblem readStructuredData(List<ACEDocument> docs, Lexiconer lm)
			throws IOException, DataFormatException {
		SLProblem sp = new SLProblem();

		for(ACEDocument doc : docs) {
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
		
		if (lm.isAllowNewFeatures()) {
	        lm.addFeature("W:unknownword");
	        lm.addFeature("l:unknownlabel");
		}
		
		// for each pair of entities
		for (int i = 0; i < entity_size; i++) {
			for (int j = i+1; j < entity_size; j++) {
				// get features
				ACEEntity e1 = ((ACEEntity) entity_list.get(i));
				ACEEntity e2 = ((ACEEntity) entity_list.get(j));
				String id1 = e1.id;
				String id2 = e2.id;
				
				String type1 = e1.type;
				String type2 = e2.type;
				
				if (lm.isAllowNewFeatures()) {
				    lm.addFeature("w:" + type1);
				    lm.addFeature("w:" + type2);
				}
				
				int type1i = (lm.containFeature("w:" + type1)) ? lm.getFeatureId("w:"+type1) : lm.getFeatureId("W:unknownword");
				int type2i = (lm.containFeature("w:" + type2)) ? lm.getFeatureId("w:"+type2) : lm.getFeatureId("W:unknownword");
				
				int distance1 = Math.abs(start_map.get(id1) - start_map.get(id2));
				int distance2 = Math.abs(start_map.get(id2) - start_map.get(id1));
				int distance = Math.min(distance1, distance2);
				
				Input x = new Input(type1i, type2i, distance);
				// if there is a relation, add that type as output
				// else add null/none as output
				String relation_type = getRelationType(id1, id2, relation_list);
				if (lm.isAllowNewFeatures()) {
				    lm.addFeature("l:" + relation_type);
				}
				
				int relation_typei = (lm.containFeature("l:" + relation_type)) ? lm.getFeatureId("") : lm.getFeatureId("W:unknownword");
				
				sp.addExample(x, new Output(relation_typei));
			}
		}
		}
		return sp;
	}

	private static String getRelationType(String id1, String id2,
			List<?> relation_list) {
		for (int i = 0; i < relation_list.size(); i++) {
			ACERelation current = (ACERelation) relation_list.get(i);
			if (
					(current.relationArgumentList.get(0).id.equals(id1) && current.relationArgumentList.get(1).id.equals(id2))
					||
					(current.relationArgumentList.get(1).id.equals(id1) && current.relationArgumentList.get(0).id.equals(id2))
				) {
				return current.type; 
			}
			
		}
		return "NONE";
	}
}
