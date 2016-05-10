package edu.illinois.perc2.cr;

import java.io.IOException;
import java.util.List;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.morph.WordnetStemmer;

/**
 * 
 * @author ptgibbo2
 */
public class WordnetFeatures {
	public static int areSynonyms(IDictionary dict, WordnetStemmer stemmer, String w1, String w2) throws IOException {
		// construct the URL to the Wordnet dictionary directory
		
		IWord word;
		IWord word2;
		
		try {
			IIndexWord idxWord = dict.getIndexWord(stemmer.findStems(w1, POS.NOUN).get(0), POS.NOUN);
			IWordID wordID = idxWord.getWordIDs().get(0);
			IIndexWord idxWord2 = dict.getIndexWord(stemmer.findStems(w2, POS.NOUN).get(0), POS.NOUN);
			IWordID wordID2 = idxWord2.getWordIDs().get(0);
			word = dict.getWord(wordID);
			word2 = dict.getWord(wordID2);
		} catch (IllegalArgumentException e) {
			return 0;
		} catch (NullPointerException e) {
			return 0;
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
		String lemma = word2.getLemma();
		ISynset synset = word.getSynset();
        for (IWord w : synset.getWords()) {
        	if (w.getLemma().equals(lemma)) {
        		return 1;
        	}
        }
        return 0;
		
	}
	
	public static int areAntonyms(IDictionary dict, WordnetStemmer stemmer, String w1, String w2) throws IOException {
		// construct the URL to the Wordnet dictionary directory
		
		IWord word;
		IWord word2;
		
		try {
			IIndexWord idxWord = dict.getIndexWord(stemmer.findStems(w1, POS.NOUN).get(0), POS.NOUN);
			IWordID wordID = idxWord.getWordIDs().get(0);
			IIndexWord idxWord2 = dict.getIndexWord(stemmer.findStems(w2, POS.NOUN).get(0), POS.NOUN);
			IWordID wordID2 = idxWord2.getWordIDs().get(0);
			word = dict.getWord(wordID);
			word2 = dict.getWord(wordID2);
		} catch (IllegalArgumentException e) {
			return 0;
		} catch (NullPointerException e) {
			return 0;
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
		String lemma = word2.getLemma();
		List<IWordID> antonyms = word.getRelatedWords(Pointer.ANTONYM);
        for (IWordID wi : antonyms) {
        	IWord w = dict.getWord(wi);
        	if (w.getLemma().equals(lemma)) {
        		
        		return 1;
        	}
        }
        
        return 0;
		
	}
	
	public static int areHypernyms(IDictionary dict, WordnetStemmer stemmer, String w1, String w2) throws IOException {
		// TODO fix hypernyms
		
		if (true) {
			return 0;
		}
		
		IWord word;
		IWord word2;
		
		try {
			IIndexWord idxWord = dict.getIndexWord(stemmer.findStems(w1, POS.NOUN).get(0), POS.NOUN);
			IWordID wordID = idxWord.getWordIDs().get(0);
			IIndexWord idxWord2 = dict.getIndexWord(stemmer.findStems(w2, POS.NOUN).get(0), POS.NOUN);
			IWordID wordID2 = idxWord2.getWordIDs().get(0);
			word = dict.getWord(wordID);
			word2 = dict.getWord(wordID2);
		} catch (IllegalArgumentException e) {
			return 0;
		} catch (NullPointerException e) {
			return 0;
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
		String lemma = word2.getLemma();
		List<IWordID> antonyms = word.getRelatedWords(Pointer.HYPERNYM);
        for (IWordID wi : antonyms) {
        	IWord w = dict.getWord(wi);
        	if (w.getLemma().equals(lemma)) {
        		return 1;
        	}
        }
        return 0;
		
	}
	
	public static int caseMatch (IDictionary dict, WordnetStemmer stemmer, String w1, String w2) throws IOException {

		IWord word=null;
		IWord word2=null;
		
		try {
			IIndexWord idxWord = dict.getIndexWord(stemmer.findStems(w1, POS.NOUN).get(0), POS.NOUN);
			IWordID wordID = idxWord.getWordIDs().get(0);
			IIndexWord idxWord2 = dict.getIndexWord(stemmer.findStems(w2, POS.NOUN).get(0), POS.NOUN);
			IWordID wordID2 = idxWord2.getWordIDs().get(0);
			word = dict.getWord(wordID);
			word2 = dict.getWord(wordID2);
		} catch (IllegalArgumentException e) {
			return 0;
		} catch (NullPointerException e) {
			return 0;
		} catch (IndexOutOfBoundsException e) {
			return 0;
		}
		String lemma1 = word.getLemma();
		String lemma2 = word2.getLemma();
		
		boolean b1 = (lemma1.equals(w1));
		boolean b2 = (lemma2.equals(w2));
		
		return ((b1 && b2) || ((!b1) && (!b2))) ? 1 : 0;
	}
}
