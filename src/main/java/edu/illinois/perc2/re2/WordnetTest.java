package edu.illinois.perc2.re2;

import java.io.File;
import java.net.URL;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class WordnetTest {
	public static void main(String[] args) throws Exception {
		String wnhome = "/usr/local/Cellar/wordnet/3.1";
		String path   = wnhome+File.separator+"dict";
		URL    url    = new URL("file", null, path);
		
		IDictionary dict = new Dictionary(url);
		dict.open();
		
		IIndexWord idxWord = dict.getIndexWord("dog", POS.NOUN);
		IWordID    wordID  = idxWord.getWordIDs().get(0);
		IWord      word    = dict.getWord(wordID);
		System.out.println("Id = "+wordID);
		System.out.println("Lemma = "+word.getLemma());
		System.out.println("Gloss = "+word.getSynset().getGloss());
	}
}
