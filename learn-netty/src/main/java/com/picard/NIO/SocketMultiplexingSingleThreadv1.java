package com.picard.NIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SocketMultiplexingSingleThreadv1 {
    int port = 9091;
    private ServerSocketChannel server = null;
    private Selector selector = null;

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            //create selector
            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("server started....");
        try {
            for (;;) {
                while (selector.select(0) > 0) {//阻塞在此
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();  //从多路复用器取出有效的连接
                    Iterator<SelectionKey> iter = selectionKeys.iterator();
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isAcceptable()) {
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            System.out.println("一般用户数据到达会触发");
                            readHandler(key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);
            //
            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------");
            System.out.println("--new client--" + client.socket().getPort());
            System.out.println("-------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            for (;;) {
                read = client.read(buffer);
                if(read>0){
                    buffer.flip();
                    while(buffer.hasRemaining()){
                        client.write(buffer);
                    }
                    buffer.clear();
                }else if(read == 0){
                    break;
                }
            }
        } catch (IOException e) {
            client.close();
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadv1 service = new SocketMultiplexingSingleThreadv1();
        service.start();
    }
}
