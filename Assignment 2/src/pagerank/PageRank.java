package pagerank;

import java.util.*;
import java.io.*;

public class PageRank {

    /**
     * Maximal number of documents. We're assuming here that we
     * don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    public static final String DAVIS_TITLES_TXT = "/Users/ttperr/Documents/Git/Pro/3A/DD2477-SEIRS-KTH/Assignment 2/src/pagerank/davisTitles.txt";
    public static final String TOP_30_TEST_TXT = "/Users/ttperr/Documents/Git/Pro/3A/DD2477-SEIRS-KTH/Assignment 2/src/pagerank/davis_top_30_test.txt";
    public static final String TOP_30 = "/Users/ttperr/Documents/Git/Pro/3A/DD2477-SEIRS-KTH/Assignment 2/src/pagerank/davis_top_30.txt";
    public static final String DATA_WIKI = "../../data/davisWiki/";

    public static final String SV_WIKI = "/Users/ttperr/Documents/Git/Pro/3A/DD2477-SEIRS-KTH/Assignment 2/src/pagerank/linksSvwiki.txt";
    public static final String SV_WIKI_TITLES = "/Users/ttperr/Documents/Git/Pro/3A/DD2477-SEIRS-KTH/Assignment 2/src/pagerank/svwikiTitles.txt";

    /**
     * Mapping from document names to document numbers.
     */
    HashMap<String, Integer> docNumber = new HashMap<>();

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
    HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<>();

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

