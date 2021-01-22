package com.example.additionaltask;

import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderWriterNonBlockingMy {

    public static final int N_CHANGES = 50;
    public static final int N_WRITERS = 5;
    public static final int N_READERS = 15;

    public static final int MAX_READER = N_WRITERS+N_READERS;
//    public static final int MAX_READER = N_READERS;

    public static void main(String[] args) throws InterruptedException {
        ReentrantLock writing = new ReentrantLock(true);
        Semaphore reading = new Semaphore(MAX_READER);
        Map<String, Idea> ideas = new HashMap<>();
        final Integer[] nChanges = {0};


        File pattern = new File("src/pattern.txt");
        File tempFile = new File("src/temp.txt");
        copyFile(pattern.getPath(), tempFile.getPath());  // pattern.txt -> temp.txt
        File wordsFile = new File("src/words.txt");
        List<String> popularWords = readFile(wordsFile);


        class Reader extends Thread {
            private String name;
            private List<String> contentLines;
            private TreeMap<String, Integer> wordMap = new TreeMap<>();

            public Reader(String name) {
                this.name = name;
            }

            private void countWords() {
                for (String line : this.contentLines) {
                    for (String word : line.split(" ")) {
                        word = word.toLowerCase();

                        if (!isLetter(word.charAt(word.length() - 1))) {
                            word = word.substring(0, word.length() - 1);
                        }
                        if (!this.wordMap.containsKey(word)) {
                            this.wordMap.put(word, 1);
                        } else {
                            int value = this.wordMap.get(word) + 1;
                            this.wordMap.put(word, value);
                        }
                    }
                }
            }

            private void printCountResult() {
                String result = "Reader " + this.name + " - counting result:\n";
                for (Map.Entry<String, Integer> entry : this.wordMap.entrySet()) {
                    String word = entry.getKey();
                    Integer number = entry.getValue();
                    result = result + "     " + word + " " + number + "\n";
                }
                System.out.print("\u001B[32m");
                System.out.println(result);
                System.out.print("\u001B[0m");
            }

            @Override
            public void run() {
                do {
                    System.out.println("reader" + this.name + " run");
                    boolean printOnce = true;
                    while (writing.isLocked()) {
                        if (printOnce) {
                            System.out.println("Reader " + this.name + ": some writer is writing. I have to wait.");
                            printOnce = false;
                        }
                    }

                    try {
                        reading.acquire();
                        System.out.println("Reader " + this.name + " is reading.");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    this.contentLines = readFile(tempFile);
                    int nChange = nChanges[0];
                    System.out.println("Reader " + this.name + " has finished reading.");
                    reading.release();

                    countWords();
//                    printCountResult();
                    wordMap.clear();

//                    System.out.println("~~~~reader "+this.name+" before wait "+nChange);
//                    printOnce = false;
//                    while (nChange == nChanges[0] && nChange<N_CHANGES) {
//                        if (!printOnce) {
//                            System.out.println("~~~~reader " + this.name + " wait" + nChange + " " + nChanges[0]);
//                            printOnce = true;
//                        }
//                    }
//                    System.out.println("~~~~reader "+this.name+" after waiting "+nChange);
                } while (nChanges[0] < N_CHANGES);
            }
        }

        class Writer extends Thread {
            String name;
            List<String> contentLines = new ArrayList<>();
            Idea idea = new Idea();

            public Writer(String name) {
                this.name = name;
            }

            private void createIdea(List<String> contentLines) {
                Random random = new Random();
                System.out.println(this.name + "Size: " + contentLines.size());
                int numberLine = random.nextInt(contentLines.size());
                this.idea.setNumberLine(numberLine);
                String[] words = contentLines.get(numberLine).split(" ");
                this.idea.setBefore(words[random.nextInt(words.length)]);
                this.idea.setWord(popularWords.get(random.nextInt(popularWords.size())));
            }

            private String appendIdea(List<String> contentLines) {
                String[] words = contentLines.get(idea.getNumberLine()).split(" ");

                String updateLine = "";
                for (int i = 0; i < words.length; i++) {
                    if (words[i].equals(idea.getBefore())) {
                        String word = words[i];
                        if (word.charAt(word.length() - 1) == '.' ||
                                word.charAt(word.length() - 1) == '!' ||
                                word.charAt(word.length() - 1) == '?') {
                            words[i] = word.substring(0, word.length() - 1) + " " + idea.getWord() + '.';
                        } else {
                            words[i] = words[i] + " " + idea.getWord();
                        }
                    }
                    updateLine = updateLine + words[i];
                    if (i < words.length - 1) updateLine += " ";
                }
                return updateLine;
            }

            private void printResult() {
                System.out.print("\u001B[31m");
                System.out.println("> Writer " + this.name +
                        " add '" + this.idea.getWord() +
                        "' after '" + this.idea.getBefore() +
                        "' in " + (this.idea.getNumberLine() + 1) + " line.");
                System.out.print("\u001B[0m");
            }

            @Override
            public void run() {
                while (nChanges[0] < N_CHANGES) {
                    System.out.println("Writer.run "+nChanges[0]); // TODO
                    boolean haveIdea = false;
                    while (!haveIdea) {
//                        boolean printOnce = true;
//                        while (writing.isLocked()) {
//                            if (printOnce) {
//                                System.out.println("Writer " + this.name + ": some writer is writing. I have to wait.");
//                                printOnce = false;
//                            }
//                        }
//                        try {
//                            reading.acquire();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                        while(this.contentLines.isEmpty()) {
                            this.contentLines = readFile(tempFile);
                        }

                        reading.release();

                        createIdea(contentLines);
                        if (!ideas.containsKey(this.idea) && this.idea.getWord() != null) {
                            ideas.put(this.name, this.idea);
                            haveIdea = true;
                            System.out.print("\u001B[34m");
                            System.out.println("> Writer " + this.name + " have idea!");
                            System.out.print("\u001B[0m");
                        }
                    }

                    boolean printOnce = true;
                    while (reading.availablePermits() < MAX_READER) {
                        if (printOnce) {
                            System.out.println("> Writer " + this.name + ": someone is reading now, I have to wait");
                            printOnce = false;
                        }
                    }

                    writing.lock();
                    System.out.println("> Writer " + this.name + ": lock!");
                    if (nChanges[0]<N_CHANGES) {
                        nChanges[0]++;
                        List<String> contentLines = readFile(tempFile);
                        String updateLine = appendIdea(contentLines);
                        contentLines.set(idea.getNumberLine(), updateLine);
                        writeFile(contentLines, tempFile);
                    }
                    System.out.println("> Writer " + this.name + ": unlock!");
                    printResult();
                    writing.unlock();
                    ideas.put(this.name, null);
                    this.contentLines.clear();
                }
            }
        }

        for (int i = 0; i < N_WRITERS; i++) {
            new Writer(i+"").start();
        }
        for (int i = 0; i < N_READERS; i++) {
            new Reader(i+"").start();
        }
    }

    private static List<String> readFile(File file) {
        List<String> lines = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) lines.add(scanner.nextLine());
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private static void writeFile(List<String> contentLines, File file) {
        try {
            FileWriter fileWriter = new FileWriter(file);
            for (String line : contentLines) {
                fileWriter.write(line);
                if (contentLines.indexOf(line) + 1 < contentLines.size()) {
                    fileWriter.write("\n");
                }
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyFile(String sourcePath, String exitPath) {
        InputStream inStream = null;
        OutputStream outStream = null;

        try {
            File afile = new File(sourcePath);
            File bfile = new File(exitPath);

            inStream = new FileInputStream(afile);
            outStream = new FileOutputStream(bfile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }
            inStream.close();
            outStream.close();

            System.out.println("Copy was successful!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z');
    }
}

//class Idea {
//    private int numberLine;
//    private String before;
//    private String word;
//
//    public Idea() {
//    }
//
//    public Idea(int numberLine, String before, String word) {
//        this.numberLine = numberLine;
//        this.before = before;
//        this.word = word;
//    }
//
//    public int getNumberLine() {
//        return numberLine;
//    }
//
//    public void setNumberLine(int line) {
//        this.numberLine = line;
//    }
//
//    public String getBefore() {
//        return before;
//    }
//
//    public void setBefore(String before) {
//        this.before = before;
//    }
//
//    public String getWord() {
//        return word;
//    }
//
//    public void setWord(String word) {
//        this.word = word;
//    }
//}

