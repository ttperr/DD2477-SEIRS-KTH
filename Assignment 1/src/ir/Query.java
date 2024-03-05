/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.*;
import java.nio.charset.*;
import java.io.*;


/**
 * A class for representing a query as a list of words, each of which has
 * an associated weight.
 */
public class Query {

    /**
     * Help class to represent one query term, with its associated weight.
     */
    static class QueryTerm {
        String term;
        double weight;

        QueryTerm(String t, double w) {
            term = t;
            weight = w;
        }
    }

    /**
     * Representation of the query as a list of terms with associated weights.
     * In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**
     * Relevance feedback constant alpha (= weight of original query terms).
     * Should be between 0 and 1.
     * (only used in assignment 3).
     */
    double alpha = 0.2;

    /**
     * Relevance feedback constant beta (= weight of query terms obtained by
     * feedback from the user).
     * (only used in assignment 3).
     */
    double beta = 1 - alpha;


    /**
     * Creates a new empty Query
     */
    public Query() {
    }


    /**
     * Creates a new Query from a string of words
     */
    public Query(String queryString) {
        StringTokenizer tok = new StringTokenizer(queryString);
        while (tok.hasMoreTokens()) {
            queryterm.add(new QueryTerm(tok.nextToken(), 1.0));
        }
    }


    /**
     * Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }


    /**
     * Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for (QueryTerm t : queryterm) {
            len += t.weight;
        }
        return len;
    }


    /**
     * Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for (QueryTerm t : queryterm) {
            queryCopy.queryterm.add(new QueryTerm(t.term, t.weight));
        }
        return queryCopy;
    }


    /**
     * Expands the Query using Relevance Feedback
     *
     * @param results       The results of the previous query.
     * @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     * @param engine        The search engine object
     */
    public void relevanceFeedback(PostingsList results, boolean[] docIsRelevant, Engine engine) {
        //
        //  YOUR CODE HERE
        //

        // Rocchio algorithm
        double numRelevant = 0;
        // Calculate the number of relevant documents
        for (boolean b : docIsRelevant) {
            if (b) {
                numRelevant++;
            }
        }
        double betaWeight = beta / numRelevant;

        HashMap<String, Double> newQuery = new HashMap<>();
        for (QueryTerm queryTerm : queryterm) {
            newQuery.put(queryTerm.term, alpha * queryTerm.weight);
        }
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                PostingsEntry entry = results.get(i);
                String docName = engine.index.docNames.get(entry.docID);
                List<String> wordsList = getWords(docName);
                for (String word : wordsList) {
                    newQuery.merge(word, betaWeight, Double::sum);
                }
            }
        }

        queryterm = new ArrayList<>();
        for (Map.Entry<String, Double> entry : newQuery.entrySet()) {
            queryterm.add(new QueryTerm(entry.getKey(), entry.getValue()));
        }
    }

    public ArrayList<String> getWords(String docName) {
        ArrayList<String> words = new ArrayList<>();
        try {
            Reader reader = new InputStreamReader(new FileInputStream(docName), StandardCharsets.UTF_8);
            Tokenizer tokenizer = new Tokenizer(reader, true, false, true, "patterns.txt");
            while (tokenizer.hasMoreTokens()) {
                words.add(tokenizer.nextToken());
            }
        } catch (IOException e) {
            System.err.println("Warning: IOException during indexing.");
        }
        return words;
    }
}


