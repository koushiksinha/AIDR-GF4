package qa.qcri.aidr.predict.featureextraction;

import java.util.Arrays;
import java.util.HashSet;

import qa.qcri.aidr.predict.common.*;
import qa.qcri.aidr.predict.data.*;

/**
 * FeatureExtractor consumes DocumentSet objects from a Redis queue, performs
 * feature extraction and pushes the DocumentSet to another queue for futher
 * processing.
 * 
 * @author jrogstadius
 * 
 */
public class FeatureExtractor extends PipelineProcess {

    protected void processItem(Document doc) {
        if (doc instanceof Tweet)
            processTweet((Tweet) doc);
        else
            throw new RuntimeException("Unknown doctype");
    }

    void processTweet(Tweet tweet) {
        WordSet wordSet = new WordSet();
        String text = tweet.getText();
        wordSet.addAll(getWordsInStringWithBigrams(text, false));
        tweet.addFeatureSet(wordSet);
    }

    static String[] getWordsInStringWithBigrams(String inputText,
            boolean useStemming) {
        // remove URLs, rt @username, and a bunch of special characters
        String text = inputText;
        text = text.toLowerCase();
        String regexp = "(^|\\s)rt\\s|@\\S+|http\\S+|www\\.\\S+|[-.,;:_+?&='\"*~¨^´`<>\\[\\]{}()\\\\/|%€¤$£@!§½…]"; //  
        text = text.replaceAll(regexp, "");
        String[] words = text.split("\\s+");

        // Stem words
        if (useStemming) {
            for (int i = 0; i < words.length; i++)
                words[i] = naiveStemming(words[i]);
        }

        // Make bigrams
        HashSet<String> bigrams = new HashSet<String>();
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i];
            if (isStopword(w1))
                continue;
            String w2 = "";
            int j = i + 1;
            while (j < words.length && isStopword(w2 = words[j]))
                j++;

            // Perform stopword removal
            if (!isStopword(w2))
                bigrams.add(w1 + "_" + w2);
        }
        bigrams.addAll(Arrays.asList(words));

        if (bigrams.isEmpty())
            return new String[0];
        else
            return bigrams.toArray(new String[bigrams.size()]);
    }

    public static String naiveStemming(String str) {
        if (str.length() < 4 || str.startsWith("#"))
            return str;
        String before = str;
        while ((str = str.replaceAll("(es|ed|s|ing|ly|n)$", "")) != before)
            before = str;
        return str;
    }

    static boolean isStopword(String word) {
        return false; // TODO: Implement stopword handling
    }
}
