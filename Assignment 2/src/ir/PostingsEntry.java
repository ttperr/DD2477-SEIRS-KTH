/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;

    /**
     * PostingsEntries are compared by their score (only relevant
     * in ranked retrieval).
     * <p>
     * The comparison is defined so that entries will be put in
     * descending order.
     */
    public int compareTo(PostingsEntry other) {
        return Double.compare(other.score, score);
    }


    //
    // YOUR CODE HERE
    //
    public ArrayList<Integer> offsets = new ArrayList<Integer>();

    public PostingsEntry(int docID, int offset, double score) {
        this.docID = docID;
        this.score = score;
        offsets.add(offset);
    }

    public PostingsEntry(int docID, ArrayList<Integer> offsets, double score) {
        this.docID = docID;
        this.score = score;
        this.offsets = offsets;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(docID).append(":");
        for (Integer offset : offsets) {
            s.append(offset).append(",");
        }
        s.append(score);
        return s.toString();
    }
}

