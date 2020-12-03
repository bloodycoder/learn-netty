package com.picard.NIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class SocketNIO {
    public static void main(String[] args) throws IOException, InterruptedException {
        LinkedList<SocketChannel>clients = new LinkedList<>();
        ServerSocketChannel ssch = ServerSocketChannel.open();
        ssch.bind(new InetSocketAddress(9091));
        ssch.configureBlocking(false);
        for(;;){
            Thread.sleep(1000);
            SocketChannel client = ssch.accept();  //不阻塞
            if(client == null){
                System.out.println("null....");
            }else{
                client.configureBlocking(false);
                int port = client.socket().getPort();
                System.out.println("client post:"+port);
                clients.add(client);
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            for(SocketChannel c:clients){ //串行化
                if(!c.isConnected()){
                    continue;
                }
                try{
                    int num = c.read(buffer); //读到的字节数 0 -1断开
                    if(num>0){
                        buffer.flip();
                        byte[] aaa = new byte[buffer.limit()];
                        buffer.get(aaa);

                        String b = new String(aaa);
                        System.out.println(c.socket().getPort()+" : "+b);
                        buffer.clear();
                    }
                }
                catch (SocketException e){
                    e.printStackTrace();
                    c.close();
                }
            }
        }
    }
}
