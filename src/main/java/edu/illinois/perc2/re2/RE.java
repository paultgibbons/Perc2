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

import java.util.List;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.nlp.tokenizer.IllinoisTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLParameters;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.learner.l2_loss_svm.L2LossSSVMLearner;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.illinois.cs.cogcomp.sl.util.WeightVector;

public class RE {

	public static void main(String[] args) throws Exception {
		boolean training = true;

		AceFileProcessor afp = new AceFileProcessor(new TokenizerTextAnnotationBuilder(new IllinoisTokenizer()));
		MultiClassIOManager.readCorpus(afp);
		List<ACEDocument> train = MultiClassIOManager.getTrainingDocuments();
		List<ACEDocument> test  = MultiClassIOManager.getTestDocuments();
		
		AnnotatorService annotator = REUtils.initializeAnnotator();
		
		if (training) {
			RE.train(train, annotator);
			System.out.println("Done training.");
			
		} else {
			System.out.println("Using pretrained model.");
		}
		RE.testREModel(test, annotator, "models/REModel1");
	}
	
	public static void train(List<ACEDocument> docs, AnnotatorService annotator) throws Exception {
			String configFilePath = "config/CR.config";
			String modelPath 	  = "models/REmodel1";
			
			SLModel model = new SLModel();
			model.lm      = new Lexiconer();

			model.lm.setAllowNewFeatures(true);
			SLProblem sp = MultiClassIOManager.readStructuredData(docs, model.lm, annotator);

			// Disallow the creation of new features
			model.lm.setAllowNewFeatures(false);

			// initialize the inference solver
			model.infSolver = new MultiClassInferenceSolver(model.lm);

			model.featureGenerator = new MultiClassFeatureGenerator();
			SLParameters para = new SLParameters();
			para.loadConfigFile(configFilePath);
			para.TOTAL_NUMBER_FEATURE = model.lm.getNumOfFeature()
					* model.lm.getNumOfLabels() + model.lm.getNumOfLabels()
					+ model.lm.getNumOfLabels() * model.lm.getNumOfLabels();
			
			// numLabels*numLabels for transition features
			// numWordsInVocab*numLabels for emission features
			// numLabels for prior on labels
			Learner learner = LearnerFactory.getLearner(model.infSolver, model.featureGenerator,
					para);
			model.wv = learner.train(sp);
			WeightVector.printSparsity(model.wv);
			if(learner instanceof L2LossSSVMLearner)
				System.out.println("Primal objective:" + ((L2LossSSVMLearner)learner).getPrimalObjective(sp, model.wv, model.infSolver, para.C_FOR_STRUCTURE));

			// save the model
			model.saveModel(modelPath);
		
	}
	
	public static void testREModel(List<ACEDocument> docs, AnnotatorService annotator, String modelPath) throws Exception {
		SLModel model = SLModel.loadModel(modelPath);
		model.lm.setAllowNewFeatures(false);
		
		SLProblem sp  = MultiClassIOManager.readStructuredData(docs, model.lm, annotator);
		
		for (int i = 0; i < model.lm.getNumOfLabels(); i++) {
			System.out.println("Label "+i+": "+model.lm.getLabelString(i));
		}

		double acc = 0.0, nacc = 0.0;
		double total = 0.0, ntotal = 0.0;
		int falses = 0;

		for (int i = 0; i < sp.instanceList.size(); i++) {

			MultiClassLabel gold = (MultiClassLabel) sp.goldStructureList.get(i);
			MultiClassLabel prediction = (MultiClassLabel) model.infSolver.getBestStructure(
					model.wv, sp.instanceList.get(i));

			if (gold.output == prediction.output) {
				acc += 1.0;
				if (gold.output != model.lm.getLabelId("NONE")) nacc += 1.0;
			}
			if (prediction.output == model.lm.getLabelId("NONE")) {
				falses += 1;
			}
			total += 1.0;
			if (gold.output != model.lm.getLabelId("NONE")) ntotal += 1.0;
		}
		System.out.println("falses:" +falses+ ", total:" +total);
		System.out.println("Acc = " + acc / total);
		System.out.println("Accuracy on non-NONE labels: " + nacc/ntotal);
	}
}
