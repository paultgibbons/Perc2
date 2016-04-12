package edu.illinois.perc2.cr;

import java.io.File;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.FilenameFilter;

import edu.illinois.cs.cogcomp.nlp.tokenizer.IllinoisTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.CcgTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;

/**
 * 
 * @author ptgibbo2
 */
public class MainClass {
	private static final String BASE_DIR="data/train/ACE05_English/";
    private static final String TEST_DIR="data/train/ACE05_English/nw";
	
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
		for (int i = 0; i < (int) (docs.size() * 0.8); i++) {
			train.add(docs.get(i));
		}
		return train;
	}
	
	public static List<ACEDocument> testDocs(List<ACEDocument> docs) {
		List<ACEDocument> train = new ArrayList<ACEDocument>();
		for (int i = (int) (docs.size() * 0.8) + 1; i < docs.size(); i++) {
			train.add(docs.get(i));
		}
		return train;
	}
	
	public static List<ACEDocument> readAll(AceFileProcessor afp) {
		List<ACEDocument> docs = new ArrayList<ACEDocument>();
		for(String dir : new String[]{"bc","bn","cts","nw","un","wl"}) {
			String folder = BASE_DIR + dir;
			docs.addAll(readFolder(afp,folder));
		}
		return docs;
	}
	
	
	public static void main(String[] args) throws Exception {
		boolean training = false;
		System.out.println("running corefernece resolution");
		AceFileProcessor afp = new AceFileProcessor(new CcgTextAnnotationBuilder( new IllinoisTokenizer() ));
		
//		List<ACEDocument> docs = readFolder(afp, TEST_DIR);
		
		List<ACEDocument> docs = readAll(afp);
		
		List<ACEDocument> train = trainDocs(docs);
		List<ACEDocument> test = testDocs(docs);
		
		if (training) {
			CRMainClass.crMain(train);
			System.out.println("done training");
			
		} else {
			System.out.println("using pretrained model");
		}
		CRMainClass.testCRModel(test, "models/CRModel1");
	}

}
