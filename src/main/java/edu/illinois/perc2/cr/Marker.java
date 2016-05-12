package edu.illinois.perc2.cr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;

/**
 * 
 * @author ptgibbo2
 */
public class Marker {
	private List<Set<ACEEntityMention>> rando(List<ACEEntityMention> mentions) {
		List<Set<ACEEntityMention>> clusters = new ArrayList<>();
		clusters.add(new HashSet<ACEEntityMention>());
		clusters.add(new HashSet<ACEEntityMention>());
		clusters.add(new HashSet<ACEEntityMention>());
		clusters.add(new HashSet<ACEEntityMention>());
		for (ACEEntityMention mention : mentions) {
			double r = Math.random();
			if (r > 0.75) {
				clusters.get(0).add(mention);
			} else if (r > 0.5) {
				clusters.get(1).add(mention);
			} else if (r > 0.25) {
				clusters.get(2).add(mention);
			} else {
				clusters.get(3).add(mention);
			}
		}
		return clusters;
	}
	
	public void color(ACEDocument doc, String colorPath) throws Exception {
		TestDoc ex = new TestDoc();
		SpanLabelView v = ex.getNP(doc);
		List<ACEEntityMention> mentions = ex.convertNPtoMention(v);
		List<Set<ACEEntityMention>> clusters = rando(mentions);
		color(doc, clusters, colorPath);
	}
	
	private String getRawText(ACEDocument doc) {
		StringBuilder sb = new StringBuilder();
		for (int i = 2; i < doc.paragraphs.size(); i++) {
			sb.append(doc.paragraphs.get(i).getSecond().content );
			sb.append("  \n");
		}
		return sb.toString();
		
	}
	
	public void color(ACEDocument doc, List<Set<ACEEntityMention>> clusters, String colorFolder) throws Exception {
		
		StringBuilder content = new StringBuilder(); 
		content.append("<html><head></head><body><p>");
		
		String documentRawText = getRawText(doc);
		int startIndex = doc.contentRemovingTags.indexOf("     ")+5;
		List<ACEEntityMention> mentions = ColorList.AssignColorsAndSort(clusters);
		
		int currentIndex = 0;
		
		// for each ACEEntityMention
		for (ACEEntityMention mention : mentions) {
			// Add text from current index to the start index
			String before;
			try {
				before = documentRawText.substring(currentIndex, mention.extentStart-startIndex);
			} catch (StringIndexOutOfBoundsException e) {
				continue;
			}
			content.append(before);
			// Add in span version of text
			content.append("<font color='");
			content.append(mention.ldcType);
			content.append("'>");
			content.append(mention.extent);
			content.append("</font>");
			// Set current index to the end index of the mention
			currentIndex = mention.extentEnd-startIndex+1;
		}
		
		try {
			content.append(documentRawText.substring(currentIndex));
		} catch (StringIndexOutOfBoundsException e) {
			content.append("<eof>");
		}
		
		content.append("</p></body></html>");
		
		try {
			File file = new File(colorFolder+"/"+(doc.aceAnnotation.id)+".html");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content.toString());
			bw.flush();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sortMention() {
		
	}
	
	public void colorGold() {
		
	}
}
