package edu.illinois.perc2.re;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.ObjectOutputStream;
import java.util.HashSet;

import edu.illinois.cs.cogcomp.nlp.tokenizer.IllinoisTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.CcgTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocumentAnnotation;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.ReadACEAnnotation;
import edu.illinois.cs.cogcomp.reader.commondatastructure.XMLException;
import edu.illinois.perc2.cr.CRMainClass;

//import edu.illinois.cs.cogcomp.sl.core.*;

/**
 * 
 * @author ptgibbo2
 */
public class MainClass {
    private static final String TEST_DIR="data/train/ACE05_English/nw";
    private static final String TEST_FILE="XIN_ENG_20030616.0274.apf.xml";
	
	static String[] failureFileList = new String[] {
		"MARKBACKER_20050105.1526",
		"FLOPPINGACES_20050203.1953.038",
		"FLOPPINGACES_20041228.0927.010",
		"MARKETVIEW_20050209.1923",
		"fsh_29786",
		"fsh_29195",
		"fsh_29303",
		"APW_ENG_20030610.0554",
		"APW_ENG_20030422.0469", 
		"NYT_ENG_20030630.0079",
		"APW_ENG_20030519.0548",
		"CNN_ENG_20030526_183538.3",
		"CNN_ENG_20030602_105829.2",
		"CNN_ENG_20030415_103039.0",
		"CNN_ENG_20030327_163556.20",
		"CNNHL_ENG_20030312_150218.13",
		"CNN_ENG_20030428_193655.2",
		"CNN_ENG_20030428_193655.2",
		"CNNHL_ENG_20030304_142751.10",
		"CNNHL_ENG_20030611_133445.24",
		"CNN_ENG_20030527_215946.12",
		"CNN_ENG_20030507_170539.0",
		"CNN_ENG_20030424_070008.15",
		"CNN_ENG_20030602_133012.9",
		"CNN_ENG_20030529_130011.6",
		"CNN_ENG_20030430_093016.0",
		"CNN_ENG_20030607_170312.6",
		"CNN_ENG_20030622_173306.9",
		"CNN_ENG_20030611_102832.4",
		"CNN_ENG_20030416_180808.15",
		"CNN_ENG_20030528_125956.8",
		"marcellapr_20050211.2013",
		"rec.travel.usa-canada_20050128.0121",
		"soc.culture.china_20050203.0639",
		"alt.atheism_20041104.2428",
		"alt.vacation.las-vegas_20050109.0133",
	};
	
	public static List<ACEDocument> readFolder (AceFileProcessor afp, String inputFolderStr) {
		File inputFolder = new File (inputFolderStr);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return fileName.endsWith(".apf.xml");
			}
		};
		File[] fileList = inputFolder.listFiles(filter);
		
		List<ACEDocument> docs = new ArrayList<ACEDocument>(); 
			
		for (int fileID = 0; fileID < fileList.length; ++fileID) {

            String annotationFile = fileList[fileID].getAbsolutePath();

            ACEDocument doc = afp.processAceEntry(new File(inputFolderStr), annotationFile);
            docs.add(doc);
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
	
	
	
	public static void main(String[] args) throws Exception {
		boolean training = true;

		AceFileProcessor afp = new AceFileProcessor(new CcgTextAnnotationBuilder( new IllinoisTokenizer() ));
		ACEDocument doc = afp.processAceEntry(new File(TEST_DIR), TEST_DIR + "/" + TEST_FILE);
		
		List<ACEDocument> docs = new ArrayList<ACEDocument>();
		docs.add(doc);
		
		if (training) {
			CRMainClass.crMain(docs);
			System.out.println("done training");
			
		} else {
			System.out.println("using pretrained model");
		}
		REMainClass.testREModel(docs, "models/REModel1");
	}

}
