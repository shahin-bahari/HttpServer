package com.shahin;


import com.shahin.httpServer.HttpServer;
import com.shahin.httpServer.router.StaticFile;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
	// write your code here

        HttpServer server = new HttpServer(2);
        server.getRouter().addStaticFile(new StaticFile("E://music",true));

        try {
            for(NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())){
                if(!ni.isVirtual() && ni.isUp() ){
                    System.out.println(ni.toString());
                    for(InetAddress ia : Collections.list(ni.getInetAddresses())){
                        if(ia.getAddress().length == 4){
                            server.listen(new InetSocketAddress(ia,8080));
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        waitForCommand();
        server.stopServer();

    }

    private static void waitForCommand(){
        while(true){
            String t = new Scanner(System.in).nextLine();
            if(t.equalsIgnoreCase("gc")){
                System.gc();
                System.out.println("gc");
            }else if(t.equalsIgnoreCase("info")){
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory();
                long allocatedMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                System.out.println("max memory is " + humanReadableByteCountBin(maxMemory));
                System.out.println("allocated memory is " + humanReadableByteCountBin(allocatedMemory));
                System.out.println("free memory is " + humanReadableByteCountBin(freeMemory));
            }else if(t.equalsIgnoreCase("exit")){
                return;
            }else if(t.equals("report")){
                //System.out.println(server.getRouter().report());
            }else if(t.equals("clients")){
                /*for(SocketChannel sc : server.getClientList()){
                    System.out.println(sc.toString());
                }*/
            }
        }
    }

    private static String humanReadableByteCountBin(long size){
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB", "PB", "EB" };
        final int exp = (int) (Math.log(size)/Math.log(1024));
        return String.format("%.2f %s", size / Math.pow(1024,exp), units[exp]);
    }
}