    public PageRank(String filename, int numberOfRuns) {
        int noOfDocs = readDocs(filename);
        docTitle = readTitles(DAVIS_TITLES_TXT);

        HashMap<Integer, Double> top30scores = readScores(TOP_30);

        System.out.println("MC End Point Random Start");
        double startTime = System.currentTimeMillis();
        double[] mcScores = mcEndPointRandomStart(noOfDocs, numberOfRuns);
        printPageRank(mcScores, "src/pagerank/mcEndPointRandomStart.txt", true, true);
        double endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) / 1000.0 + "s");

        HashMap<Integer, Double> top30scoresMC = readScores("src/pagerank/mcEndPointRandomStart.txt");
        double loss = computeLoss(top30scores, top30scoresMC);
        System.out.println("Loss: " + loss);

        System.out.println("MC End Point Cyclic Start");
        startTime = System.currentTimeMillis();
        mcScores = mcEndPointCyclicStart(noOfDocs, numberOfRuns);
        printPageRank(mcScores, "src/pagerank/mcEndPointCyclicStart.txt", true, true);
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) / 1000.0 + "s");

        top30scoresMC = readScores("src/pagerank/mcEndPointCyclicStart.txt");
        loss = computeLoss(top30scores, top30scoresMC);
        System.out.println("Loss: " + loss);

        System.out.println("MC Complete Path Stop");
        startTime = System.currentTimeMillis();
        mcScores = mcCompletePathStop(noOfDocs, numberOfRuns);
        printPageRank(mcScores, "src/pagerank/mcCompletePathStop.txt", true, true);
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) / 1000.0 + "s");

        top30scoresMC = readScores("src/pagerank/mcCompletePathStop.txt");
        loss = computeLoss(top30scores, top30scoresMC);
        System.out.println("Loss: " + loss);

        System.out.println("MC Complete Path Stop Random Start");
        startTime = System.currentTimeMillis();
        mcScores = mcCompletePathStopRandomStart(noOfDocs, numberOfRuns);
        printPageRank(mcScores, "src/pagerank/mcCompletePathStopRandomStart.txt", true, true);
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) / 1000.0 + "s");

        top30scoresMC = readScores("src/pagerank/mcCompletePathStopRandomStart.txt");
        loss = computeLoss(top30scores, top30scoresMC);
        System.out.println("Loss: " + loss);
    }

    public PageRank(String filename, boolean isWiki) {
        int noOfDocs = readDocs(filename);
        scores = mcCompletePathStop(noOfDocs, noOfDocs * 20);
        printPageRank(scores, "src/pagerank/test_mc_wiki.text", true, false);
        writeTitlesOnResult("src/pagerank/test_mc_wiki.text", SV_WIKI_TITLES);
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
                        link.put(from_doc, new HashMap<>());
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

        docTitle = readTitles(DAVIS_TITLES_TXT);

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
        printPageRank(newScores, TOP_30_TEST_TXT, true, true);
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

    void printPageRank(double[] scores, String filename, boolean top30, boolean printTitles) {
        int numberOfDocs = scores.length;
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
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            for (Integer integer : resultList) {
                if (printTitles) {
                    writer.write(docName[integer] + ": " + scores[integer] + ": " +
                            DATA_WIKI + docTitle[integer] + "\n");
                } else {
                    writer.write(integer + ": " + scores[integer] + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String[] readTitles(String filename) {
        String[] docTitle = new String[MAX_NUMBER_OF_DOCS];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            int docID;
            while ((line = reader.readLine()) != null) {
                docID = Integer.parseInt(line.split(";")[0]);
                int indexOfDocID = -1;
                for (int i = 0; i < docName.length; i++) {
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

    private void writeTitlesOnResult(String result, String titles) {
        String newFile = result.substring(0, result.lastIndexOf(".")) + "_withTitles.txt";
        try {
            BufferedReader resultReader = new BufferedReader(new FileReader(result));
            BufferedWriter resultWriter = new BufferedWriter(new FileWriter(newFile));
            String line;
            boolean found;
            while ((line = resultReader.readLine()) != null) {
                String[] split = line.split(": ");
                int docID = Integer.parseInt(split[0]);
                BufferedReader titleReader = new BufferedReader(new FileReader(titles));
                String titleLine;
                found = false;
                while ((titleLine = titleReader.readLine()) != null) {
                    String[] splitTitle = titleLine.split(";");
                    if (Integer.parseInt(splitTitle[0]) == docID) {
                        resultWriter.write(split[0] + ": " + split[1] + ": " + splitTitle[1] + "\n");
                        titleReader.close();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    resultWriter.write(split[0] + ": " + split[1] + ": " + "No title found" + "\n");
                    titleReader.close();
                }
            }
            resultReader.close();
            resultWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getScore(String docName) {
        String fileName = docName.lastIndexOf("/") > 0 ? docName.substring(docName.lastIndexOf("/") + 1) : docName;
        return scores[docNumberMap.get(fileName)];
    }

    private HashMap<Integer, Double> readScores(String filename) {
        HashMap<Integer, Double> readScores = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(": ");
                readScores.put(Integer.parseInt(split[0]), Double.parseDouble(split[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return readScores;
    }

    private double[] mcEndPointRandomStart(int numberOfDocs, int numberOfRuns) {
        double[] estimatedScores = new double[numberOfDocs];
        int randomDoc;
        Set<Integer> outlinks;
        Random random = new Random();
        for (int i = 0; i < numberOfRuns; i++) {
            randomDoc = random.nextInt(numberOfDocs);
            while (Math.random() > BORED) {
                try {
                    outlinks = link.get(randomDoc).keySet();
                } catch (NullPointerException e) {
                    randomDoc = random.nextInt(numberOfDocs);
                    continue;
                }
                randomDoc = (int) outlinks.toArray()[(int) (Math.random() * outlinks.size())];
            }
            estimatedScores[randomDoc]++;
        }
        for (int i = 0; i < numberOfDocs; i++) {
            estimatedScores[i] /= numberOfRuns;
        }
        return estimatedScores;
    }

    private double[] mcEndPointCyclicStart(int numberOfDocs, int numberOfRuns) {
        int m = numberOfRuns / numberOfDocs;
        double[] estimatedScores = new double[numberOfDocs];
        Set<Integer> outlinks;
        for (int i = 0; i < numberOfDocs; i++) {
            for (int j = 0; j < m; j++) {
                int randomDoc = i;
                while (Math.random() > BORED) {
                    try {
                        outlinks = link.get(randomDoc).keySet();
                    } catch (NullPointerException e) {
                        randomDoc = (int) (Math.random() * numberOfDocs);
                        continue;
                    }
                    randomDoc = (int) outlinks.toArray()[(int) (Math.random() * outlinks.size())];
                }
                estimatedScores[randomDoc]++;
            }
        }
        for (int i = 0; i < numberOfDocs; i++) {
            estimatedScores[i] /= numberOfRuns;
        }
        return estimatedScores;
    }

    private double[] mcCompletePathStop(int numberOfDocs, int numberOfRuns) {
        int m = numberOfRuns / numberOfDocs;
        double[] estimatedScores = new double[numberOfDocs];
        int visits = 0;
        Set<Integer> outlinks;
        for (int i = 0; i < numberOfDocs; i++) {
            for (int j = 0; j < m; j++) {
                int randomDoc = i;
                while (Math.random() > BORED) {
                    estimatedScores[randomDoc]++;
                    visits++;
                    try {
                        outlinks = link.get(randomDoc).keySet();
                    } catch (NullPointerException e) {
                        break;
                    }
                    randomDoc = (int) outlinks.toArray()[(int) (Math.random() * outlinks.size())];
                }
            }
        }
        for (int i = 0; i < numberOfDocs; i++) {
            estimatedScores[i] /= visits;
        }
        return estimatedScores;
    }

    private double[] mcCompletePathStopRandomStart(int numberOfDocs, int numberOfRuns) {
        double[] estimatedScores = new double[numberOfDocs];
        int randomDoc;
        int visits = 0;
        Set<Integer> outlinks;
        Random random = new Random();
        for (int i = 0; i < numberOfRuns; i++) {
            randomDoc = random.nextInt(numberOfDocs);
            while (Math.random() > BORED) {
                estimatedScores[randomDoc]++;
                visits++;
                try {
                    outlinks = link.get(randomDoc).keySet();
                } catch (NullPointerException e) {
                    break;
                }
                randomDoc = (int) outlinks.toArray()[(int) (Math.random() * outlinks.size())];
            }
        }
        for (int i = 0; i < numberOfDocs; i++) {
            estimatedScores[i] /= visits;
        }
        return estimatedScores;
    }

    private double computeLoss(HashMap<Integer, Double> scores, HashMap<Integer, Double> top30scores) {
        double loss = 0;
        for (Integer integer : top30scores.keySet()) {
            if (scores.containsKey(integer)) {
                loss += Math.pow(scores.get(integer) - top30scores.get(integer), 2);
            }
        }
        return loss;
    }

    /* --------------------------------------------- */


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please give the name of the link file");
        } else if (args[0].equals(SV_WIKI)) {
            new PageRank(args[0], true);
        } else {
            new PageRank(args[0], 17478 * 100);
        }
    }
}