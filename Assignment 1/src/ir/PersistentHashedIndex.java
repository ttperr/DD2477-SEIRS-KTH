/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.nio.charset.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "../../index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 12000017L; // 600000 * 20 next prime number
    public static final int ENTRY_LENGTH = 2*Long.BYTES + Integer.BYTES;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        //
        //  YOUR CODE HERE
        //
        private long ptr;
        private int size;
        private long hash;

        public Entry(long ptr, int size, long hash) {
            this.ptr = ptr;
            this.size = size;
            this.hash = hash;
        }

        public long getPtr() {
            return ptr;
        }

        public int getSize() {
            return size;
        }

        public long getHash() {
            return hash;
        }
    }


    // ==================================================================


    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr );
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /**
     *  Writes an entry to the dictionary hash table file.
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        //
        //  YOUR CODE HERE
        //
        try {
            dictionaryFile.seek(ptr);
            dictionaryFile.writeLong(entry.getPtr());
            dictionaryFile.writeInt(entry.getSize());
            dictionaryFile.writeLong(entry.getHash());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        try {
            dictionaryFile.seek(ptr);
            long ptrData = dictionaryFile.readLong();
            int size = dictionaryFile.readInt();
            long hash = dictionaryFile.readLong();
            if (ptrData == 0 && size == 0 && hash == 0) {
                return null;
            }
            return new Entry(ptrData, size, hash);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  {exception_description}
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  {exception_description}
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put( new Integer(data[0]), data[1] );
                docLengths.put( new Integer(data[0]), new Integer(data[2]) );
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list

            //
            //  YOUR CODE HERE
            //
            dictionaryFile.setLength( 0 ); // Remove old contents
            dictionaryFile.setLength( TABLESIZE );
            for (Map.Entry<String, PostingsList> entry : index.entrySet()) {
                String key = entry.getKey();
                PostingsList postingsList = entry.getValue();
                long hash = hashFunction(key);
                long ptr = free;
                Entry e = readEntry(hash);
                while (e != null) {
                    collisions++;
                    hash = (hash + ENTRY_LENGTH) % TABLESIZE;
                    e = readEntry(hash);
                }
                String postingsListBuilder = key + ";" + postingsList;
                int size = postingsListBuilder.getBytes().length;
                writeEntry(new Entry(ptr, size, hash), hash);
                free += writeData(postingsListBuilder, ptr);
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions" );
    }

    public static long hashFunction(String s) {
        return (Math.abs(s.hashCode()) + ENTRY_LENGTH)% TABLESIZE;
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        long hash = hashFunction(token);
        Entry e = readEntry(hash);
        while (e != null) {
            if (e.getHash() == hash) {
                String data = readData(e.getPtr(), e.getSize());
                if (data.startsWith(token)) {
                    String postingsListString = data.substring(token.length() + 1);
                    return PostingsList.fromString(postingsListString);
                }
            }
            hash = (hash + ENTRY_LENGTH) % TABLESIZE ;
            e = readEntry(hash);
        }
        return null;
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        //  YOUR CODE HERE
        //
        if (index.containsKey(token)) {
            PostingsList postingsList = index.get(token);
            if (postingsList.get(postingsList.size() - 1).docID == docID) {
                postingsList.get(postingsList.size() - 1).score++;
                postingsList.get(postingsList.size() - 1).offsets.add(offset);
            } else {
                postingsList.add(docID, offset, 1);
            }
        } else {
            PostingsList postingsList = new PostingsList();
            postingsList.add(docID, offset, 1);
            index.put(token, postingsList);
        }
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        long time = System.currentTimeMillis();
        System.err.print( "Writing index to disk..." );
        writeIndex();
        long elapsedTime = System.currentTimeMillis() - time;
        System.err.println( "done!" );
        System.err.println( "Elapsed time:    " + elapsedTime + "ms" );
    }
}
