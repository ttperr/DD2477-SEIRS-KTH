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

    /**
     * The directory where the persistent index files are stored.
     */
    public static final String INDEX_DIR = "../../indexDavis"; // TODO: Change this to the path of the index directory

    public static final String EUCLIDEAN_LENGTHS = "euclideanLengths";

    /**
     * The dictionary file name
     */
    public static final String DICTIONARY_FNAME = "dictionary";

    /**
     * The data file name
     */
    public static final String DATA_FNAME = "data";

    /**
     * The terms file name
     */
    public static final String TERMS_FNAME = "terms";

    /**
     * The doc info file name
     */
    public static final String DOCINFO_FNAME = "docInfo";

    /**
     * The dictionary hash table on disk can fit this many entries.
     */
    public static final long TABLE_SIZE = 611953L;// 3500017L;// TODO: Change this to the size of the dictionary hash table

    /**
     * The dictionary hash table is stored in this file.
     */
    RandomAccessFile dictionaryFile;

    /**
     * The data (the PostingsLists) are stored in this file.
     */
    RandomAccessFile dataFile;

    /**
     * Pointer to the first free memory cell in the data file.
     */
    long free = 0L;

    /**
     * The cache as a main-memory hash map.
     */
    HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();


    // ===================================================================

    /**
     * A helper class representing one entry in the dictionary hashtable.
     */
    public class Entry {
        //
        //  YOUR CODE HERE
        //
        private long ptr;
        private int size;
        private long hash;
        public static final int BYTES = 2 * Long.BYTES + Integer.BYTES;

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
     * Constructor. Opens the dictionary file and the data file.
     * If these files don't exist, they will be created.
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile(INDEX_DIR + "/" + DICTIONARY_FNAME, "rw");
            dataFile = new RandomAccessFile(INDEX_DIR + "/" + DATA_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes data to the data file at a specified place.
     *
     * @return The number of bytes written.
     */
    int writeData(String dataString, long ptr) {
        try {
            dataFile.seek(ptr);
            byte[] data = dataString.getBytes();
            dataFile.write(data);
            return data.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * Reads data from the data file
     */
    String readData(long ptr, int size) {
        try {
            dataFile.seek(ptr);
            byte[] data = new byte[size];
            dataFile.readFully(data);
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /**
     * Writes an entry to the dictionary hash table file.
     *
     * @param entry The key of this entry is assumed to have a fixed length
     * @param ptr   The place in the dictionary file to store the entry
     */
    protected void writeEntry(Entry entry, long ptr, RandomAccessFile dictionaryFile) {
        //
        //  YOUR CODE HERE
        //
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Entry.BYTES);
            buffer.putLong(entry.getPtr());
            buffer.putInt(entry.getSize());
            buffer.putLong(entry.getHash());
            dictionaryFile.seek(ptr);
            dictionaryFile.write(buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads an entry from the dictionary file.
     *
     * @param ptr The place in the dictionary file where to start reading.
     */
    protected Entry readEntry(long ptr, RandomAccessFile dictionaryFile) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Entry.BYTES);
            dictionaryFile.seek(ptr);
            dictionaryFile.readFully(buffer.array());
            long ptrData = buffer.getLong();
            int size = buffer.getInt();
            long hash = buffer.getLong();
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
     * Writes the document names and document lengths to file.
     *
     * @throws IOException {exception_description}
     */
    protected void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEX_DIR + "/docInfo");
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     * Reads the document names and document lengths from file, and
     * put them in the appropriate data structures.
     *
     * @throws IOException {exception_description}
     */
    protected void readDocInfo() throws IOException {
        File file = new File(INDEX_DIR + "/docInfo");
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(Integer.valueOf(data[0]), data[1]);
                docLengths.put(Integer.valueOf(data[0]), Integer.valueOf(data[2]));
            }
        }
        freader.close();
    }


    /**
     * Write the index to files.
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
            collisions = writeDictData(collisions);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(collisions + " collisions");
    }

    /**
     * Write the dictionary and the postings list in the data
     *
     * @param collisions The number of collisions already found
     * @return The number of collisions
     * @throws IOException {exception_description}
     */
    protected int writeDictData(int collisions) throws IOException {
        dictionaryFile.setLength(0); // Remove old contents
        dictionaryFile.setLength(TABLE_SIZE * Entry.BYTES);
        for (Map.Entry<String, PostingsList> entry : index.entrySet()) {
            String key = entry.getKey();
            PostingsList postingsList = entry.getValue();
            long hash = hashFunction(key);
            long ptrData = free;
            long ptrDict = hash * Entry.BYTES;
            Entry e = readEntry(ptrDict, dictionaryFile);
            while (e != null) {
                collisions++;
                hash = (hash + 1) % TABLE_SIZE;
                ptrDict = hash * Entry.BYTES;
                e = readEntry(ptrDict, dictionaryFile);
            }
            String postingsListBuilder = key + ">" + postingsList;
            int size = postingsListBuilder.getBytes().length;
            writeEntry(new Entry(ptrData, size, hash), ptrDict, dictionaryFile);
            free += writeData(postingsListBuilder, ptrData);
        }
        return collisions;
    }

    /**
     * Returns the hash value of a string modulo the table size.
     */
    public long hashFunction(String s) {
        return Math.abs(s.hashCode()) % TABLE_SIZE;
    }


    // ==================================================================


    /**
     * Returns the postings for a specific term, or null
     * if the term is not in the index.
     */
    public PostingsList getPostings(String token) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        long hash = hashFunction(token);
        long ptrDict = hash * Entry.BYTES;
        Entry e = readEntry(ptrDict, dictionaryFile);
        while (e != null) {
            if (e.getHash() == hash) {
                String data = readData(e.getPtr(), e.getSize());
                if (data.startsWith(token)) {
                    return PostingsList.fromString(data.substring(token.length() + 1));
                }
            }
            hash = (hash + 1) % TABLE_SIZE;
            ptrDict = hash * Entry.BYTES;
            e = readEntry(ptrDict, dictionaryFile);
        }
        return null;
    }


    /**
     * Inserts this token in the main-memory hashtable.
     */
    public void insert(String token, int docID, int offset) {
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
     * Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println(index.keySet().size() + " unique words");
        System.err.print("Writing index to disk...");
        long time = System.currentTimeMillis();
        writeIndex();
        long elapsedTime = System.currentTimeMillis() - time;
        System.err.println("done!");
        System.err.println("Writing took: " + elapsedTime / 1000F + "s");
    }

    @Override
    public void putDocEuclideanLength(int docID) {
        int length = 0;
        for (Map.Entry<String, PostingsList> entry : index.entrySet()) {
            PostingsList postingsList = entry.getValue();
            for (int i = 0; i < postingsList.size(); i++) {
                if (postingsList.get(i).docID == docID) {
                    length += (postingsList.get(i).offsets.size())*(postingsList.get(i).offsets.size());
                }
            }
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(INDEX_DIR + "/" + EUCLIDEAN_LENGTHS, true));
            writer.write(docID + ";" + Math.sqrt(length) + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getEuclideanLength(int docID) {
        try {
            File file = new File(INDEX_DIR + "/" + EUCLIDEAN_LENGTHS);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                if (Integer.parseInt(data[0]) == docID) {
                    return Double.parseDouble(data[1]);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
