/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import pagerank.PageRank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /**
     * The index to be searched by this Searcher.
     */
    Index index;

    /**
     * The k-gram index to be searched by this Searcher
     */
    KGramIndex kgIndex;

    /**
     * PageRank object
     */
    PageRank pageRank;

    HITSRanker hitsRanker;

    static final HashMap<Integer, Double> euclideanLengths = new HashMap<>();

    final double TFIDF_WEIGHT = 1;
    final double PR_WEIGHT = 750;

    /**
     * Constructor
     */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
        // pageRank = new PageRank("../../Assignment 2/src/pagerank/linksDavis.txt");
        index.readEuclideanLengths(euclideanLengths);
        hitsRanker = new HITSRanker("../../Assignment 2/src/pagerank/linksDavis.txt", "../../Assignment 2/src/pagerank/davisTitles.txt", index);
        System.err.println("Ready to receive queries!");
    }

    /**
     * Searches the index for postings matching the query.
     *
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        List<KGramPostingsEntry> postings = null;
        for (Query.QueryTerm queryTerm : query.queryterm) {
            if (queryTerm.term.length() == kgIndex.getK()) {
                if (postings == null) {
                    postings = kgIndex.getPostings(queryTerm.term);
                } else {
                    postings = kgIndex.intersect(postings, kgIndex.getPostings(queryTerm.term));
                }
            }
        }
        if (postings != null) {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
        boolean wildCard = false;
        for (Query.QueryTerm queryTerm : query.queryterm) {
            if (queryTerm.term.contains("*")) {
                wildCard = true;
                break;
            }
        }
        if (queryType == QueryType.INTERSECTION_QUERY) {
            return wildCard ? intersectionWildCardQuery(query) : intersectionQuery(query);
        } else if (queryType == QueryType.PHRASE_QUERY) {
            return wildCard ? phraseWildCardQuery(query) : phraseQuery(query);
        } else if (queryType == QueryType.RANKED_QUERY) {
            query = wildCard ? wildCardQuery(query) : query;
            if (rankingType == RankingType.TF_IDF) {
                return rankedQueryTFIDF(query, normType);
            } else if (rankingType == RankingType.PAGERANK) {
                return rankedQueryPageRank(query);
            } else if (rankingType == RankingType.COMBINATION) {
                return rankedQueryCombination(query, normType);
            } else if (rankingType == RankingType.HITS) {
                return rankedQueryHITS(query);
            }
        }
        return null;
    }

    private PostingsList intersectionQuery(Query query) {
        ArrayList<PostingsList> postingsLists = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            postingsLists.add(index.getPostings(query.queryterm.get(i).term));
        }
        return intersectQueries(postingsLists);
    }

    private PostingsList intersectionWildCardQuery(Query query) {
        ArrayList<PostingsList> postingsLists = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            HashSet<String> words = wildCardSearch(query.queryterm.get(i).term);
            PostingsList postingsList = new PostingsList();
            for (String word : words) {
                PostingsList postings = index.getPostings(word);
                if (postings != null) {
                    postingsList = merge(postingsList, postings);
                }
            }
            postingsLists.add(postingsList);
        }
        return intersectQueries(postingsLists);
    }

    private PostingsList intersectQueries(ArrayList<PostingsList> postingsLists) {
        if (postingsLists.isEmpty()) {
            return new PostingsList();
        }
        PostingsList result = postingsLists.getFirst();
        for (int i = 1; i < postingsLists.size(); i++) {
            if (result != null && postingsLists.get(i) != null) {
                result = intersect(result, postingsLists.get(i));
            }
        }
        return result;
    }

    private PostingsList intersect(PostingsList p1, PostingsList p2) {
        PostingsList result = new PostingsList();
        int i = 0;
        int j = 0;
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i).docID == p2.get(j).docID) {
                if (result.size() == 0 || result.get(result.size() - 1).docID != p1.get(i).docID) {
                    PostingsEntry entry = new PostingsEntry(p1.get(i).docID, p1.get(i).offsets, p1.get(i).score + p2.get(j).score);
                    entry.addOffsets(p2.get(j).offsets);
                    result.add(entry);
                } else {
                    result.get(result.size() - 1).score = result.get(result.size() - 1).score + p2.get(j).score;
                    result.get(result.size() - 1).addOffsets(p2.get(j).offsets);
                }
                i++;
                j++;
            } else if (p1.get(i).docID < p2.get(j).docID) {
                i++;
            } else {
                j++;
            }
        }
        return result;
    }

    private PostingsList merge(PostingsList p1, PostingsList p2) {
        PostingsList result = new PostingsList();
        int i = 0;
        int j = 0;
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i).docID == p2.get(j).docID) {
                if (result.size() == 0 || result.get(result.size() - 1).docID != p1.get(i).docID) {
                    PostingsEntry newEntry = new PostingsEntry(p1.get(i).docID, p1.get(i).offsets, p1.get(i).score + p2.get(j).score);
                    newEntry.addOffsets(p2.get(j).offsets);
                    result.add(newEntry);
                } else {
                    result.get(result.size() - 1).score += p1.get(j).score + p2.get(i).score;
                    result.get(result.size() - 1).addOffsets(p1.get(i).offsets);
                    result.get(result.size() - 1).addOffsets(p2.get(j).offsets);
                }
                i++;
                j++;
            } else if (p1.get(i).docID < p2.get(j).docID) {
                result.add(p1.get(i));
                i++;
            } else {
                result.add(p2.get(j));
                j++;
            }
        }
        while (i < p1.size()) {
            result.add(p1.get(i));
            i++;
        }
        while (j < p2.size()) {
            result.add(p2.get(j));
            j++;
        }
        return result;
    }

    private PostingsList phraseQuery(Query query) {
        ArrayList<PostingsList> postingsLists = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            postingsLists.add(index.getPostings(query.queryterm.get(i).term));
        }
        return intersectPhraseQueries(postingsLists);
    }

    private PostingsList phraseWildCardQuery(Query query) {
        ArrayList<PostingsList> postingsLists = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            HashSet<String> words = wildCardSearch(query.queryterm.get(i).term);
            PostingsList postingsList = new PostingsList();
            for (String word : words) {
                PostingsList postings = index.getPostings(word);
                if (postings != null) {
                    postingsList = merge(postingsList, postings);
                }
            }
            postingsLists.add(postingsList);
        }
        return intersectPhraseQueries(postingsLists);
    }

    private PostingsList intersectPhraseQueries(ArrayList<PostingsList> postingsLists) {
        if (postingsLists.isEmpty()) {
            return new PostingsList();
        }
        PostingsList result = postingsLists.getFirst();
        for (int i = 1; i < postingsLists.size(); i++) {
            result = positionalIntersection(result, postingsLists.get(i), 1);
        }
        return result;
    }

    private PostingsList positionalIntersection(PostingsList p1, PostingsList p2, int k) {
        PostingsList result = new PostingsList();
        int i = 0;
        int j = 0;
        if (p1 == null || p2 == null) {
            return result;
        }
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i).docID == p2.get(j).docID) {
                ArrayList<Integer> p1Offsets = p1.get(i).offsets;
                ArrayList<Integer> p2Offsets = p2.get(j).offsets;
                int m = 0;
                int n = 0;
                while (m < p1Offsets.size() && n < p2Offsets.size()) {
                    if (p1Offsets.get(m) + k == p2Offsets.get(n)) {
                        if (result.size() == 0 || result.get(result.size() - 1).docID != p1.get(i).docID) {
                            result.add(p1.get(i).docID, p2Offsets.get(n), p1.get(i).score + p2.get(j).score);
                        } else {
                            result.get(result.size() - 1).score++;
                            result.get(result.size() - 1).offsets.add(p2Offsets.get(n));
                        }
                        m++;
                        n++;
                    } else if (p1Offsets.get(m) + k < p2Offsets.get(n)) {
                        m++;
                    } else {
                        n++;
                    }
                }
                i++;
                j++;
            } else if (p1.get(i).docID < p2.get(j).docID) {
                i++;
            } else {
                j++;
            }
        }
        return result;
    }

    private PostingsList rankedQueryTFIDF(Query query, NormalizationType normType) {
        int N = index.docLengths.size();
        double[] scores = new double[N];
        PostingsList result = new PostingsList();
        for (int i = 0; i < query.queryterm.size(); i++) {
            computeTFIDF(query, i, N, scores);
        }
        for (int i = 0; i < N; i++) {
            if (scores[i] > 0) {
                if (normType == NormalizationType.NUMBER_OF_WORDS) {
                    scores[i] /= index.docLengths.get(i);
                } else if (normType == NormalizationType.EUCLIDEAN) {
                    scores[i] /= euclideanLengths.get(i);
                }
                result.add(i, 0, scores[i]);
            }
        }
        result.sort();
        return result;
    }

    private void computeTFIDF(Query query, int i, int N, double[] scores) {
        PostingsList postingsList = index.getPostings(query.queryterm.get(i).term);
        if (postingsList != null) {
            double idf = Math.log((double) N / postingsList.size());
            for (int j = 0; j < postingsList.size(); j++) {
                int docID = postingsList.get(j).docID;
                double tf = postingsList.get(j).score;
                scores[docID] += tf * idf * query.queryterm.get(i).weight;
            }
        }
    }

    private PostingsList rankedQueryPageRank(Query query) {
        int N = index.docLengths.size();
        double[] scores = new double[N];
        PostingsList result = new PostingsList();
        for (int i = 0; i < query.queryterm.size(); i++) {
            computePageRank(query, i, scores);
        }
        for (int i = 0; i < N; i++) {
            if (scores[i] > 0) {
                result.add(i, 0, scores[i]);
            }
        }
        result.sort();
        return result;
    }

    private void computePageRank(Query query, int i, double[] scores) {
        PostingsList postingsList = index.getPostings(query.queryterm.get(i).term);
        if (postingsList != null) {
            for (int j = 0; j < postingsList.size(); j++) {
                int docID = postingsList.get(j).docID;
                String docName = index.docNames.get(docID);
                scores[docID] += pageRank.getScore(docName);
            }
        }
    }

    private PostingsList rankedQueryCombination(Query query, NormalizationType normType) {
        PostingsList result = new PostingsList();
        double[] scores = new double[index.docLengths.size()];
        for (int i = 0; i < query.queryterm.size(); i++) {
            computeTFIDF(query, i, index.docLengths.size(), scores);
        }
        ArrayList<PostingsEntry> tfidf = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0) {
                if (normType == NormalizationType.NUMBER_OF_WORDS) {
                    tfidf.add(new PostingsEntry(i, 0, scores[i] / index.docLengths.get(i)));
                } else if (normType == NormalizationType.EUCLIDEAN) {
                    tfidf.add(new PostingsEntry(i, 0, scores[i] / euclideanLengths.get(i)));
                } else {
                    tfidf.add(new PostingsEntry(i, 0, scores[i]));
                }
            }
        }
        for (PostingsEntry postingsEntry : tfidf) {
            int docID = postingsEntry.docID;
            String docName = index.docNames.get(docID);
            double score = TFIDF_WEIGHT * postingsEntry.score + PR_WEIGHT * pageRank.getScore(docName);
            result.add(docID, 0, score);
        }
        result.sort();
        return result;
    }

    private PostingsList rankedQueryHITS(Query query) {
        PostingsList result = new PostingsList();
        HashMap<Integer, Double> scores = new HashMap<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            computeHITS(query, i, scores);
        }
        for (int docID : scores.keySet()) {
            result.add(docID, 0, scores.get(docID));
        }
        result.sort();
        return result;
    }

    private void computeHITS(Query query, int i, HashMap<Integer, Double> scores) {
        PostingsList postingsList = index.getPostings(query.queryterm.get(i).term);
        if (postingsList != null) {
            postingsList = hitsRanker.rank(postingsList);
            for (int j = 0; j < postingsList.size(); j++) {
                int docID = postingsList.get(j).docID;
                double score = postingsList.get(j).score;
                if (scores.containsKey(docID)) {
                    scores.put(docID, scores.get(docID) + score);
                } else {
                    scores.put(docID, score);
                }
            }
        }
    }

    private HashSet<String> wildCardSearch(String token) {
        HashSet<String> result = new HashSet<>();
        if (!token.contains("*")) {
            result.add(token);
        } else if (token.startsWith("*")) {
            token = token + "$";
            String kgram = token.substring(1, 3);
            List<KGramPostingsEntry> postings = kgIndex.getPostings(kgram);
            String word;
            for (KGramPostingsEntry entry : postings) {
                word = kgIndex.getTermByID(entry.tokenID);
                if (word.endsWith(token.substring(1, token.length() - 1))) {
                    result.add(word);
                }
            }
        } else if (token.endsWith("*")) {
            token = "^" + token;
            String kgram = token.substring(token.length() - 3, token.length() - 1);
            List<KGramPostingsEntry> postings = kgIndex.getPostings(kgram);
            String word;
            for (KGramPostingsEntry entry : postings) {
                word = kgIndex.getTermByID(entry.tokenID);
                if (word.startsWith(token.substring(1, token.length() - 1))) {
                    result.add(word);
                }
            }
        } else {
            token = "^" + token + "$";
            int index = token.indexOf("*");
            String kgram1 = token.substring(index - 2, index);
            String kgram2 = token.substring(index + 1, index + 3);
            List<KGramPostingsEntry> postings1 = kgIndex.getPostings(kgram1);
            List<KGramPostingsEntry> postings2 = kgIndex.getPostings(kgram2);
            List<KGramPostingsEntry> postings = kgIndex.intersect(postings1, postings2);
            String word;
            for (KGramPostingsEntry entry : postings) {
                word = kgIndex.getTermByID(entry.tokenID);
                if (word.startsWith(token.substring(1, index)) && word.endsWith(token.substring(index + 1, token.length() - 1))) {
                    result.add(word);
                }
            }
        }
        return result;
    }

    private Query wildCardQuery(Query query) {
        Query newQuery = new Query();
        for (Query.QueryTerm queryTerm : query.queryterm) {
            if (queryTerm.term.contains("*")) {
                HashSet<String> words = wildCardSearch(queryTerm.term);
                for (String word : words) {
                    newQuery.queryterm.add(new Query.QueryTerm(word, queryTerm.weight));
                }
            } else {
                newQuery.queryterm.add(queryTerm);
            }
        }
        return newQuery;
    }
}
