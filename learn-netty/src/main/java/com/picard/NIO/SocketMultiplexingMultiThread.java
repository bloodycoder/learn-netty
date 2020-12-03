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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketMultiplexingMultiThread {
    private static final int port = 9091;
    private ServerSocketChannel server = null;
    private Selector selector1 = null;
    private Selector selector2 = null;
    private Selector selector3 = null;

    public static void main(String[] args) {
        SocketMultiplexingMultiThread service = new SocketMultiplexingMultiThread();
        service.initServer();
        NioThread T1 = new NioThread(service.selector1, 2);
        NioThread T2 = new NioThread(service.selector2);
        NioThread T3 = new NioThread(service.selector3);
        T1.start();
        T2.start();
        T3.start();
    }

    public void initServer() {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));
            //create selector
            selector1 = Selector.open();
            selector2 = Selector.open();
            selector3 = Selector.open();
            server.register(selector1, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class NioThread extends Thread {
    static int selectors = 0;
    Selector selector = null;
    int id = 0;
    boolean master = false;
    static BlockingQueue<SocketChannel>[] queue;
    static AtomicInteger idx = new AtomicInteger();
    NioThread(Selector sel,int n){
        //boss
        this.selector = sel;
        selectors = n;
        queue = new LinkedBlockingQueue[selectors];
        for(int i=0;i<n;i++){
            queue[i] = new LinkedBlockingQueue<>();
        }
        System.out.println("boss started...");
    }
    NioThread(Selector sel){
        this.selector = sel;
        id = idx.getAndIncrement()%selectors;
        System.out.println("worker "+id+" started");
    }

    @Override
    public void run() {
        try{
            for(;;){
                while(selector.select(10)>0){
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
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
                if(!master && !queue[id].isEmpty()){
                    ByteBuffer buffer = ByteBuffer.allocate(4096);
                    SocketChannel client = queue[id].take();
                    client.register(selector,SelectionKey.OP_READ,buffer);
                    System.out.println("--------");
                    System.out.println("--new client "+client.socket().getPort()+" 分配到worker "+(id));
                    System.out.println("--------");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);
            //
            int num = idx.getAndIncrement()%selectors;
            queue[num].add(client);
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
};
