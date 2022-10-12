package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server(){
        done = false;
        connections = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done){
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        }
        catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message){
        for(ConnectionHandler ch : connections){
            if(ch != null){
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown(){
        try{
            done = true;
            pool.shutdown();
            if(!server.isClosed()){
               server.close();
            }
            for(ConnectionHandler ch : connections){
                ch.shutdown();
            }
        }
        catch (IOException e){
            //ignore it because it goes again and again
        }
    }

    class ConnectionHandler implements Runnable{

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client){
            this.client = client;
        }

        @Override
        public void run() {
            try{
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                String message;
                while((message = in.readLine()) != null){
                    if(message.startsWith("/nick ")){
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length == 2){
                            nickname = messageSplit[1];
                            System.out.println(nickname + " connected!");
                            broadcast(nickname + " entered the chat server!");
                        }
                    }
                    else if(message.startsWith("/msg")){
                        String[] messageSplit = message.split(" ", 2);
                        broadcast(nickname + ": " + messageSplit[1]);
                    }
                    else if(message.startsWith("/quit")){
                        broadcast(nickname + " left the chat!");
                        shutdown();
                    }
                }
            }catch (IOException e){
                shutdown();
            }
        }

        public void sendMessage(String message){
            out.println(message);
        }

        public void shutdown(){
            try{
                in.close();
                out.close();
                if (!client.isClosed()){
                    client.close();
                }
            }
            catch (IOException e){
                //ignore it because it goes again and again
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
