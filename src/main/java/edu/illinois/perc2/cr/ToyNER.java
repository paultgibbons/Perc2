package edu.illinois.perc2.cr;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import edu.illinois.cs.cogcomp.openeval.learner.Server;
import edu.illinois.cs.cogcomp.openeval.learner.ServerPreferences;
import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.nlp.utilities.POSUtils;

public class ToyNER extends Annotator
{
    public ToyNER()
    {
        // The problem we are trying to solve is parts of speech (POS)
        // The view we require is TOKENS
        super(ViewNames.COREF , new String[] {ViewNames.SENTENCE, ViewNames.TOKENS});
    }

    @Override
    public void addView(TextAnnotation textAnnotation) throws AnnotatorException
    {
    	System.out.println(textAnnotation.text);
        String[] tokens = textAnnotation.getTokens();
        for (String token : tokens) {
        	System.out.println(token);
        }
        //textAnnotation.addView(ViewNames.COREF, new View(viewName, viewName, textAnnotation, 0) );
        List<String> tags = POSUtils.allPOS;

        // Create a new view with our view name (the other fields are unimportant for this example)
        View corefView = new View(ViewNames.COREF,"COREF-annotator",textAnnotation,1.0);
        textAnnotation.addView(ViewNames.COREF,corefView);

        Random random = new Random();

        for(int i=0;i<tokens.length;i++){
            // For this example we will just randomly assigning tags.
            int randomTagIndex = random.nextInt(tags.size());
            // Add the tag to the view for the specified token
            corefView.addConstituent(new Constituent(tags.get(randomTagIndex),ViewNames.COREF,textAnnotation,i,i+1));
        }
    }
    
    public static void main(String args[]) throws IOException {
        // Create the annotator
        Annotator annotator = new ToyNER();

        // We will have our server listen on port 5757 and pass it our toy annotator
        ServerPreferences p = new ServerPreferences(10, 50);
        Server server = new Server(5758, p, annotator);

        // We have no more work to do, so we will use the executeInstance method to start and keep our Server alive
        fi.iki.elonen.util.ServerRunner.executeInstance(server);
    }
}