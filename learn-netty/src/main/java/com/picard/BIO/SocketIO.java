package com.picard.BIO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketIO {
    /*
    * socketIO server
    * nc 127.0.0.1 9091
    * */
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(9091);
        System.out.println("step1:new server socket");
        Socket client = server.accept();
        System.out.println("client in");

        InputStream in = client.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        while(true){
            System.out.println(reader.readLine());
        }
    }
}
