package com.example.additionaltask;

import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderWriterNonBlocking {

    public static final int MAX_READER = 15;

    public static void main(String[] args) throws InterruptedException {
        ReentrantLock writing = new ReentrantLock(true);
        Semaphore reading = new Semaphore(MAX_READER);
        Map<String, Idea> ideas = new HashMap<>();

        File tempFile = new File("src/temp.txt");
        File wordsFile = new File("src/words.txt");
        List<String> popularWords = readFile(wordsFile);
        File pattern = new File("src/pattern.txt");
        copyFile(pattern.getPath(), tempFile.getPath());  // pattern.txt -> temp.txt


        class Reader extends Thread {
            String name;
            List<String> contentLines;

            public Reader(String name) {
                this.name = name;
//                System.out.println("Reader " + this.name + " was started");
            }

            @Override
            public void run() {
                boolean printOnce = true;
                while (writing.isLocked()) {
                    if (printOnce) {
                        System.out.println("Reader " + this.name + ": some writer is writing. I have to wait.");
                        printOnce = false;
                    }
                }
                try {
                    reading.acquire();
                    System.out.println("Reader " + this.name + " is reading");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                contentLines = readFile(tempFile);
                reading.release();
                System.out.println("Reader " + this.name + " has finished reading");

                TreeMap<String, Integer> wordMap = wordCounter(contentLines);
                printCountResult(name, wordMap);
            }
        }

        class Writer extends Thread {
            String name;
            Idea idea = new Idea();

            public Writer(String name) {
                this.name = name;
            }

            @Override
            public void run() {
                boolean haveIdea = false;
                while (!haveIdea) {
                    try {
                        reading.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    List<String> contentLines = readFile(tempFile);
                    reading.release();

                    Random random = new Random();
                    int numberLine = random.nextInt(contentLines.size());
                    idea.setNumberLine(numberLine);
                    String[] words = contentLines.get(numberLine).split(" ");
                    idea.setBefore(words[random.nextInt(words.length)]);
                    idea.setWord(popularWords.get(random.nextInt(popularWords.size())));
                    if (!ideas.containsKey(idea)) {
                        ideas.put(name, idea);
                        haveIdea = true;
                        System.out.print("\u001B[34m");
                        System.out.println("> Writer " + this.name + " have idea!");
                        System.out.print("\u001B[0m");
                    }
                }

                boolean printOnce = true;
                while (reading.availablePermits() < MAX_READER) {
                    if (printOnce) {
                        System.out.println("> Writer " + this.name + ": some reader is reading now, i have to wait");
                        printOnce = false;
                    }
                }
                writing.lock();
                System.out.println("> Writer " + this.name + ": lock!");
                List<String> contentLines = readFile(tempFile);

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
                contentLines.set(idea.getNumberLine(), updateLine);
                writeFile(contentLines, tempFile);
                System.out.println("> Writer " + this.name + ": unlock!");
                writing.unlock();
                System.out.print("\u001B[31m");
                System.out.println("> Writer " + this.name +
                        " add '" + idea.getWord() +
                        "' after '" + idea.getBefore() +
                        "' in " + (idea.getNumberLine() + 1) + " line.");
                ideas.put(name, null);
                System.out.print("\u001B[0m");
            }
        }

        for (int i = 1; i <= 5; i++) {
            new Writer(i+"").start();
            for (int j = 1; j <= 3; j++) {
                new Reader(((i*3)-3+j)+"").start();
            }
        }

//        for (int i = 0; i < 2; i++) {
//            new Writer(i+"//1").start();
//            new Writer(i+"//2").start();
//            new Writer(i+"//3").start();
//            new Reader(i+"//1").start();
//            new Reader(i+"//2").start();
//            new Reader(i+"//3").start();
//            for (int j = 0; j < 1; j++) {
//                new Reader(i+"/"+j+"/1").start();
//                new Reader(i+"/"+j+"/2").start();
//                new Reader(i+"/"+j+"/3").start();
//                new Writer(i+"/"+j+"/1").start();
//                new Writer(i+"/"+j+"/2").start();
//                new Writer(i+"/"+j+"/3").start();
//            }
//            Thread.sleep(10000);
//        }
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
            //copy the file content in bytes
            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }
            inStream.close();
            outStream.close();

            System.out.println("Plik zostal skopiowany!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static TreeMap<String, Integer> wordCounter(List<String> lines) {
        TreeMap<String, Integer> wordMap = new TreeMap<String, Integer>();
        for (String line : lines) {
            for (String word : line.split(" ")) {
                word = word.toLowerCase();

                if (!isLetter(word.charAt(word.length() - 1))) {
                    word = word.substring(0, word.length() - 1);
                }
                if (!wordMap.containsKey(word)) {
                    wordMap.put(word, 1);
                } else {
                    int value = wordMap.get(word) + 1;
                    wordMap.put(word, value);
                }
            }
        }
        return wordMap;
    }

    private static void printCountResult(String name, TreeMap<String, Integer> wordMap) {
        String result = "Reader " + name + " - counting result:\n";
        for (Map.Entry<String, Integer> entry : wordMap.entrySet()) {
            String word = entry.getKey();
            Integer number = entry.getValue();
            result = result + "     " + word + " " + number + "\n";
        }
        System.out.print("\u001B[32m");
        System.out.println(result);
        System.out.print("\u001B[0m");
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z');
    }
}

class Idea {
    private int numberLine;
    private String before;
    private String word;

    public Idea() {
    }

    public Idea(int numberLine, String before, String word) {
        this.numberLine = numberLine;
        this.before = before;
        this.word = word;
    }

    public int getNumberLine() {
        return numberLine;
    }

    public void setNumberLine(int line) {
        this.numberLine = line;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }
}
