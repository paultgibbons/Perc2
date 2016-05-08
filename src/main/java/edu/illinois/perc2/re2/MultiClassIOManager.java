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

import java.io.File;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class MultiClassIOManager {
	
	private static final String BASE_DIR="data/train/ACE05_English/";   // base directory for train/test files
	private static final String TRAINING="data/all/training_files.txt"; // file of training filenames
    
    public static List<ACEDocument> train;				// training documents
    public static List<ACEDocument> test;				// test documents
    public static List<String> 		training_filenames; // filenames of training documents (from Piazza)
    
    /**
     * Accessor method for training documents.
     * @return list of training documents
     */
    public static List<ACEDocument> getTrainingDocuments()  { return train; }
    
    /**
     * Accessor methods for test documents.
     * @return list of test documents
     */
    public static List<ACEDocument> getTestDocuments()		{ return test; 	}
    
    /**
     * Reads a corpus using the BASE_DIR directory path and TRAINING file list specified in MultiClassIOManager.
     * Initializes training and test document lists.
     * @param afp AceFileProcessor object to be used
     */
	public static void readCorpus(AceFileProcessor afp) {
		train = new ArrayList<ACEDocument>(); // initialize list of training documents
		test  = new ArrayList<ACEDocument>(); // initialize list of test documents
		
		// read in the list of training filenames to be used
		readTrainingFilenames(TRAINING);
		
		// read in files in bn, nw directories and add them to train/test splits as specified in training filename list
		for(String dir : new String[]{"bn","nw"}) {
			String folder = BASE_DIR + dir;
			readFolderAndAddToSplits(afp,folder);
		}
	}
	
	/**
	 * Reads in a list of training filenames that specify which files to add to the training split.
	 * @param fileName Filename of list of training filenames
	 */
	public static void readTrainingFilenames(String fileName) {
		training_filenames = new ArrayList<String>(); // initialize list of training filenames
		
		// add filenames to list of training filenames
		Path file = Paths.get(fileName);
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNext()) training_filenames.add(scanner.nextLine().trim());
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads in files from a directory and adds relevant training documents to the appropriate lists
	 * according to train/test splits.
	 * @param afp AceFileProcessor object to be used
	 * @param inputFolderStr Directory path of files to be read
	 */
	public static void readFolderAndAddToSplits (AceFileProcessor afp, String inputFolderStr) {
		File inputFolder = new File (inputFolderStr);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return fileName.endsWith(".apf.xml");
			}
		};
		
		File[] fileList = inputFolder.listFiles(filter);
		
		PrintStream out = System.out;
    	PrintStream err = System.err;
    	PrintStream devnull = new PrintStream(new OutputStream() {
            public void write(int b) {
                //DO NOTHING
            }
        });
			
		for (int fileID = 0; fileID < fileList.length; ++fileID) {

            String annotationFile = fileList[fileID].getAbsolutePath();
            String fileName       = fileList[fileID].getName();
            
            try{
            	System.setOut(devnull);
            	System.setErr(devnull);
            	ACEDocument doc = afp.processAceEntry(new File(inputFolderStr), annotationFile);
            	System.setOut(out);
            	System.setErr(err);
            	if (training_filenames.contains(fileName)) train.add(doc); // add to training split
            	else test.add(doc);										   // add to test split
            } catch (Exception e) {
            	System.setOut(out);
            	System.setErr(err);
            	System.err.println("Error on reading file: "+annotationFile);
            	System.err.println(e.toString());
            }
		}
	}

	/**
	 * Reads in a cost matrix from an input filename and initializes 2D float cost matrix.
	 * Uses the Lexiconer to find integer indices corresponding to the labels in the cost matrix file.
	 * @param lm Lexiconer to be used
	 * @param fname Cost matrix filename
	 * @return 2D float cost matrix
	 * @throws Exception If file fname cannot be read
	 */
	public static float[][] getCostMatrix(Lexiconer lm, String fname) throws Exception{
		System.out.println("Reading cost matrix: "+fname);
		int numLabels = lm.getNumOfLabels();
		float[][] res = new float[numLabels][numLabels];
		
		// initialize cost matrix
		for(int i=0;i  < numLabels ;i ++){
			for(int j=0; j < numLabels; j ++){
				if (i==j)
					res[i][j] = 0;
				else
					res[i][j] = 1.0f;
			}
		}
		
		ArrayList<String> lines = LineIO.read(fname);
		
		for(String line : lines){
			if (line.trim().charAt(0) == '#') // ignore comments
				continue;
			
			// check for incorrect formatting
			String[] tokens = line.split("\\s+");
			if (tokens.length != 3)
				throw new Exception("Format error in the cost matrix file.");
			if (!lm.containsLabel(tokens[0]))
				throw new Exception("Format error in the cost matrix file. Label (" + tokens[0] +") does not exist!"); 
			if (!lm.containsLabel(tokens[1]))
				throw new Exception("Format error in the cost matrix file. Label (" + tokens[1] +") does not exist!");
						
			int i = lm.getLabelId(tokens[0]); // gold label
			int j = lm.getLabelId(tokens[1]); // predicted label
			float cost = -1;
			
			// check for incorrect formatting of cost value
			try{
				cost = Float.parseFloat(tokens[2]);				
			} catch(NumberFormatException e){
				throw new Exception("Format error in the cost matrix file. The cost should be a number!");
			}
			
			if (i ==j && cost !=0 )
				throw new Exception("The cost should be zero when pred == gold.");
			
			if (cost < 0)
				throw new Exception("The cost cannot be negative.");
			
			// set cost
			res[i][j] = cost;
		}		
		
		System.out.println("Done! Cost matrix: ");
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[i].length; j++) {
				System.out.print(res[i][j]+" ");
			}
			System.out.println();
		}
		System.out.println();
		
		return res;
	}
	
	/**
	 * Reads structured data from a given list of documents. Adds features to a given Lexiconer (if training)
	 * and creates instances added to the SLProblem (using feature vector representation).
	 * @param docs List of documents to be read
	 * @param lm Lexiconer object to be used
	 * @param annotator Initialized annotator service to be used
	 * @return Initialized SLProblem with training instances initialized from the input documents
	 */
	public static SLProblem readStructuredData(List<ACEDocument> docs, Lexiconer lm, AnnotatorService annotator) {
		SLProblem sp = new SLProblem();
        
        if (lm.isAllowNewFeatures()) REUtils.addFeaturesToLexiconer(docs, lm, annotator);
        REUtils.createInstances(docs, lm, annotator, sp);
		
		return sp;
	}
}
