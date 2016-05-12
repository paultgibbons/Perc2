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

import java.util.HashMap;
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
		boolean use_ner  = true;

		AceFileProcessor afp = new AceFileProcessor(new TokenizerTextAnnotationBuilder(new IllinoisTokenizer()));
		MultiClassIOManager.readCorpus(afp,use_ner);
		List<ACEDocument> train      = MultiClassIOManager.getTrainingDocuments();
		List<ACEDocument> test_gold  = MultiClassIOManager.getGoldTestDocuments();
		List<ACEDocument> test_ner   = MultiClassIOManager.getNERTestDocuments();
		
		System.out.println(train.size()+" training documents read.");
		System.out.println(test_gold.size()+" gold test documents read.");
		System.out.println(test_ner.size()+" NER test documents read.");
		
		AnnotatorService annotator = REUtils.initializeAnnotator();
		
		if (training) {
			RE.train(train, annotator, use_ner);
			System.out.println("Done training.");
			
		} else {
			System.out.println("Using pretrained model.");
		}
		
		RE.test(test_gold, test_ner, annotator, use_ner, "models/REModel1");
	}
	
	/**
	 * Trains a model with given list of training documents and an initialized annotator service.
	 * @param docs List of training documents
	 * @param annotator Initialized annotator service
	 * @throws Exception 
	 */
	public static void train(List<ACEDocument> docs, AnnotatorService annotator, boolean ner) throws Exception {
			String configFilePath = "config/RE.config";
			String modelPath 	  = "models/REmodel1";
			
			MultiClassModel model = new MultiClassModel();
			model.lm = new Lexiconer();

			model.lm.setAllowNewFeatures(true);
			SLProblem sp = MultiClassIOManager.readStructuredData(docs, model.lm, annotator, false, ner);

			// Disallow the creation of new features
			model.lm.setAllowNewFeatures(false);
			
			// initialize cost matrix
			model.cost_matrix = MultiClassIOManager.getCostMatrix(model.lm, "models/cost/cost_matrix.txt");

			// initialize the inference solver
			model.infSolver = new MultiClassInferenceSolver(model.cost_matrix);

			model.featureGenerator = new MultiClassFeatureGenerator();
			SLParameters para = new SLParameters();
			para.loadConfigFile(configFilePath);

			Learner learner = LearnerFactory.getLearner(model.infSolver, model.featureGenerator,
					para);
			model.wv = learner.train(sp);
			WeightVector.printSparsity(model.wv);
			if(learner instanceof L2LossSSVMLearner)
				System.out.println("Primal objective:" + ((L2LossSSVMLearner)learner).getPrimalObjective(sp, model.wv, model.infSolver, para.C_FOR_STRUCTURE));

			// save the model
			model.saveModel(modelPath);
		
	}
	
	/**
	 * Tests a trained model read in from a given model path on a list of test documents, using an initialized
	 * annotator service.
	 * @param docs List of test documents
	 * @param annotator Initialized annotator service
	 * @param modelPath File path of the saved model
	 * @throws Exception
	 */
	public static void test(List<ACEDocument> gold_docs, List<ACEDocument> ner_docs, AnnotatorService annotator, boolean ner, String modelPath) throws Exception {
		MultiClassModel model = (MultiClassModel) SLModel.loadModel(modelPath);
		model.lm.setAllowNewFeatures(false);
		
		List<ACEDocument> docs = (ner ? ner_docs : gold_docs);
		MultiClassIOManager.readStructuredData(gold_docs, model.lm, annotator, false, true);
		SLProblem sp  = MultiClassIOManager.readStructuredData(docs, model.lm, annotator, ner, ner);
		if (ner) REUtils.initializeSortedMentionList(gold_docs, annotator);

		double acc   = 0.0, nacc   = 0.0;
		double total = 0.0, ntotal = 0.0;
		int falses = 0;
		
		HashMap<String, HashMap<String, Integer>> confusion_mtx = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, Integer> tp = new HashMap<String, Integer>();
		HashMap<String, Integer> fp = new HashMap<String, Integer>();
		HashMap<String, Integer> fn = new HashMap<String, Integer>();
		
		System.out.println();
		for (int i = 0; i < model.lm.getNumOfLabels(); i++) {
			System.out.println("Label "+i+": "+model.lm.getLabelString(i));
			confusion_mtx.put(model.lm.getLabelString(i), new HashMap<String, Integer>());
			for (int j = 0; j < model.lm.getNumOfLabels(); j++) {
				confusion_mtx.get(model.lm.getLabelString(i)).put(model.lm.getLabelString(j), 0);
				tp.put(model.lm.getLabelString(j), 0);
				fp.put(model.lm.getLabelString(j), 0);
				fn.put(model.lm.getLabelString(j), 0);
			}
		}

		for (int i = 0; i < sp.instanceList.size(); i++) {
			MultiClassInstance ri   = (MultiClassInstance) sp.instanceList.get(i);
			MultiClassLabel    pred = (MultiClassLabel)    model.infSolver.getBestStructure(model.wv, ri);
			MultiClassLabel    gold = null;
			
			if (ner) gold = new MultiClassLabel(model.lm.getLabelId(REUtils.getMappedRelation(ri)));
			gold = (MultiClassLabel) sp.goldStructureList.get(i);
			
			String gold_label = model.lm.getLabelString(gold.output);
			String pred_label = model.lm.getLabelString(pred.output);
			
			confusion_mtx.get(gold_label).put(pred_label, confusion_mtx.get(gold_label).get(pred_label)+1);
			
			if (gold.output == pred.output) {
				acc += 1.0;
				if (gold.output != model.lm.getLabelId(Features.REL+"NONE")) nacc += 1.0;
				tp.put(gold_label, tp.get(gold_label)+1);
			}
			else {
				fp.put(pred_label, fp.get(pred_label)+1);
				fn.put(gold_label, fn.get(gold_label)+1);
			}
			
			if (pred.output == model.lm.getLabelId(Features.REL+"NONE")) {
				falses += 1;
			}
			total += 1.0;
			if (gold.output != model.lm.getLabelId(Features.REL+"NONE")) ntotal += 1.0;
		}
		System.out.println("Unmapped NER mentions: "+REUtils.unmapped_mentions);
		
		System.out.println("\nNONE predictions: " +falses+ ", total: " +total);
		System.out.println("Accuracy: " + acc / total);
		System.out.println("Accuracy on non-NONE labels: " + nacc/ntotal);
		System.out.println();
		
		for (String g : confusion_mtx.keySet()) {
			HashMap<String, Integer> m = confusion_mtx.get(g);
			
			double precision = REUtils.computePrecision(confusion_mtx, g, model.lm);
			double recall    = REUtils.computeRecall(confusion_mtx, g, model.lm);
			double F1        = REUtils.computeF1(precision, recall);
			System.out.println("GOLD: "+g+"\tPrecision: "+String.format("%1$-7.5f", precision)+" | Recall: "+String.format("%1$-7.5f", recall)+" | F1: "+String.format("%1$-7.5f", F1));
			
			for (String p : m.keySet()) {
				System.out.println("\tPRED: "+String.format("%1$-10s\t", p)+m.get(p)+(g.equals(p) ? "\t<correct>" : ""));
			}
			System.out.println();
		}
		
		int true_positive = 0, false_positive = 0, false_negative = 0;
		for (String k : tp.keySet()) if (!k.contains("NONE")) true_positive  += tp.get(k);
		for (String k : fp.keySet()) if (!k.contains("NONE")) false_positive += fp.get(k);
		for (String k : fn.keySet()) if (!k.contains("NONE")) false_negative += fn.get(k);
		
		double precision = REUtils.computePrecision(true_positive, false_positive);
		double recall	 = REUtils.computeRecall(true_positive, false_negative);
		
		System.out.println("\nOverall precision -NONE: "+precision);
		System.out.println("Overall recall -NONE:    "+recall);
		System.out.println("Overall F1 -NONE:        "+REUtils.computeF1(precision, recall));
		
		true_positive = 0; false_positive = 0; false_negative = 0;
		for (String k : tp.keySet()) true_positive  += tp.get(k);
		for (String k : fp.keySet()) false_positive += fp.get(k);
		for (String k : fn.keySet()) false_negative += fn.get(k);
		
		precision = REUtils.computePrecision(true_positive, false_positive);
		recall	 = REUtils.computeRecall(true_positive, false_negative);
		
		System.out.println("\nOverall precision: "+precision);
		System.out.println("Overall recall:    "+recall);
		System.out.println("Overall F1:        "+REUtils.computeF1(precision, recall));
	}
}
