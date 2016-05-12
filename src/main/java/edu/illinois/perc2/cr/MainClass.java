package edu.illinois.perc2.cr;

import java.io.File;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.FilenameFilter;
import java.net.URL;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;
import edu.illinois.cs.cogcomp.nlp.tokenizer.IllinoisTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.CcgTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

/**
 * 
 * @author ptgibbo2
 */
public class MainClass {
	private static final String BASE_DIR="data/train/ACE05_English/";
    public static Map<ACEDocument,Boolean> isTrain = new HashMap<ACEDocument, Boolean>();
	
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
            	isTrain.put(doc, TrainingData.isTraining(annotationFile));
            	System.setOut(out);
            	System.setErr(err);
            	docs.add(doc);
            } catch (Exception e) {
            	System.setOut(out);
            	System.setErr(err);
            	System.err.println("Error on reading flie: "+annotationFile);
            	System.err.println(e.toString());
            }
		}

		return docs;
	}
	
	public static List<ACEDocument> trainDocs(List<ACEDocument> docs) {
		List<ACEDocument> train = new ArrayList<ACEDocument>();
		for (int i = 0; i < (int) docs.size(); i++) {
			if (isTrain.get(docs.get(i)))
				train.add(docs.get(i));
		}
		return train;
	}
	
	public static List<ACEDocument> testDocs(List<ACEDocument> docs) {
		List<ACEDocument> train = new ArrayList<ACEDocument>();
		for (int i = 0; i < (int) docs.size(); i++) {
			if (!isTrain.get(docs.get(i)))
				train.add(docs.get(i));
		}
		return train;
	}
	
	public static List<ACEDocument> readAll(AceFileProcessor afp) {
		List<ACEDocument> docs = new ArrayList<ACEDocument>();
		for(String dir : new String[]{"bn","nw"}) {
			String folder = BASE_DIR + dir;
			docs.addAll(readFolder(afp,folder));
		}
		return docs;
	}
	
	public static void play() throws Exception {
		String wnhome = System.getenv("WNHOME");
		wnhome = "/usr/local/Cellar/wordnet/3.1";
		String path = wnhome + File.separator + "dict"; URL url = new URL("file", null, path);
		// construct the dictionary object and open it
		IDictionary dict = new Dictionary(url); dict.open();
		
		WordnetStemmer stemmer = new WordnetStemmer(dict);
		
		String s = stemmer.findStems("dog", POS.NOUN).get(0);
		
		IIndexWord idxWord = dict.getIndexWord(s, POS.NOUN);
		IWordID wordID = idxWord.getWordIDs().get(0);
		IWord word = dict.getWord(wordID);
		//List<IWordID> m = word.getRelatedWords(Pointer.ANTONYM);
		
		//System.out.println(dict.getWord( m.get(0) ).getLemma());
		
		System.out.println(word.getLemma());
		System.out.println(word.getSynset().getGloss());
		System.exit(0);
	}
	
	public static void play3() throws Exception {
		AceFileProcessor afp = new AceFileProcessor(new CcgTextAnnotationBuilder( new IllinoisTokenizer() ));
		List<ACEDocument> docs = readAll(afp);
		
		Marker m = new Marker();
		String colorPath = "/Users/paultgibbons/colored/color.html";
		for (ACEDocument doc : docs) {
			m.color(doc, colorPath);
		}
		System.exit(0);
	}
	
	
	public static void main(String[] args) throws Exception {
		//play3();
		boolean training = true;
		System.out.println("running corefernece resolution");
		AceFileProcessor afp = new AceFileProcessor(new CcgTextAnnotationBuilder( new IllinoisTokenizer() ));
		
		List<ACEDocument> docs = readAll(afp);
		//System.exit(0);
		List<ACEDocument> train = trainDocs(docs);
		List<ACEDocument> test = testDocs(docs);
		
		if (training) {
			CRMainClass.crMain(train);
			System.out.println("done training");
			
		} else {
			System.out.println("using pretrained model");
		}
		(new TestDoc()).evaluate(test);
		//CRMainClass.testCRModel(test, "models/CRModel1");
	}

}
