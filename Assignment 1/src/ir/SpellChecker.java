/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.*;


public class SpellChecker {
    /**
     * The regular inverted index to be used by the spell checker
     */
    Index index;

    /**
     * K-gram index to be used by the spell checker
     */
    KGramIndex kgIndex;

    /**
     * The auxiliary class for containing the value of your ranking function for a token
     */
    class KGramStat implements Comparable {
        double score;
        String token;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
        }

        public String getToken() {
            return token;
        }

        public int compareTo(Object other) {
            if (this.score == ((KGramStat) other).score) return 0;
            return this.score < ((KGramStat) other).score ? -1 : 1;
        }

        public String toString() {
            return token + ";" + score;
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling
     * correction should pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.4;


    /**
     * The threshold for edit distance for a candidate spelling
     * correction to be accepted.
     */
    private static final int MAX_EDIT_DISTANCE = 2;


    public SpellChecker(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     * Computes the Jaccard coefficient for two sets A and B, where the size of set A is
     * <code>szA</code>, the size of set B is <code>szB</code> and the intersection
     * of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, int szB, int intersection) {
        //
        // YOUR CODE HERE
        //
        return (double) intersection / (szA + szB - intersection);
    }

    /**
     * Computing Levenshtein edit distance using dynamic programming.
     * Allowed operations are:
     * => insert (cost 1)
     * => delete (cost 1)
     * => substitute (cost 2)
     */
    private int editDistance(String s1, String s2) {
        //
        // YOUR CODE HERE
        //
        // Compute Levenshtein distance
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 2),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    /**
     * Checks spelling of all terms in <code>query</code> and returns up to
     * <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {
        //
        // YOUR CODE HERE
        //
        int numTerms = query.queryterm.size();
        if (numTerms == 0) return new String[0];

        List<List<KGramStat>> qCorrections = new ArrayList<>();
        String term;
        ArrayList<KGramStat> list;
        for (int i = 0; i < numTerms; i++) {
            term = query.queryterm.get(i).term;
            list = new ArrayList<>();
            if (index.getPostings(term) == null) {
                list = getRankedCorrections(term);
            } else {
                list.add(new KGramStat(term, 1));
            }
            qCorrections.add(list);
        }

        List<KGramStat> merged = mergeCorrections(qCorrections, limit);
        String[] result = new String[Math.min(limit, merged.size())];
        for (int i = 0; i < result.length; i++) {
            result[i] = merged.get(i).getToken();
        }
        return result;
    }

    /**
     * Computing ranked spelling corrections for <code>query</code> based on the
     * Jaccard coefficient and edit distance. Returns up to <code>limit</code>
     * ranked suggestions.
     */
    private ArrayList<KGramStat> getRankedCorrections(String term) {
        HashSet<String> kgrams = new HashSet<>();
        String termRegexp = "^" + term + "$";
        for (int i = 0; i <= termRegexp.length() - kgIndex.getK(); i++) {
            kgrams.add(termRegexp.substring(i, i + kgIndex.getK()));
        }

        // Find words containing kgrams
        HashSet<String> wordSet = new HashSet<>();
        for (String gram : kgrams) {
            for (KGramPostingsEntry entry : kgIndex.getPostings(gram)) {
                wordSet.add(kgIndex.id2term.get(entry.tokenID));
            }
        }

        // Compute Jaccard coefficient
        HashMap<String, Double> jaccardScores = new HashMap<>();
        int szA = kgrams.size();
        int szB;
        int intersection;
        for (String word : wordSet) {
            szB = 0;
            intersection = 0;
            String token = "^" + word + "$";
            for (int i = 0; i <= token.length() - kgIndex.getK(); i++) {
                String gram = token.substring(i, i + kgIndex.getK());
                if (kgrams.contains(gram)) {
                    intersection++;
                }
                szB++;
            }
            double jaccard = jaccard(szA, szB, intersection);

            if (jaccard >= JACCARD_THRESHOLD) {
                jaccardScores.put(word, jaccard);
            }
        }

        ArrayList<KGramStat> result = new ArrayList<>();
        PostingsList postings;
        int distance;
        for (String word : jaccardScores.keySet()) {
            distance = editDistance(term, word);
            if (distance <= MAX_EDIT_DISTANCE) {
                postings = index.getPostings(word);
                if (postings == null) {
                    result.add(new KGramStat(word, (double) distance / jaccardScores.get(word)));
                } else {
                    result.add(new KGramStat(word, (double) distance / (postings.size() * jaccardScores.get(word))));
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Merging ranked candidate spelling corrections for all query terms available in
     * <code>qCorrections</code> into one final merging of query phrases. Returns up
     * to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        //
        // YOUR CODE HERE
        //
        if (qCorrections.isEmpty()) return new ArrayList<>();

        List<KGramStat> result = qCorrections.getFirst();
        for (int i = 1; i < qCorrections.size(); i++) {
            List<KGramStat> current = qCorrections.get(i);
            List<KGramStat> merged = new ArrayList<>();
            for (KGramStat kgs : result) {
                for (KGramStat kgs2 : current) {
                    if (kgs.getToken().equals(kgs2.getToken())) {
                        kgs.score += kgs2.score;
                    }
                }
                merged.add(kgs);
            }
            for (KGramStat kgs : current) {
                boolean found = false;
                for (KGramStat kgs2 : result) {
                    if (kgs.getToken().equals(kgs2.getToken())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    merged.add(kgs);
                }
            }
            Collections.sort(merged);
            result = merged.subList(0, Math.min(limit, merged.size()));
        }
        return result;
    }
}
