package edu.illinois.perc2.re2;

import java.util.HashMap;
import java.util.Properties;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class Utils {
	
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
	 * Helper method to compute precision of a specified label.
	 * @param confusion_mtx Initialized confusion matrix
	 * @param label Label for which precision is to be computed
	 * @param lm Lexiconer managing label lexicon
	 * @return Precision of label given the confusion matrix
	 */
	public static double computePrecision(HashMap<String, HashMap<String, Integer>> confusion_mtx, String label, Lexiconer lm) {
		double mii = confusion_mtx.get(label).get(label);
		double mji = 0.0;
		
		for (int j = 0; j < lm.getNumOfLabels(); j++) mji += confusion_mtx.get(lm.getLabelString(j)).get(label);
		
		return mii / mji;
	}

	/**
	 * Helper method to compute precision of a specified label given values for true positives and false positives.
	 * @param tp Number of true positives
	 * @param fn Number of false positives
	 * @return Precision of label given true positives and false positives
	 */
	public static double computePrecision(int tp, int fp) {
		return tp / ((double) tp + fp);
	}
	
	/**
	 * Helper method to compute recall of a specified label given a confusion matrix.
	 * @param confusion_mtx Initialized confusion matrix
	 * @param label Label for which precision is to be computed
	 * @param lm Lexiconer managing label lexicon
	 * @return Recall of label given the confusion matrix
	 */
	public static double computeRecall(HashMap<String, HashMap<String, Integer>> confusion_mtx, String label, Lexiconer lm) {
		double mii = confusion_mtx.get(label).get(label);
		double mij = 0.0;
		
		for (int j = 0; j < lm.getNumOfLabels(); j++) mij += confusion_mtx.get(label).get(lm.getLabelString(j));
		
		return mii / mij;
	}
	
	/**
	 * Helper method to compute recall of a specified label given values for true positives and false negatives.
	 * @param tp Number of true positives
	 * @param fn Number of false negatives
	 * @return Recall of label given true positives and false negatives
	 */
	public static double computeRecall(int tp, int fn) {
		return tp / ((double) tp + fn);
	}
	
	/**
	 * Helper method to compute F1 score given precision and recall.
	 * @param precision Precision
	 * @param recall Recall
	 * @return F1 score
	 */
	public static double computeF1(double precision, double recall) {
		return 2 * (precision * recall) / (precision + recall);
	}
}
