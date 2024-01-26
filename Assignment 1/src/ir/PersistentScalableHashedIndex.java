package ir;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.*;

import static java.lang.Thread.sleep;

public class PersistentScalableHashedIndex extends PersistentHashedIndex implements Runnable {

    public static final int MAX_TOKENS = 70000;
    public static final long TABLE_SIZE = 611953L;
    private int threadNumber = 0;
    private static int threadLaunched = 0;
    private static int threadFinished = 0;
    private static final Object lockMerge = new Object();
    private static int mergingThreads = 0;
    private static volatile boolean finalMerge;
    private static final Queue<String> mergeWaitList = new LinkedList<>();
    private static int collisions = 0;
    private static int lastDocIDInfo = -1;


    public PersistentScalableHashedIndex() {
        try {
            readDocInfo();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PersistentScalableHashedIndex(int threadNumber, HashMap<String, PostingsList> index) {
        this.threadNumber = threadNumber;
        this.index = index;
        try {
            dictionaryFile = new RandomAccessFile(INDEX_DIR + "/" + threadNumber + DICTIONARY_FNAME, "rw");
            dataFile = new RandomAccessFile(INDEX_DIR + "/" + threadNumber + DATA_FNAME, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the document names and document lengths to file.
     *
     * @throws IOException {exception_description}
     */
    @Override
    protected void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream(INDEX_DIR + "/docInfo", true);
        for (Map.Entry<Integer, String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            if (key > lastDocIDInfo) {
                String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
                fout.write(docInfoEntry.getBytes());
                lastDocIDInfo = key;
            }
        }
        fout.close();
        docNames.clear();
        docLengths.clear();
    }

    /**
     * Inserts this token.
     */
    @Override
    public void insert(String token, int docID, int offset) {
        if (index.size() >= MAX_TOKENS) {
            (new Thread(new PersistentScalableHashedIndex(++threadLaunched, new HashMap<>(index)))).start();
            try {
                writeDocInfo();
            } catch (IOException e) {
                e.printStackTrace();
            }
            index.clear();
            System.out.println("Launched thread number " + threadLaunched);
            insert(token, docID, offset);
        } else {
            super.insert(token, docID, offset);
        }
    }

    @Override
    public void cleanup() {
        System.err.print("Writing index to disk...");
        long startTime = System.currentTimeMillis();
        writeIndex();
        run();
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("done!");
        System.err.println("Writing took: " + elapsedTime / 1000F + "s");
    }

    @Override
    public void run() {
        finalMerge = false;
        try {
            if (threadNumber == 0) {
                writeDocInfo();
                dictionaryFile = new RandomAccessFile(INDEX_DIR + "/" + 0 + DICTIONARY_FNAME, "rw");
                dataFile = new RandomAccessFile(INDEX_DIR + "/" + 0 + DATA_FNAME, "rw");
            }

            collisions += writeDictData(0);

            synchronized (lockMerge) {
                mergeWaitList.add(threadNumber + "");
                System.out.println("Thread number: " + threadNumber + " wrote, queue size: " + mergeWaitList.size());
            }
            threadFinished++;

            while (!finalMerge) {
                if (mergeWaitList.size() > 1) {
                    String prefix1;
                    String prefix2;
                    synchronized (lockMerge) {
                        if (mergeWaitList.size() < 2) {
                            continue;
                        }
                        prefix1 = mergeWaitList.poll();
                        prefix2 = mergeWaitList.poll();
                        mergingThreads++;
                    }
                    System.out.println("-- Merging thread count: " + mergingThreads);
                    finalMerge = mergeWaitList.isEmpty() && threadFinished > threadLaunched && mergingThreads == 1;
                    if (prefix1 == null || prefix2 == null) {
                        System.err.println("Error: prefix1 or prefix2 is null");
                        System.exit(1);
                    }
                    String mergedPrefix = mergeFiles(finalMerge, prefix1, prefix2);
                    synchronized (lockMerge) {
                        mergeWaitList.add(mergedPrefix);
                        mergingThreads--;
                    }
                    System.out.println("-- Merging thread count: " + mergingThreads);
                }
            }
            if (threadNumber == 0) {
                while (!finalMerge || mergingThreads > 0) {
                    sleep(1000);
                }
                readDocInfo();
                if (threadLaunched == 0 && threadFinished == 1) {
                    System.out.println("Collisions: " + collisions);
                    System.out.println("Repoint to main index");
                    dictionaryFile = new RandomAccessFile(INDEX_DIR + "/" + 0 + DICTIONARY_FNAME, "rw");
                    dataFile = new RandomAccessFile(INDEX_DIR + "/" + 0 + DATA_FNAME, "rw");
                } else if (threadFinished > threadLaunched) {
                    System.out.println("Collisions: " + collisions);
                    System.out.println("Thread finished: " + threadFinished);
                    System.out.println("Thread launched: " + threadLaunched);
                    System.out.println("Repoint to main index");
                    dictionaryFile = new RandomAccessFile(INDEX_DIR + "/" + DICTIONARY_FNAME, "rw");
                    dataFile = new RandomAccessFile(INDEX_DIR + "/" + DATA_FNAME, "rw");
                }
            }
            System.out.println("Thread number: " + threadNumber + " finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String mergeFiles(boolean finalMerge, String prefix1, String prefix2) throws IOException {
        String mergedPrefix = finalMerge ? "" : prefix1 + "x" + prefix2;

        System.out.println("-------- Thread n°" + threadNumber + " merging " + prefix1 + " and " + prefix2 + " into " + mergedPrefix);
        System.out.println("-------- Final merge: " + finalMerge);

        String dict1Name = INDEX_DIR + "/" + prefix1 + DICTIONARY_FNAME;
        String dict2Name = INDEX_DIR + "/" + prefix2 + DICTIONARY_FNAME;
        String data1Name = INDEX_DIR + "/" + prefix1 + DATA_FNAME;
        String data2Name = INDEX_DIR + "/" + prefix2 + DATA_FNAME;
        RandomAccessFile dict1 = new RandomAccessFile(dict1Name, "r");
        RandomAccessFile dict2 = new RandomAccessFile(dict2Name, "r");
        RandomAccessFile data1 = new RandomAccessFile(data1Name, "r");
        RandomAccessFile data2 = new RandomAccessFile(data2Name, "r");
        RandomAccessFile dict = new RandomAccessFile(INDEX_DIR + "/" + mergedPrefix + DICTIONARY_FNAME, "rw");
        RandomAccessFile data = new RandomAccessFile(INDEX_DIR + "/" + mergedPrefix + DATA_FNAME, "rw");
        dict.setLength(TABLE_SIZE * Entry.BYTES);

        long free = 0;

        String line1 = data1.readLine();
        String line2;
        HashMap<String, Boolean> duplicates = new HashMap<>();

        while (line1 != null) {
            String token1 = line1.split("\\[")[0];
            String postingsList1 = line1.substring(token1.length() + 1);
            long hash1 = hashFunction(token1);
            long ptrDict1 = hash1 * Entry.BYTES;
            Entry temp = readEntry(ptrDict1, dict);
            while (temp != null) {
                hash1 = (hash1 + 1) % TABLE_SIZE;
                ptrDict1 = hash1 * Entry.BYTES;
                temp = readEntry(ptrDict1, dict);
            }
            line2 = isInData(token1, dict2, data2);
            if (line2 != null) {
                duplicates.put(token1, true);
                // Merging posting lists
                PostingsList postingList1 = PostingsList.fromString(postingsList1);
                PostingsList postingList2 = PostingsList.fromString(line2.substring(token1.length() + 1));
                PostingsList postingListMerged = new PostingsList();
                int i = 0;
                int j = 0;
                while (i < postingList1.size() && j < postingList2.size()) {
                    PostingsEntry entry1 = postingList1.get(i);
                    PostingsEntry entry2 = postingList2.get(j);
                    if (entry1.docID == entry2.docID) {
                        ArrayList<Integer> offsetsMerged = new ArrayList<>(entry1.offsets);
                        offsetsMerged.addAll(entry2.offsets);
                        postingListMerged.add(new PostingsEntry(entry1.docID, offsetsMerged, entry1.score + entry2.score));
                        i++;
                        j++;
                    } else if (entry1.docID < entry2.docID) {
                        postingListMerged.add(entry1);
                        i++;
                    } else {
                        postingListMerged.add(entry2);
                        j++;
                    }
                }
                while (i < postingList1.size()) {
                    postingListMerged.add(postingList1.get(i));
                    i++;
                }
                while (j < postingList2.size()) {
                    postingListMerged.add(postingList2.get(j));
                    j++;
                }
                // Writing merged posting list
                String stringMerged = token1 + "[" + postingListMerged;
                int size = stringMerged.getBytes().length;
                ByteBuffer buffer = ByteBuffer.allocate(size);
                buffer.put(stringMerged.getBytes());
                data.write(buffer.array());
                buffer.clear();
                writeEntry(new Entry(free, size, hash1), ptrDict1, dict);

                free += size;
            } else {
                line1 = line1 + "\n";
                int size = line1.getBytes().length;
                ByteBuffer buffer = ByteBuffer.allocate(size);
                buffer.put(line1.getBytes());
                data.write(buffer.array());
                buffer.clear();
                writeEntry(new Entry(free, size, hash1), ptrDict1, dict);

                free += size;
            }
            line1 = data1.readLine();
        }
        // Adding remaining tokens from data
        data2.seek(0);
        line2 = data2.readLine();
        while (line2 != null) {
            String token2 = line2.split("\\[")[0];
            if (!duplicates.containsKey(token2)) {
                long hash2 = hashFunction(token2);
                long ptrDict2 = hash2 * Entry.BYTES;
                line2 = line2 + "\n";
                int size = line2.getBytes().length;
                ByteBuffer buffer = ByteBuffer.allocate(size);
                buffer.put(line2.getBytes());
                data.write(buffer.array());
                buffer.clear();
                Entry temp = readEntry(ptrDict2, dict);
                while (temp != null) {
                    hash2 = (hash2 + 1) % TABLE_SIZE;
                    ptrDict2 = hash2 * Entry.BYTES;
                    temp = readEntry(ptrDict2, dict);
                }
                writeEntry(new Entry(free, size, hash2), ptrDict2, dict);

                free += size;
            }
            line2 = data2.readLine();
        }

        dict1.close();
        dict2.close();
        data1.close();
        data2.close();
        dict.close();
        data.close();

        // Delete old files
        ProcessBuilder pb = new ProcessBuilder("rm", dict1Name, dict2Name, data1Name, data2Name);
        pb.start();
        System.out.println("-------- Merged " + mergedPrefix);
        return mergedPrefix;
    }

    private String isInData(String token, RandomAccessFile dict, RandomAccessFile data) throws IOException {
        long hash = hashFunction(token);
        long ptrDict = hash * Entry.BYTES;
        Entry temp = readEntry(ptrDict, dict);
        while (temp != null) {
            if (temp.getHash() == hash) {
                data.seek(temp.getPtr());
                ByteBuffer buffer = ByteBuffer.allocate(temp.getSize());
                data.readFully(buffer.array());
                String line = new String(buffer.array());
                buffer.clear();
                String token2 = line.split("\\[")[0];
                if (token2.equals(token)) {
                    return line;
                }
            }
            hash = (hash + 1) % TABLE_SIZE;
            ptrDict = hash * Entry.BYTES;
            temp = readEntry(ptrDict, dict);
        }
        return null;
    }
}
