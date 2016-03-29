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

	public static void crMain(ACEDocument doc) throws Exception {
			String configFilePath = "config/CR.config";
			String modelPath = "models/CRmodel1";
			
			SLModel model = new SLModel();
			model.lm = new Lexiconer();

			SLProblem sp = readStructuredData(doc);

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
	
	public static void testCRModel(ACEDocument doc, String modelPath) throws Exception {
		SLModel model = SLModel.loadModel(modelPath);
		SLProblem sp = readStructuredData(doc);

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
			if (prediction.relationType.equals("NONE")) {
				falses += 1;
			}
			total += 1.0;
		}
		System.out.println("Acc = " + acc / total);
	}

	public static SLProblem readStructuredData(ACEDocument doc)
			throws IOException, DataFormatException {
		SLProblem sp = new SLProblem();

		
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
				
				int distance1 = Math.abs(start_map.get(id1) - start_map.get(id2));
				int distance2 = Math.abs(start_map.get(id2) - start_map.get(id1));
				int distance = Math.min(distance1, distance2);
				
				Input x = new Input(type1, type2, distance);
				// if there is a relation, add that type as output
				// else add null/none as output
				String relation_type = getRelationType(id1, id2, relation_list);
				
				sp.addExample(x, new Output(relation_type));
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
