package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

public class SocketServer {
    private static final int SERVER_PORT = 1023;
    static Vector clients=new Vector(10);   //存储连接客户信息
    static ServerSocket serverSocket=null;    //建立服务器socket
    static Socket socket=null;   //套接字连接
    BufferedReader order = null;

    public SocketServer() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Start Listening!!");

            order = new BufferedReader(new InputStreamReader(System.in));

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    String order_msg;
                    while(true){
                        try {
                            order_msg = order.readLine();
                            if (order_msg == null)
                                return;
                            if (order_msg.equals(""))
                                continue;
                            StringTokenizer st = new StringTokenizer(order_msg," ");
                            String head = st.nextToken();
                            if (head.equals("kick")){
                                String usrname = st.nextToken();
                                boolean flag =false;
                                for(int i=0;i<clients.size();i++){
                                    Client client = (Client)clients.elementAt(i);
                                    if (usrname.equals(client.name)){
                                        disconnect(client);
                                        flush_users();
                                        remove_user(client);
                                        flag = true;
                                        break;
                                    }
                                }
                                if (!flag)
                                    System.out.println("所踢用户不在线上~");
                            }
                            else if (head.equals("checkall")){
                                System.out.println("当前在线用户为:");
                                for(int i=0;i<clients.size();i++){
                                    Client client = (Client)clients.elementAt(i);
                                    System.out.println(client.name);
                                }
                            }
                            else
                                continue;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t.start();

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
        System.out.println(client.name + " disconnect");
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

        //for file io
        byte[] inputBytes = null;
        byte[] sendBytes = null;
        int length = 0;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;

        public Client(Socket socket) {
            this.socket = socket;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(),true);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());


                //获取用户基本信息
                String info = reader.readLine();
                StringTokenizer st_info = new StringTokenizer(info,"|");
                String head = st_info.nextToken();
                name = st_info.nextToken();
                ip = st_info.nextToken();

                System.out.println("Client(" + name + ") come in...");
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
                } catch (IOException e) {//客户端异常退出
                    //e.printStackTrace();
                    System.err.println("client error out!!");
                    socket = null;
                    clients.removeElement(socket);
                    return;
                }

                if (line == null) {//输入输出流被关闭
                    socket = null;
                    clients.removeElement(socket);
                    return;
                }

                StringTokenizer st = new StringTokenizer(line,"|");
                String keyword = st.nextToken();

                if (keyword.equals("MSG")){
                    //信息的格式：用户+说话内容
                    StringBuffer msg = new StringBuffer("MSG|");
                    msg.append(name+"|");
                    msg.append(st.nextToken("\0"));
                    SendToAll(msg);
                }
                else if (keyword.equals("Push")){
                    inputBytes = new byte[1024];
                    String filename = "";
                    System.out.println("开始接收数据...");
                    try {
                        filename = st.nextToken();
                        long len = Long.parseLong(st.nextToken());
                        File file = new File("Download/"+filename);
                        if (!file.exists())
                            file.createNewFile();
                        fos = new FileOutputStream(file);

                        long sum = 0;
                        while ((length = dis.read(inputBytes, 0, inputBytes.length)) > 0) {
                            //System.out.println(length);
                            sum += length;
                            //输出%得用%%
                            System.out.printf("已传输：%.2f%%\n",(float)((sum*1.0/len)*100));
                            fos.write(inputBytes, 0, length);
                            fos.flush();
                            if (sum == len)
                                break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("完成接收");

                    StringBuffer msg = new StringBuffer("MSG|Server|");
                    msg.append(filename);
                    msg.append(" has pushed up successfully~!");
                    SendToAll(msg);
                }
                else if (keyword.equals("Pull")){
                    String filename = st.nextToken();
                    File file = new File("Download/"+filename);
                    long len = file.length();

                    //如果文件不存在
                    if (!file.exists())
                    {
                        String msg = "MSG|Server|The file doesn't exist!!";
                        writer.println(msg);
                        continue;
                    }

                    //先发送信息让客户端准备接受文件
                    StringBuilder msg = new StringBuilder("Download|");
                    msg.append(filename+"|");
                    msg.append(len);
                    writer.println(msg);

                    try {
                        fis = new FileInputStream(file);
                        sendBytes = new byte[1024];
                        if (file.exists()){
                            System.out.println("开始传送数据...");
                            try {
                                while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                                    System.out.println("传送:"+length);
                                    dos.write(sendBytes, 0, length);
                                    dos.flush();
                                }
                                System.out.println("数据传送完成...");
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            } finally {
                                //并不能关闭，否则socket的输入输出流就直接断了
//                                    try {
//                                        dos.close();
//                                    } catch (IOException e1) {
//                                        e1.printStackTrace();
//                                    }
                            }
                        }
                        else{
                            writer.println("MSG|Server|"+filename+"doesn't exist");
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                else if (keyword.equals("PTP")){
                    String from = st.nextToken();
                    String to = st.nextToken();
                    String msg = st.nextToken();
                    SendToOne(from,to,msg);
                }
                else if (keyword.equals("QUIT")){
                    disconnect(this);
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