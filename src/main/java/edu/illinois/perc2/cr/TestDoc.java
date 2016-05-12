package edu.illinois.perc2.cr;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.morph.WordnetStemmer;

/**
 * 
 * @author ptgibbo2
 */
public class TestDoc {
	IDictionary dict;
	WordnetStemmer stemmer;
	AnnotatorService annotator;
	MentionDetector detector;
	
	public TestDoc() throws Exception {
		String wnhome = "/usr/local/Cellar/wordnet/3.1";
		String path = wnhome + File.separator + "dict"; URL url = new URL("file", null, path);
		// construct the dictionary object and open it
		this.dict = new Dictionary(url); dict.open();
		this.stemmer = new WordnetStemmer(dict);
		this.annotator = getAnnotator();
		this.detector = new MentionDetector();
	}
	
	@Override
	protected void finalize() throws Throwable {
		dict.close();
		super.finalize();
	}
	
	public AnnotatorService getAnnotator() {
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
        AnnotatorService _annotator = null;
        try {
            _annotator = IllinoisPipelineFactory.buildPipeline(rm);
            return _annotator;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
	}
	
	public boolean checkMention(Constituent NP, ACEEntityMention mention) {
		// return whether or not one fits inside the other
		return (NP.getStartCharOffset() <= mention.extentStart && 
				NP.getEndCharOffset()-1 >= mention.extentEnd) || (
				NP.getStartCharOffset() >= mention.extentStart && 
				NP.getEndCharOffset()-1 <= mention.extentEnd);
	}
	
	private SpanLabelView getNP(ACEDocument doc) throws Exception {
		Map<String, ACEEntityMention> startingSPACEending = new HashMap<>();
        for (ACEEntity entity : doc.aceAnnotation.entityList) {
        	for (edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention entityMention : entity.entityMentionList) {
        		startingSPACEending.put(entityMention.extentStart+" "+entityMention.extentEnd, entityMention);
        	}
        }
        
        //AnnotatorService annotator = getAnnotator();
		TextAnnotation ta = annotator.createBasicTextAnnotation("","",doc.contentRemovingTags); // create a TextAnnotation from the document text

		annotator.addView(ta,  ViewNames.SHALLOW_PARSE); 																				// add view for chunking information
		SpanLabelView v = (SpanLabelView) ta.getView(ViewNames.SHALLOW_PARSE);								  // access the view information

		//Map<String, Constituent> m = new HashMap<>();
		
		// view gives you a list of constituents, each of which has a surface string and a corresponding label ("NP", "VP", etc)
		List<Constituent> constituents = v.getConstituents();
		List<Constituent> bad = new ArrayList<>();
		for (int i = 0; i < constituents.size(); i++) {
			Constituent c = constituents.get(i);
			if (! detector.isMention(constituents,i) ) {
				bad.add(c);
			}
		}
		
		for (Constituent c : bad) {
			v.removeConstituent(c);
		}
		return v;
	}
	
	public List<ACEEntityMention> convertNPtoMention(SpanLabelView v) {
		List<ACEEntityMention> mentions = new ArrayList<>();
		for (Constituent c : v.getConstituents()) {
			ACEEntityMention mention = new ACEEntityMention();
			mention.extentStart = c.getStartCharOffset();
			mention.extentEnd = c.getEndCharOffset()-1;
			mention.extent = c.toString();
			mention.headStart = mention.extentStart;
			mention.headEnd = mention.extentEnd;
			mention.head = mention.extent;
			mention.type = "";
			mentions.add(mention);
		}
		return mentions;
	}
	
	/**
	 * 
	 * @param doc
	 * @return
	 */
	public List<Set<ACEEntityMention>> getGoldClusters (ACEDocument doc) {
		List<Set<ACEEntityMention>> clusters = new ArrayList<>();
		if (doc == null) {
			return clusters;
		}
		for (ACEEntity entity : doc.aceAnnotation.entityList) {
			Set<ACEEntityMention> cluster = new HashSet<>();
			for (ACEEntityMention mention : entity.entityMentionList) {
				cluster.add(mention);
			}
			clusters.add(cluster);
		}
		return clusters;
	}
	
	private List<ACEEntityMention> getGoldMentions(ACEDocument doc) {
		List<ACEEntityMention> mentions = new ArrayList<>();
		if(doc == null) {
			return mentions;
		}
		for (ACEEntity entity : doc.aceAnnotation.entityList) {
			for (ACEEntityMention mention : entity.entityMentionList) {
				// make it look like non gold mentions
				mention.headStart = mention.extentStart;
				mention.headEnd = mention.extentEnd;
				mention.head = mention.extent;
				mention.type = "";
				mentions.add(mention);
			}
		}
		return mentions;
	}
	
	private List<ACEEntityMention> getMentions(ACEDocument doc) throws Exception {
		SpanLabelView v = getNP(doc);
		return convertNPtoMention(v);
	}
	
	private Input getInput(ACEEntityMention first, ACEEntityMention second) throws Exception {
		String id1 = "";
		String id2 = "";
		
		String type1 = ""; // parallel_type_list.get(i);
		String type2 = ""; // parallel_type_list.get(j);
		
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
		//boolean match = ((parallel_entity_list.get(i)).equals(parallel_entity_list.get(j))); 
		
		int syn = WordnetFeatures.areSynonyms(dict, stemmer, head1, head2);
		int ant = WordnetFeatures.areAntonyms(dict, stemmer, head1, head2);
		int hyp = WordnetFeatures.areHypernyms(dict, stemmer, head1, head2);
		int cas = WordnetFeatures.caseMatch(dict, stemmer, head1, head2);
		
		Input x = new Input(id1, id2, type1, type2, distance, head1, head2, extent1, extent2, substr, syn, ant, hyp, cas);
		
		x.mention1 = first;
		x.mention2 = second;
		
		return x;
	}
	
	/**
	 * 
	 * @param doc
	 * @return the structured learning problem of pairs of mentions to a boolean (false for testing)
	 * @throws Exception 
	 */
	private List<Problem> getProblems(List<ACEEntityMention> mentions) throws Exception {
		List<Problem> problemList = new ArrayList<>();
		for (int i = 0; i < mentions.size(); i++) {
			ACEEntityMention mention = mentions.get(i);
			SLProblem sp = new SLProblem();
			for (int j = 0; j < i; j++) {
				ACEEntityMention mention2 = mentions.get(j);
				IInstance instance = getInput(mention, mention2);
				IStructure goldStructure = new Output(false);
				sp.addExample(instance, goldStructure);
			}
			problemList.add(new Problem(mention, sp));
		}
		return problemList;
	}
	
	/**
	 * Predict SLProblem and then cluster into clusters
	 * @param sp
	 * @return
	 * @throws Exception
	 */
	private List<Set<ACEEntityMention>> predict(List<Problem> problems) throws Exception {
		SLModel model = SLModel.loadModel("models/CRmodel1");
		List<Set<ACEEntityMention>> clusters = new ArrayList<Set<ACEEntityMention>>();
		for (Problem problem : problems) {
			assignMention(problem, model, clusters);
		}
		return clusters;
	}
	
	private void assignMention(Problem problem, SLModel model,
			List<Set<ACEEntityMention>> clusters) throws Exception {
		SLProblem sp = problem.sp;
		ACEEntityMention mention = problem.mention;
		
		double[] scores = new double[clusters.size()];
		int[] counts = new int[clusters.size()];
		
		double best_score = Double.NEGATIVE_INFINITY;
		int best_cluster_index = -1;
		for (IInstance instance : sp.instanceList) {
			Output prediction = (Output) model.infSolver.getBestStructure(model.wv, instance);
			int index = getAssociatedIndex(clusters,prediction.mention2);
			scores[index] += prediction.trueScore;
			counts[index] += 1;
		}
		for (int i = 0; i < clusters.size(); i++) {
			double average = (scores[i] / counts[i]);
			if (average > best_score) {
				best_score = average;
				best_cluster_index = i;
			}
		}
		// add mention id to correct cluster
		if (best_score <= 0.5) {
			Set<ACEEntityMention> h = new HashSet<>();
			h.add(mention);
			clusters.add(h);
		} else {
			clusters.get(best_cluster_index).add(mention);
		}
		
	}
	
	private int getAssociatedIndex(List<Set<ACEEntityMention>> clusters, ACEEntityMention mention1id) {
		for(int i = 0; i < clusters.size(); i++) {
			if (clusters.get(i).contains(mention1id)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * A predicted mention matches a glod mention if one contains the other
	 * @param predicted
	 * @param gold
	 * @return
	 */
	private boolean match (ACEEntityMention predicted, ACEEntityMention gold) {
		boolean match = (predicted.extentStart <= gold.extentStart && 
				predicted.extentEnd >= gold.extentEnd) || (
				predicted.extentStart >= gold.extentStart && 
				predicted.extentEnd <= gold.extentEnd);
		return match;
	}
	
	private double getMatchCount(Set<ACEEntityMention> cluster,
			Set<ACEEntityMention> goldCluster) {
		double numerator = 0.0;
		for (ACEEntityMention predictedMention : cluster) {
			boolean matched = false;
			for (ACEEntityMention goldMention : goldCluster) {
				if (match(predictedMention, goldMention)) {
					matched = true;
					break;
				}
			}
			if (matched) {
				numerator += 1.0;
			}
		}
		return numerator;
	}
	
	private Set<ACEEntityMention> findGoldCluster(ACEEntityMention predicted,
			List<Set<ACEEntityMention>> goldClusters) {
		for ( Set<ACEEntityMention> cluster : goldClusters ) {
			for ( ACEEntityMention goldMention : cluster ) {
				if (match(predicted,goldMention)) {
					return cluster;
				}
			}
		}
		return null;
	}

	/**
	 * Evaluation of a single document
	 * @param doc The document
	 * @param sp
	 * @throws Exception 
	 */
	public void evaluateDocument(ACEDocument doc, List<Problem> problems, double[] metrics) throws Exception {
		List<Set<ACEEntityMention>> goldClusters = getGoldClusters(doc);
		List<Set<ACEEntityMention>> predictedClusters = predict(problems);
		
		metrics[4] += (double) goldClusters.size();
		for ( Set<ACEEntityMention> cluster : predictedClusters ) {
			// for each cluster in the document
			for ( ACEEntityMention mention : cluster ) {
				// for each mention in the cluster
				Set<ACEEntityMention> goldCluster = findGoldCluster(mention, goldClusters);
				if (goldCluster == null) {
					metrics[0] += 1.0; // no matching cluster
				} else {
					double numerator = getMatchCount(cluster, goldCluster);
					metrics[1] += numerator / cluster.size(); // precision
					metrics[2] += numerator / goldCluster.size(); // recall
					metrics[3] += 1.0; // total
				}
			}
		}
	}

	public void evaluate(List<ACEDocument> docs) throws Exception {
		double[] metrics = new double[5];
		
		int i = 0;
		
		for(ACEDocument doc : docs) {
			if (doc == null) {
				System.err.println("null doc");
			} else {
				System.out.println("On doc "+(++i));
				List<Problem> problems = getProblems(getMentions(doc));
				evaluateDocument(doc,problems,metrics);
			}
		}
		
		int total = (int) metrics[3];
		double presum = metrics[1];
		double resum = metrics[2];
		int unmatched = (int) metrics[0];
		int gold_size = (int) metrics[4];
		
		System.out.println("Unmatched: " + unmatched);
		System.out.println("Total matched: " + total);
		System.out.println("Gold mention list size: " + gold_size);
		double precision = (presum/total);
		System.out.println("Precision: " + precision);
		double recall= (resum/total);
		System.out.println("Recall: " + recall);
		double f1 = 2 * (precision * recall) / (precision + recall);
		System.out.println("F1: " + f1);
	}
}
