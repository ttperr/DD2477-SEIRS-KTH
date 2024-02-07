package pagerank;

import java.util.*;
import java.io.*;

public class PageRank {

    /**
     * Maximal number of documents. We're assuming here that we
     * don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     * Mapping from document names to document numbers.
     */
    HashMap<String, Integer> docNumber = new HashMap<String, Integer>();

    /**
     * Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**
     * A memory-efficient representation of the transition matrix.
     * The outlinks are represented as a HashMap, whose keys are
     * the numbers of the documents linked from.<p>
     * <p>
     * The value corresponding to key i is a HashMap whose keys are
     * all the numbers of documents j that i links to.<p>
     * <p>
     * If there are no outlinks from i, then the value corresponding
     * key i is null.
     */
    HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<Integer, HashMap<Integer, Boolean>>();

    /**
     * The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     * The probability that the surfer will be bored, stop
     * following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     * Convergence criterion: Transition probabilities do not
     * change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;


    /* --------------------------------------------- */


    public PageRank(String filename) {
        int noOfDocs = readDocs(filename);
        iterate(noOfDocs, 1000);
    }


    /* --------------------------------------------- */


    /**
     * Reads the documents and fills the data structures.
     *
     * @return the number of documents read.
     */
    int readDocs(String filename) {
        int fileIndex = 0;
        try {
            System.err.print("Reading file... ");
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
                int index = line.indexOf(";");
                String title = line.substring(0, index);
                Integer fromdoc = docNumber.get(title);
                //  Have we seen this document before?
                if (fromdoc == null) {
                    // This is a previously unseen doc, so add it to the table.
                    fromdoc = fileIndex++;
                    docNumber.put(title, fromdoc);
                    docName[fromdoc] = title;
                }
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
                while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = docNumber.get(otherTitle);
                    if (otherDoc == null) {
                        // This is a previously unseen doc, so add it to the table.
                        otherDoc = fileIndex++;
                        docNumber.put(otherTitle, otherDoc);
                        docName[otherDoc] = otherTitle;
                    }
                    // Set the probability to 0 for now, to indicate that there is
                    // a link from fromdoc to otherDoc.
                    if (link.get(fromdoc) == null) {
                        link.put(fromdoc, new HashMap<Integer, Boolean>());
                    }
                    if (link.get(fromdoc).get(otherDoc) == null) {
                        link.get(fromdoc).put(otherDoc, true);
                        out[fromdoc]++;
                    }
                }
            }
            if (fileIndex >= MAX_NUMBER_OF_DOCS) {
                System.err.print("stopped reading since documents table is full. ");
            } else {
                System.err.print("done. ");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + filename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + filename);
        }
        System.err.println("Read " + fileIndex + " number of documents");
        return fileIndex;
    }


    /* --------------------------------------------- */


    /**
     * Chooses a probability vector a, and repeatedly computes
     * aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate(int numberOfDocs, int maxIterations) {
        // YOUR CODE HERE
        System.err.println("Started Iteration...");
        double startTime = System.currentTimeMillis();
        double[] scores;
        double[] newScores = new double[numberOfDocs];
        newScores[0] = 1.0;
        double[] inverseOut = new double[numberOfDocs];
        for (int k = 0; k < numberOfDocs; k++) {
            inverseOut[k] = 1.0 / out[k];
        }


        for (int i = 0; i < maxIterations; i++) {
            scores = newScores.clone();
            for (int j = 0; j < numberOfDocs; j++) {
                newScores[j] = 0;
                for (int k = 0; k < numberOfDocs; k++) {
                    if (link.get(k) != null && link.get(k).get(j) != null) {
                        newScores[j] += scores[k] * inverseOut[k];
                    }
                }
                newScores[j] = BORED / numberOfDocs + (1 - BORED) * newScores[j];
            }
            boolean isConverged = convergenceCriteria(scores, newScores);
            System.out.println("Iteration " + i);
            if (isConverged) {
                break;
            }
            if (System.currentTimeMillis() - startTime > 180000) {
                System.err.println("Time limit reached");
                break;
            }
        }
        printPageRank(newScores, true);
        double estimatedTime = (System.currentTimeMillis() - startTime) / 1000.0;
        System.err.println("Done! Time: " + estimatedTime + "s");
    }

    private boolean convergenceCriteria(double[] tab1, double[] tab2) {
        for (int i = 0; i < tab1.length; i++) {
            if (Math.abs(tab1[i] - tab2[i]) > EPSILON) {
                return false;
            }
        }
        return true;
    }

    private void printPageRank(double[] scores, boolean top30) {
        int numberOfDocs = scores.length;
        // Insert into davis_top_30_test.txt the 30 most important pages
        List<Integer> resultList = new ArrayList<>();
        if (top30) {
            for (int i = 0; i < 30; i++) {
                double max = 0;
                int maxIndex = 0;
                for (int j = 0; j < numberOfDocs; j++) {
                    if (scores[j] > max && !resultList.contains(j)) {
                        max = scores[j];
                        maxIndex = j;
                    }
                }
                resultList.add(maxIndex);
            }
        } else {
            // Insert all into resultList
            for (int i = 0; i < numberOfDocs; i++) {
                resultList.add(i);
            }
        }
        // Write in the file
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("src/pagerank/davis_top_30_test.txt"));
            BufferedReader reader = new BufferedReader(new FileReader("src/pagerank/davisTitles.txt"));
            String line;
            int docID;
            String[] docTitle = new String[resultList.size()];
            while ((line = reader.readLine()) != null) {
                docID = Integer.parseInt(line.split(";")[0]);
                int indexOfDocID = -1;
                for (int i = 0; i < docName.length; i++) {
                    if (Integer.parseInt(docName[i]) == docID) {
                        indexOfDocID = i;
                        break;
                    }
                }
                int j = resultList.indexOf(indexOfDocID);
                if (j != -1) {
                    docTitle[j] = line.split(";")[1];
                }
            }
            for (int i = 0; i < 30; i++) {
                writer.write(docName[resultList.get(i)] + ": " + String.format("%.5f", scores[resultList.get(i)]) + ": " + docTitle[i] + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* --------------------------------------------- */


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please give the name of the link file");
        } else {
            new PageRank(args[0]);
        }
    }
}