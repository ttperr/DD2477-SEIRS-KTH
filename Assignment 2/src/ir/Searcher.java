/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

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
     * Constructor
     */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
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
        }/* else if (queryType == QueryType.RANKED_QUERY) {
            return rankedQuery(query, rankingType, normType);
        } */ else {
            return null;
        }
    }

    private PostingsList intersectionQuery(Query query) {
        ArrayList<PostingsList> postingsLists = new ArrayList<>();
        for (int i = 0; i < query.queryterm.size(); i++) {
            postingsLists.add(index.getPostings(query.queryterm.get(i).term));
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

}
