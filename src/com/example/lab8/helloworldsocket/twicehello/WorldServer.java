package com.example.lab8.helloworldsocket.twicehello;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class WorldServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(6000);
        System.out.println("Started server at: " + serverSocket.getLocalSocketAddress());
        Socket connection = serverSocket.accept();

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        for (int i=0;i<2;i++) {
            String hello = br.readLine();
            System.out.println("Client sends: " + hello);
        }

        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
        dataOutputStream.writeBytes(" world!\n");
        System.out.println("Client answers: " + br.readLine());
        dataOutputStream.close();
        br.close();
        connection.close();
        serverSocket.close();
    }
}
