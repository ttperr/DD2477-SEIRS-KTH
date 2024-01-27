/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;

public class PostingsList {

    /**
     * The postings list
     */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();


    /**
     * Number of postings in this list.
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns the ith posting.
     */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    // 
    //  YOUR CODE HERE
    //
    public void add(int docID, int offset, double score) {
        list.add(new PostingsEntry(docID, offset, score));
    }

    public void add(PostingsEntry entry) {
        list.add(entry);
    }

    /**
     * Converts the postings list to a string
     *
     * @return the string
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (PostingsEntry entry : list) {
            s.append(entry).append(";");
        }
        s.deleteCharAt(s.length() - 1);
        s.append("\n");
        return s.toString();
    }


    /**
     * Converts a string to a postings list
     *
     * @param s the string
     * @return the postings list
     */
    public static PostingsList fromString(String s) {
        PostingsList postingsList = new PostingsList();
        String[] entries = s.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            try {
                int docID = Integer.parseInt(parts[0]);
                String[] offsets = parts[1].split(",");
                postingsList.add(docID, Integer.parseInt(offsets[0]), 1);
                for (int i = 1; i < offsets.length - 1; i++) {
                    postingsList.get(postingsList.size() - 1).offsets.add(Integer.parseInt(offsets[i]));
                    postingsList.get(postingsList.size() - 1).score++;
                }
            } catch (NumberFormatException e) {
                System.err.println("Error parsing postings list: " + s);
                e.printStackTrace();
                System.exit(1);
            }
        }
        return postingsList;
    }
}

