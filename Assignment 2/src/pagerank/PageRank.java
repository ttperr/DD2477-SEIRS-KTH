package pagerank;

import java.util.*;
import java.io.*;

public class PageRank {

    /**
     * Maximal number of documents. We're assuming here that we
     * don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    public static final String TITLES_TXT = "/Users/ttperr/Documents/Git/Pro/3A/DD2477-SEIRS-KTH/Assignment 2/src/pagerank/davisTitles.txt";
    public static final String TOP_30_TEST_TXT = "/Users/ttperr/Documents/Git/Pro/3A/DD2477-SEIRS-KTH/Assignment 2/src/pagerank/davis_top_30_test.txt";
    public static final String DATA_WIKI = "../../data/davisWiki/";

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

    /**
     * The score of every document
     */
    static double[] scores;

    /**
     * The docID from the real name of the document
     */
    HashMap<String, Integer> docNumberMap = new HashMap<>();

    String[] docTitle = new String[MAX_NUMBER_OF_DOCS];

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
                Integer from_doc = docNumber.get(title);
                //  Have we seen this document before?
                if (from_doc == null) {
                    // This is a previously unseen doc, so add it to the table.
                    from_doc = fileIndex++;
                    docNumber.put(title, from_doc);
                    docName[from_doc] = title;
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
                    // a link from from_doc to otherDoc.
                    if (link.get(from_doc) == null) {
                        link.put(from_doc, new HashMap<Integer, Boolean>());
                    }
                    if (link.get(from_doc).get(otherDoc) == null) {
                        link.get(from_doc).put(otherDoc, true);
                        out[from_doc]++;
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
        System.err.println("Started PageRank Iteration...");
        double startTime = System.currentTimeMillis();
        scores = new double[numberOfDocs];
        double[] newScores = new double[numberOfDocs];
        newScores[0] = 1.0;
        double[] inverseOut = new double[numberOfDocs];

        docTitle = readTitles();

        for (int k = 0; k < numberOfDocs; k++) {
            inverseOut[k] = 1.0 / out[k];
        }

        for (int i = 0; i < maxIterations; i++) {
            scores = newScores.clone();
            double normalizer = 0.0;
            for (int p = 0; p < numberOfDocs; p++) {
                newScores[p] = 0;
                for (int q = 0; q < numberOfDocs; q++) {
                    if (link.get(q) != null && link.get(q).get(p) != null) {
                        newScores[p] += scores[q] * inverseOut[q];
                    }
                }
                newScores[p] = BORED / numberOfDocs + (1 - BORED) * newScores[p];
                normalizer += newScores[p];
            }
            boolean isConverged = convergenceCriteria(scores, newScores);
            System.out.println("Iteration " + i);
            if (isConverged) {
                // TODO: Done at the end because is more closer to the given result & more efficient
                for (int p = 0; p < numberOfDocs; p++) {
                    newScores[p] /= normalizer;
                    // round to 5 decimal places
                    scores[p] = Math.round(newScores[p] * 100000.0) / 100000.0;
                    docNumberMap.put(docTitle[p], p);
                }
                break;
            }
        }
        printPageRank(newScores, true);
        double estimatedTime = (System.currentTimeMillis() - startTime) / 1000.0;
        System.err.println("PageRank done! Time: " + estimatedTime + "s");
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
            BufferedWriter writer = new BufferedWriter(new FileWriter(TOP_30_TEST_TXT));
            for (int i = 0; i < resultList.size(); i++) {
                writer.write(docName[resultList.get(i)] + ": " + scores[resultList.get(i)] + ": " +
                        DATA_WIKI + docTitle[resultList.get(i)] + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] readTitles() {
        String[] docTitle = new String[MAX_NUMBER_OF_DOCS];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(TITLES_TXT));
            String line;
            int docID;
            while ((line = reader.readLine()) != null) {
                docID = Integer.parseInt(line.split(";")[0]);
                int indexOfDocID = -1;
                for (int i = 0; i < scores.length; i++) {
                    if (Integer.parseInt(docName[i]) == docID) {
                        indexOfDocID = i;
                        break;
                    }
                }
                if (indexOfDocID != -1) {
                    docTitle[indexOfDocID] = line.split(";")[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return docTitle;
    }

    public double getScore(int docID) {
        return scores[docID];
    }

    public double getScore(String docName) {
        String fileName = docName.lastIndexOf("/") > 0 ? docName.substring(docName.lastIndexOf("/") + 1) : docName;
        return scores[docNumberMap.get(fileName)];
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