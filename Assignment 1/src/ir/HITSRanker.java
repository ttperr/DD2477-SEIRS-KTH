/**
 * Computes the Hubs and Authorities for an every document in a query-specific
 * link graph, induced by the base set of pages.
 *
 * @author Dmytro Kalpakchi
 */

package ir;

import java.text.DecimalFormat;
import java.util.*;
import java.io.*;


public class HITSRanker {

    /**
     * Ratio of hub score to authority score
     */
    public static int alpha = 1;

    /**
     * Ratio of authority score to hub score
     */
    public static int beta = 1;

    public static final String SAVING_DIR = "Assignment 2/";

    /**
     * Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    /**
     * Convergence criterion: hub and authority scores do not
     * change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     * The inverted index
     */
    Index index;

    /**
     * Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String, Integer> titleToId = new HashMap<String, Integer>();
    HashMap<Integer, String> idToTitle = new HashMap<Integer, String>();

    /**
     * Mapping from the internal document ids to links (i.e. nodeIDs)
     */
    HashMap<Integer, ArrayList<Integer>> links = new HashMap<Integer, ArrayList<Integer>>();

    /**
     * Sparse vector containing hub scores
     */
    HashMap<Integer, Double> hubs;

    /**
     * Sparse vector containing authority scores
     */
    HashMap<Integer, Double> authorities;


    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * <p>
     * A set of linked documents can be presented as a graph.
     * Each page is a node in graph with a distinct nodeID associated with it.
     * There is an edge between two nodes if there is a link between two pages.
     * <p>
     * Each line in the links file has the following format:
     * nodeID;outNodeID1,outNodeID2,...,outNodeIDK
     * This means that there are edges between nodeID and outNodeIDi, where i is between 1 and K.
     * <p>
     * Each line in the titles file has the following format:
     * nodeID;pageTitle
     * <p>
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the same
     * as docIDs used by search engine's Indexer
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages titles
     * @param index          The inverted index
     */
    public HITSRanker(String linksFilename, String titlesFilename, Index index) {
        this.index = index;
        readDocs(linksFilename, titlesFilename);
    }


    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path.
     * For example, given the path "davisWiki/hello.f",
     * the function will return "hello.f".
     *
     * @param path The file path
     * @return The file name.
     */
    private String getFileName(String path) {
        String result = "";
        StringTokenizer tok = new StringTokenizer(path, "\\/");
        while (tok.hasMoreTokens()) {
            result = tok.nextToken();
        }
        return result;
    }


    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages titles
     */
    void readDocs(String linksFilename, String titlesFilename) {
        //
        // YOUR CODE HERE
        //
        try {
            BufferedReader br = new BufferedReader(new FileReader(titlesFilename));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                int docId = Integer.parseInt(parts[0]);
                titleToId.put(parts[1], docId);
                idToTitle.put(docId, parts[1]);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(linksFilename));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                int docId = Integer.parseInt(parts[0]);
                ArrayList<Integer> outNodes = new ArrayList<>();
                if (parts.length > 1) {
                    for (String outNode : parts[1].split(",")) {
                        outNodes.add(Integer.parseInt(outNode));
                    }
                }
                links.put(docId, outNodes);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param titles The titles of the documents in the root set
     */
    private void iterate(String[] titles) {
        //
        // YOUR CODE HERE
        //

        // Computing the base set
        String[] baseSet = new String[0];
        for (String title : titles) {
            baseSet = Arrays.copyOf(baseSet, baseSet.length + 1);
            baseSet[baseSet.length - 1] = title;
        }
        // Adding the document that are linked to the root set
        for (String title : titles) {
            int docId = titleToId.get(title);
            for (int link : links.get(docId)) {
                if (!Arrays.asList(baseSet).contains(idToTitle.get(link))) {
                    baseSet = Arrays.copyOf(baseSet, baseSet.length + 1);
                    baseSet[baseSet.length - 1] = idToTitle.get(link);
                }
            }
        }
        // Adding the document that are linked from the root set
        for (String title : titles) {
            int docId = titleToId.get(title);
            for (Map.Entry<Integer, ArrayList<Integer>> entry : links.entrySet()) {
                if (entry.getValue().contains(docId)) {
                    if (!Arrays.asList(baseSet).contains(idToTitle.get(entry.getKey()))) {
                        baseSet = Arrays.copyOf(baseSet, baseSet.length + 1);
                        baseSet[baseSet.length - 1] = idToTitle.get(entry.getKey());
                    }
                }
            }
        }

        hubs = new HashMap<>();
        authorities = new HashMap<>();

        for (String title : baseSet) {
            hubs.put(titleToId.get(title), 1.0);
            authorities.put(titleToId.get(title), 1.0);
        }

        for (int i = 0; i < MAX_NUMBER_OF_STEPS; i++) {
            HashMap<Integer, Double> newHubs = new HashMap<>();
            HashMap<Integer, Double> newAuthorities = new HashMap<>();
            double hubsNorm = 0;
            double authoritiesNorm = 0;
            boolean converged = true;

            for (String title : baseSet) {
                int docId = titleToId.get(title);
                double hub = 0;
                double authority = 0;

                for (String link : baseSet) {
                    int linkId = titleToId.get(link);
                    if (links.get(linkId).contains(docId)) {
                        authority += hubs.get(linkId);
                    }
                    if (links.get(docId).contains(linkId)) {
                        hub += authorities.get(linkId);
                    }
                }

                newHubs.put(docId, hub);
                newAuthorities.put(docId, authority);
                hubsNorm += hub * hub;
                authoritiesNorm += authority * authority;
            }

            authoritiesNorm = Math.sqrt(authoritiesNorm);
            hubsNorm = Math.sqrt(hubsNorm);

            for (Integer docId : newAuthorities.keySet()) {
                double newAuthority = 0;
                if (authoritiesNorm != 0) {
                    newAuthority = newAuthorities.get(docId) / authoritiesNorm;
                }
                double newHub = 0;
                if (hubsNorm != 0) {
                    newHub = newHubs.get(docId) / hubsNorm;
                }
                if (converged && Math.abs(authorities.get(docId) - newAuthority) > EPSILON) {
                    converged = false;
                }
                if (converged && Math.abs(hubs.get(docId) - newHub) > EPSILON) {
                    converged = false;
                }
                authorities.put(docId, newAuthority);
                hubs.put(docId, newHub);
            }
            if (converged) {
                break;
            }
        }
    }


    /**
     * Rank the documents in the subgraph induced by the documents present
     * in the postings list `post`.
     *
     * @param post The list of postings fulfilling a certain information need
     * @return A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList post) {
        //
        // YOUR CODE HERE
        //
        String[] titles = new String[post.size()];
        for (int i = 0; i < post.size(); i++) {
            titles[i] = getFileName(index.docNames.get(post.get(i).docID));
        }
        iterate(titles);
        HashMap<Integer, Double> scores = new HashMap<>();
        for (int i = 0; i < post.size(); i++) {
            scores.put(post.get(i).docID, alpha * hubs.get(titleToId.get(getFileName(index.docNames.get(post.get(i).docID)))) + beta * authorities.get(titleToId.get(getFileName(index.docNames.get(post.get(i).docID)))));
        }
        scores = sortHashMapByValue(scores);
        PostingsList ranked = new PostingsList();
        for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
            ranked.add(new PostingsEntry(entry.getKey(), 0, entry.getValue()));
        }
        return ranked;
    }


    /**
     * Sort a hash map by values in the descending order
     *
     * @param map A hash map to sorted
     * @return A hash map sorted by values
     */
    private HashMap<Integer, Double> sortHashMapByValue(HashMap<Integer, Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(map.entrySet());

            list.sort((o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

            HashMap<Integer, Double> res = new LinkedHashMap<Integer, Double>();
            for (Map.Entry<Integer, Double> el : list) {
                res.put(el.getKey(), el.getValue());
            }
            return res;
        }
    }


    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param map   A hash map
     * @param fname The filename
     * @param k     A number of entries to write
     */
    void writeToFile(HashMap<Integer, Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));

            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer, Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k) break;
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Rank all the documents in the links file. Produces two files:
     * hubs_top_30.txt with documents containing top 30 hub scores
     * authorities_top_30.txt with documents containing top 30 authority scores
     */
    void rank() {
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer, Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer, Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, SAVING_DIR + "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, SAVING_DIR + "authorities_top_30.txt", 30);
    }


    /* --------------------------------------------- */


    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Please give the names of the link and title files");
        } else {
            HITSRanker hr = new HITSRanker(args[0], args[1], null);
            hr.rank();
        }
    }
} 