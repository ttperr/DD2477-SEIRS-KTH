/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import pagerank.PageRank;

import java.util.ArrayList;

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

    final double TFIDF_WEIGHT = 1;
    final double PR_WEIGHT = 750;

    /**
     * Constructor
     */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
        pageRank = new PageRank("../../Assignment 2/src/pagerank/linksDavis.txt");
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
        if (queryType == QueryType.INTERSECTION_QUERY) {
            return intersectionQuery(query);
        } else if (queryType == QueryType.PHRASE_QUERY) {
            return phraseQuery(query);
        } else if (queryType == QueryType.RANKED_QUERY) {
            if (rankingType == RankingType.TF_IDF) {
                return rankedQueryTFIDF(query, normType);
            } else if (rankingType == RankingType.PAGERANK) {
                return rankedQueryPageRank(query);
            } else if (rankingType == RankingType.COMBINATION) {
                return rankedQueryCombination(query, normType);
            }
        }
        return null;
    }

    private PostingsList intersectionQuery(Query query) {
        ArrayList<PostingsList> postingsLists = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            postingsLists.add(index.getPostings(query.queryterm.get(i).term));
        }
        if (postingsLists.isEmpty()) {
            return new PostingsList();
        }
        PostingsList result = postingsLists.getFirst();
        for (int i = 1; i < postingsLists.size(); i++) {
            result = intersect(result, postingsLists.get(i));
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
                    result.add(p1.get(i).docID, p1.get(i).offsets.getFirst(), p1.get(i).score + p2.get(j).score);
                } else {
                    result.get(result.size() - 1).score++;
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

    private PostingsList phraseQuery(Query query) {
        ArrayList<PostingsList> postingsLists = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            postingsLists.add(index.getPostings(query.queryterm.get(i).term));
        }
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
                    scores[i] /= getEuclideanLength(i);
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
                int tf = postingsList.get(j).offsets.size();
                scores[docID] += tf * idf;
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
                    tfidf.add(new PostingsEntry(i, 0, scores[i] / getEuclideanLength(i)));
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

    private double getEuclideanLength(int docID) {
        return index.getEuclideanLength(docID);
    }
}
