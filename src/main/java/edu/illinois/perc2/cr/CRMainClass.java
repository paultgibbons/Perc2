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
import java.util.List;
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

			if (gold.areCoReferencing == prediction.areCoReferencing) {
				acc += 1.0;
			}
			if (!prediction.areCoReferencing) {
				falses += 1;
			}
			total += 1.0;
		}
		System.out.println("Acc = " + acc / total);
	}

	public static SLProblem readStructuredData(ACEDocument doc)
			throws IOException, DataFormatException {
		SLProblem sp = new SLProblem();

		// w:unknownword indicates out-of-vocabulary words in test phase
		//if (lm.isAllowNewFeatures())
		//	lm.addFeature("w:unknownword");
		
		List<?> entity_list = doc.aceAnnotation.entityList;
		int entity_size = entity_list.size();
		
		List<ACEEntityMention> entity_mention_list= new ArrayList<ACEEntityMention>();
		List<String> parallel_type_list = new ArrayList<String>();
		List<String> parallel_entity_list = new ArrayList<String>();

		for (int i = 0; i < entity_size; i++) {
			ACEEntity current = (ACEEntity) entity_list.get(i);
			for (int j = 0; j < current.entityMentionList.size(); j++ ) {
				entity_mention_list.add(current.entityMentionList.get(j) );
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
				
				String id1 = first.id;
				String id2 = second.id;
				
				String type1 = parallel_type_list.get(i);
				String type2 = parallel_type_list.get(j);
				
				int distance1 = Math.abs(first.headEnd - second.headStart);
				int distance2 = Math.abs(first.headStart - second.headEnd);
				int distance = Math.min(distance1, distance2);
				
				boolean match = ((parallel_entity_list.get(i)).equals(parallel_entity_list.get(j))); 
				
				Input x = new Input(id1, id2, type1, type2, distance);
				
				// add tags to lm - may need to use this to make integer ids?
				
				// create output - output for relations:
				//   is really just the two ids, a boolean yes or no, and a type if the boolean is yes
				// currently just need a boolean yes or no, do the coreference
				// add example
				sp.addExample(x, new Output(match));
			}
		}

		return sp;
	}
}
