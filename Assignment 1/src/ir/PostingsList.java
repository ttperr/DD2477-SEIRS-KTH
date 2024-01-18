/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();


    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
    return list.get( i );
    }

    // 
    //  YOUR CODE HERE
    //
    public void add(int docID, int offset, double score) {
        list.add(new PostingsEntry(docID, offset, score));
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        for (PostingsEntry entry : list) {
            s.append(entry.toString()).append("\n");
        }
        return s.toString();
    }
}

