package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

public class SocketServer {
    private static final int SERVER_PORT = 1023;
    static Vector clients=new Vector(10);   //存储连接客户信息
    static ServerSocket serverSocket=null;    //建立服务器socket
    static Socket socket=null;   //套接字连接

    public SocketServer() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Start Listening!!");
            while (true) {
                socket = serverSocket.accept();
                //System.out.println(socket.getRemoteSocketAddress() + "join~");
                Client client = new Client(socket);
                clients.addElement(client);
                client.start();
                flush_users();
                add_user(client);
            }
        } catch (Exception e) {
            System.out.println("Exception:" + e);
        }
    }

    void  add_user(Client client)
    {
        StringBuffer newUser = new StringBuffer("Notify|NewUser");
        newUser.append("|"+client.name);
        SendToAll(newUser);
    }

    void  remove_user(Client client)
    {
        StringBuffer leaveUser = new StringBuffer("Notify|Leave");
        leaveUser.append("|"+client.name);
        SendToAll(leaveUser);
    }

    void flush_users()
    {
        StringBuffer flush = new StringBuffer("Flush");
        for(int i=0;i<clients.size();i++){
            Client client = (Client)clients.elementAt(i);
            flush.append("|"+client.name);
        }
        SendToAll(flush);
    }

    void SendToAll(StringBuffer msg)
    {
        for(int i=0;i<clients.size();i++) {
            Client client = (Client)clients.elementAt(i);
            client.send(msg);
        }
    }

    void SendToOne(String from,String to,String msg)
    {
        for(int i=0;i<clients.size();i++)
        {
            Client client = (Client)clients.elementAt(i);
            if (client.name.equals((to))){
                StringBuffer pri_msg = new StringBuffer("PTP");
                pri_msg.append("|"+from);
                pri_msg.append("|"+to);
                pri_msg.append("|"+msg);
                client.send(pri_msg);
                break;
            }
        }
    }

    void disconnect(Client client)
    {
        System.out.println(client.ip + "disconnect\n");
        client.send(new StringBuffer("QUIT"));
//        try {
//            client.socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        client.socket = null;
        clients.removeElement(client);
    }

    class Client extends Thread {
        Socket socket;    //连接端口
        String name;   //用户姓名
        String ip;    //客户端IP地址
        BufferedReader reader;  //输入流
        PrintWriter writer;   //输出流

        public Client(Socket socket) {
            this.socket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(),true);
                System.out.println("Client(" + getName() + ") come in...");

                //获取用户基本信息
                String info = reader.readLine();
                StringTokenizer st_info = new StringTokenizer(info,"|");
                String head = st_info.nextToken();
                name = st_info.nextToken();
                ip = st_info.nextToken();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void send(StringBuffer msg){
            writer.println(msg);
        }

        @Override
        public void run() {
            while(true){
                String line = null;
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                if (line == null) {
                    return;
                }

                StringTokenizer st = new StringTokenizer(line,"|");
                String keyword = st.nextToken();

                if (keyword.equals("MSG")){
                    //信息的格式：用户+说话内容
                    StringBuffer msg = new StringBuffer("MSG|");
                    msg.append(name);
                    msg.append(st.nextToken("\0"));
                    SendToAll(msg);
                }
                else if (keyword.equals("PTP")){
                    String from = st.nextToken();
                    String to = st.nextToken();
                    String msg = st.nextToken();
                    SendToOne(from,to,msg);
                }
                else if (keyword.equals("QUIT")){
                    disconnect(this);
                    //这里并没有能和新添加用户做好的配合
                    flush_users();
                    remove_user(this);
                }
            }
        }
    }

    public static void main(String[] args) {
        new SocketServer();
    }
}