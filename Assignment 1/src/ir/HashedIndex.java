/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        // YOUR CODE HERE
        //
        if (index.containsKey(token)) {
            PostingsList postingsList = index.get(token);
            if (postingsList.get(postingsList.size() - 1).docID == docID) {
                postingsList.get(postingsList.size() - 1).score++;
            } else {
                postingsList.add(docID, offset);
            }
        } else {
            PostingsList postingsList = new PostingsList();
            postingsList.add(docID, offset);
            index.put(token, postingsList);
        }
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        System.out.println("Get postings for: " + token);
        System.out.println("Postings: " + index.get(token).toString());
        return index.get(token);
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
