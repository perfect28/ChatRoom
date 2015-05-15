package com.company;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

/**
* Created by sony on 2015/4/26.
*/
public class ChatRoom {
    public JPanel panel;
    public JTextField txt_username;
    private JButton btn_disconnect;
    private JButton btn_connect;
    public JTextField txt_send;
    private JButton btn_send;
    private JList txt_users;
    private JTextArea txt_content;

    private static final int PORT = 1023;
    static int WIDTH = 800;
    static int HEIGHT = 800;
    static Vector dialogs = new Vector(100);
    static Vector rooms = new Vector(100);

    Socket socket = null;
    PrintWriter printWriter;
    BufferedReader bufferedReader;
    Listen listen=null;  //监听线程
    public DefaultListModel listModel = new DefaultListModel();//存放当前在线用户信息

    //for file io
    byte[] inputBytes = null;
    byte[] sendBytes = null;
    int length = 0;
    DataInputStream dis = null;
    DataOutputStream dos = null;
    FileInputStream fis = null;
    FileOutputStream fos = null;

    public ChatRoom() {
        btn_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (socket == null){
                    try {
                        socket = new Socket(InetAddress.getLocalHost(), PORT);
                        //socket.setSoTimeout(10000);

                        printWriter = new PrintWriter(socket.getOutputStream(), true);
                        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        dis = new DataInputStream(socket.getInputStream());

                        //向服务器发送消息，已通知并同步其他客户端
                        String info = "INFO|"+txt_username.getText()+"|"+InetAddress.getLocalHost().toString();
                        printWriter.println(info);

                        //开启监听服务器端的线程，以随时更新界面
                        listen = new Listen(socket,txt_username.getText());
                        listen.start();

                    } catch (Exception ex) {
                        System.out.println("Exception:" + ex);
                    }
                }
            }
        });
        btn_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (socket != null) {
                    StringTokenizer st = new StringTokenizer(txt_send.getText()," ");
                    if (st.hasMoreTokens()){
                        String head = st.nextToken();
                        String filename = "";
                        if (head.equals("push")){
                            StringBuilder msg = new StringBuilder("Push|");
                            try {
                                String filepath = st.nextToken();
                                File file = new File(filepath);

                                if (!file.exists()){
                                    txt_content.append("Sorry,the file doesn't exists~\n");
                                    return ;
                                }

                                filename = file.getName();
                                msg.append(filename+"|");
                                long len = file.length();
                                msg.append(len);
                                printWriter.println(msg);
                                //这里写入流和下面写入流都是写到同一个socket的OutputStream里
                                //为了避免服务器读入的时候出错
                                //先写入printwriter,服务器会先读入命令，然后阻塞在dis.read方法处
                                //然后客户端这里继续执行下面的输入流
                                //服务端继续读取文件，并且在读完后自己break，而不是等带断开

                                fis = new FileInputStream(file);
                                sendBytes = new byte[1024];

                                try {
                                    dos = new DataOutputStream(socket.getOutputStream());
                                    System.out.println(file.getName() + "文件传送中！");
                                    long sum = 0;
                                    while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                                        sum += length;
                                        dos.write(sendBytes, 0, length);
                                        dos.flush();
                                    }
                                    if (sum!=len)
                                        System.out.println("文件上传不完整!");
                                    else
                                        System.out.println("文件上传完毕！");
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
                            } catch (FileNotFoundException e1) {
                                e1.printStackTrace();
                            }
                        }
                        else if (head.equals("pull")){
                            StringBuilder msg = new StringBuilder("Pull|");
                            filename = st.nextToken();
                            msg.append(filename+"");
                            printWriter.println(msg);
                        }
                        else{
                            String msg = "MSG|" + txt_send.getText();
                            printWriter.println(msg);
                        }
                    }
                }
                txt_send.setText("");
            }
        });
        btn_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        });
        txt_users.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()==2){   //When double click JList
                    JList source = (JList) e.getSource();
                    int index = source.getSelectedIndex();
                    Object item = source.getModel().getElementAt(index);
                    //JOptionPane.showConfirmDialog(null, "你选择了"+item.toString(), "title", JOptionPane.YES_NO_OPTION);

                    JFrame frame = new JFrame(txt_username.getText());

                    frame.setSize(500, 400);
                    frame.setLocationRelativeTo(null);

                    PTPRoom ptpRoom = new PTPRoom(txt_username.getText(),item.toString(),socket);
                    frame.setContentPane(ptpRoom.panel);
                    //frame.setDefaultCloseOperation(JFrame.NORMAL);
                    frame.setVisible(true);
                    dialogs.addElement(frame);
                    rooms.addElement(ptpRoom);
                }
            }
        });
    }

    void disconnect(){
        if (socket != null)
            printWriter.println("QUIT");
    }

    void flush_users(StringTokenizer st){
        listModel.clear();
        while(st.hasMoreTokens())
        {
            String user = st.nextToken();
            listModel.addElement(user);  //增加到用户列表中
        }
        txt_users.setModel(listModel);
    }

    class Listen extends Thread{    //监听服务器传送的信息
        String name=null;          //用户名
        BufferedReader reader=null ;    //输入流
        PrintWriter writer=null;     //输出流
        Socket ss=null;      //本地套接字

        //for file io
        byte[] inputBytes = null;
        byte[] sendBytes = null;
        int length = 0;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;

        public Listen(Socket socket,String name) {
            this.ss=socket;
            this.name = name;
            try{
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); //获取输入流
                writer = new PrintWriter(socket.getOutputStream(),true);  //获取输出流

                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
                //Listen.class.getClassLoader().getResource("").getPath()+"\\test.txt"
                //System.getProperty("user.dir")
            }
            catch(IOException e){
                disconnect(); //出错则断开连接
                e.printStackTrace(); //输出错误信息
            }
        }

        public void run(){
            String msg=null;
            while(socket!=null){
                try{
                    msg=reader.readLine();  //读取服务器端传来信息
                }
                catch(IOException ex){
                    disconnect(); //出错则断开连接
                    socket = null;
                    //ex.printStackTrace(); //输出错误信息
                    System.err.println("Server has closed!!");
                    return;
                }
                if (msg==null) {    //从服务器传来的信息为空则断开此次连接
                    socket = null;
                    return;
                }
                StringTokenizer st=new StringTokenizer(msg,"|");   //分解字符串
                String keyword=st.nextToken();

                if(keyword.equals("Flush")) {    //新用户连接信息
                    flush_users(st);
                }
                else if (keyword.equals("Notify")){
                    String info = st.nextToken();
                    if (info.equals("NewUser")){
                        String user=st.nextToken();
                        txt_content.append(user + " join in~\n");
                    }
                    else if (info.equals("Leave")){
                        String user=st.nextToken();
                        txt_content.append(user + " leave out~\n");
                    }
                }
                else if (keyword.equals("Download")){
                    //Download|filename|len
                    String filename = st.nextToken();
                    txt_content.append("开始接受"+filename+"文件的数据\n");
                    long len = Long.parseLong(st.nextToken());
                    try {
                        File file = new File("Download/"+filename);
                        if (!file.exists())
                            file.createNewFile();
                        fos = new FileOutputStream(file);

                        inputBytes = new byte[1024];
                        long sum = 0;
                        while ((length = dis.read(inputBytes, 0, inputBytes.length)) > 0) {
                            sum += length;
                            System.out.printf("已传输：%.2f%%\n", (float) ((sum * 1.0 / len) * 100));
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
                    txt_content.append("数据接受完成！\n");
                }
                else if(keyword.equals("MSG")) {    //聊天信息
                    String usr=st.nextToken();
                    txt_content.append(usr+" :");  //增加聊天信息到信息显示框
                    if (st.hasMoreTokens())
                        txt_content.append(st.nextToken());
                    if (st.hasMoreTokens())
                        txt_content.append(st.nextToken("\0"));
                    txt_content.append("\n");
                }
                else  if (keyword.equals("PTP")){   //私聊信息
                    String from = st.nextToken();
                    String to = st.nextToken();
                    String pri_msg = st.nextToken();

                    int id = 0;
                    boolean is_exist =false;
                    //遍历已经建立过的点对点聊天框
                    for(int i=0;i<rooms.size();i++)
                    {
                        PTPRoom ptpRoom  = (PTPRoom)rooms.elementAt(i);
                        //对面给我发的，在我这边显示为我给对面发的
                        if (ptpRoom.user_from.equals(to)&&ptpRoom.user_to.equals(from)){
                            is_exist = true;
                            id = i;
                            break;
                        }
                    }
                    if (!is_exist){
                        JFrame frame = new JFrame(to);

                        frame.setSize(500, 400);
                        frame.setLocationRelativeTo(null);

                        PTPRoom ptpRoom = new PTPRoom(to,from,socket);

                        frame.setContentPane(ptpRoom.panel);
                        //frame.setDefaultCloseOperation(JFrame.NORMAL);
                        frame.setVisible(true);
                        ptpRoom.show_msg(from+": "+pri_msg+"\n");

                        dialogs.addElement(frame);
                        rooms.addElement(ptpRoom);
                    }
                    else{
                        JFrame frame = (JFrame)dialogs.elementAt(id);
                        PTPRoom ptpRoom = (PTPRoom)rooms.elementAt(id);
                        if (frame.isVisible()==false)
                            frame.setVisible(true);
                        ptpRoom.show_msg(from+": "+pri_msg+"\n");
                    }
                }
                else if(keyword.equals("QUIT")) {   //断天连接信息
                    printWriter.println("Notify|Leave|"+name+"has gone~");
                    try{
                        listen=null;
                        socket.close();  //关闭端口
                        socket=null;
                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }
                    listModel.clear();  //移除用户列表
                    txt_content.append("you have disconnected~\n");
                    return;
                }
            }
        }
    }

//    public static void main(String[] args) {
//        JFrame frame = new JFrame("ChatRoom");
//
////        frame.setSize(WIDTH,HEIGHT);
////        int windowWidth = frame.getWidth();                     //获得窗口宽
////        int windowHeight = frame.getHeight();                   //获得窗口高
////        Toolkit kit = Toolkit.getDefaultToolkit();              //定义工具包
////        Dimension screenSize = kit.getScreenSize();             //获取屏幕的尺寸
////        int screenWidth = screenSize.width;                     //获取屏幕的宽
////        int screenHeight = screenSize.height;                   //获取屏幕的高
////        frame.setLocation(screenWidth/2-windowWidth/2, screenHeight/2-windowHeight/2);//设置窗口居中显示
//        frame.setSize(500, 400);
//        frame.setLocationRelativeTo(null);
//        //frame.setBounds(450,200,500,400);
//
//        frame.setContentPane(new ChatRoom().panel);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        //frame.pack();
//        frame.setVisible(true);
//    }
}

