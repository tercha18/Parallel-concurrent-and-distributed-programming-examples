package com.example.lab7.readerwriteroptimized;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderWriterOptimizedEdit {
    public static void main(String[] args) throws InterruptedException {
        String lineSep = System.lineSeparator();
        File f = new File("temp.txt");
        ReentrantLock writing = new ReentrantLock(true);
        Condition fileExist = writing.newCondition();
        Semaphore reading = new Semaphore(10);
        class Reader extends Thread {
            String name;
            public Reader(String name) {
                this.name = name;
            }
            @Override
            public void run() {
                boolean printOnce = true;
                while (writing.isLocked()) {
                    if (printOnce) {
//                        System.out.println("Some writer is writing, i have to wait!");
                        System.out.println("Reader waiting.");
                        printOnce = false;
                    }
                }
                try {
                    reading.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    if (!f.exists()) {
                        writing.lock();
                        reading.release();
                        fileExist.await();
                        writing.unlock();
                        reading.acquire();
                    }
                    FileReader fileReader = new FileReader(f);
                    String fileContent = "";
                    Scanner scanner = new Scanner(fileReader);
                    StringBuilder sb = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        sb.append(scanner.nextLine());
                        sb.append(lineSep);
                    }
                    fileContent = sb.toString();
                    fileReader.close();

                    System.out.print("\u001B[32m");
                    System.out.println("Reader " + name + ": ");
                    System.out.print("\u001B[0m");
                    System.out.print(fileContent);

                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                } finally {
                    reading.release();
                }
            }
        }
        class Writer extends Thread {
            String name;
            public Writer(String name) {
                this.name = name;
            }
            @Override
            public void run() {
                boolean printOnce = true;
                while (reading.availablePermits() < 10) {
                    if (printOnce) {
//                        System.out.println("Some reader is reading now, i have to wait");
                        System.out.println("Writer waiting.");
                        printOnce = false;
                    }
                }
                boolean fileExst = f.exists();
                writing.lock();
                try {
                    Random r = new Random();
                    String[] words = {"aaa", "bbb", "ccc", "ddd", "eee", "fff"};
                    String s = words[r.nextInt(words.length)];
                    String fileContent = "";
                    if (fileExst) {
                        FileReader fileReader = new FileReader(f);
                        Scanner scanner = new Scanner(fileReader);
                        StringBuilder sb = new StringBuilder();
                        while (scanner.hasNextLine()) {
                            sb.append(scanner.nextLine());
                            sb.append(lineSep);
                        }
                        fileContent = sb.toString();
                    }
                    FileWriter fileWriter = new FileWriter(f);
//                    fileWriter.write(fileContent + "Writer " + name + " writes: " + s + lineSep);
                    fileWriter.write(fileContent + "    W " + name + ": " + s + lineSep);

                    System.out.print("\u001B[34m");
                    System.out.println("Writer " + name + ": ");
                    System.out.print("\u001B[0m");

                    System.out.print("\u001B[35m");
                    System.out.print(fileContent);
                    System.out.print("\u001B[0m");


                    System.out.print("\u001b[31m");
                    System.out.println("    appends: " + s);
                    System.out.print("\u001B[0m");
                    fileWriter.close();
                    if (!fileExst) {
                        fileExist.signalAll();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    writing.unlock();
                }
            }
        }
        new Writer("I").start();
        new Reader("I").start();
        new Reader("II").start();
        new Writer("II").start();
        new Reader("III").start();
        new Reader("IV").start();
        new Reader("V").start();
        new Reader("VI").start();

//        int maxIterationsNo = 100000;
//        int i = 0;
//        while (i < maxIterationsNo) {
//            Thread.sleep(20000);
//            new Writer("I").start();
//            new Writer("II").start();
//            new Writer("III").start();
//            new Reader("I").start();
//            new Reader("II").start();
//            new Reader("III").start();
//            new Reader("IV").start();
//            new Reader("V").start();
//            new Reader("VI").start();
//            i++;
//        }
    }
}
