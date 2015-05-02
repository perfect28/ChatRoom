package com.company;

import sun.net.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sony on 2015/5/2.
 */
public class Login {
    private JPanel panel;
    private JTextField txt_pwd;
    private JButton btn_login;
    private JButton btn_reg;
    private JTextField txt_user;
    private JLabel lable_user;
    private JLabel label_pw;

    static JFrame login_frame;

    static Connection conn;
    static Statement stmt;
    //static Set<String> usrs = new HashSet<String>();

    public Login() {
        btn_login.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String usr = txt_user.getText();
                String pwd = txt_pwd.getText();
//                if (usrs.contains(usr)){
//                    JOptionPane.showConfirmDialog(null, "您输入的用户名已经登陆~","error", JOptionPane.CLOSED_OPTION);
//                    return ;
//                }
                if (check_user(usr,pwd)) {
                    //usrs.add(usr);
                    JFrame chat_frame = new JFrame("ChatRoom");
                    chat_frame.setSize(500, 400);
                    chat_frame.setLocationRelativeTo(null);
                    //frame.setBounds(450,200,500,400);

                    ChatRoom chatRoom = new ChatRoom();
                    chatRoom.txt_username.setText(txt_user.getText());
                    chat_frame.setContentPane(chatRoom.panel);
                    chat_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    chat_frame.setVisible(true);
                    login_frame.setVisible(false);

                    try {
                        stmt.close();
                        conn.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                else{
                    JOptionPane.showConfirmDialog(null, "您输入的用户名或密码有误，请重新登陆~","error", JOptionPane.CLOSED_OPTION);
                }
            }
        });
        btn_reg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = txt_user.getText();
                String password = txt_pwd.getText();
                try{
                    String sql = "select * from user_info";    //要执行的SQL
                    ResultSet rs = stmt.executeQuery(sql);//创建数据对象
                    while (rs.next()) {
                        String usr = rs.getString("username");
                        if (username.equals(usr)){
                            JOptionPane.showConfirmDialog(null, "抱歉，该id已经被注册~","error", JOptionPane.CLOSED_OPTION);
                            rs.close();
                            return;
                        }
                    }
                    rs.close();

                    sql = "insert into user_info values(\'"+username+"\',\'"+password+"\');";
                    int judge = stmt.executeUpdate(sql);//创建数据对象
                    if (judge > 0)
                        JOptionPane.showConfirmDialog(null, "恭喜注册成功~~", "info", JOptionPane.CLOSED_OPTION);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    boolean check_user(String username,String password){
        try{
            String sql = "select * from user_info";    //要执行的SQL
            ResultSet rs = stmt.executeQuery(sql);//创建数据对象
            while (rs.next()) {
                String usr = rs.getString("username");
                String pwd = rs.getString("password");
                if (username.equals(usr)&&password.equals(pwd))
                    return true;
            }
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static void database_ini(){
        try{
            //调用Class.forName()方法加载驱动程序
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("成功加载MySQL驱动！");
        }catch(ClassNotFoundException e1){
            System.out.println("找不到MySQL驱动!");
            e1.printStackTrace();
        }

        String url="jdbc:mysql://localhost:3306/chatdb";    //JDBC的URL
        //调用DriverManager对象的getConnection()方法，获得一个Connection对象

        try {
            conn = DriverManager.getConnection(url, "root", "950612");
            //创建一个Statement对象
            stmt = conn.createStatement(); //创建Statement对象
            System.out.print("成功连接到数据库！");
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        database_ini();

        login_frame = new JFrame("login");
        login_frame.setSize(400, 300);
        login_frame.setLocationRelativeTo(null);

        Login login = new Login();
        login_frame.setContentPane(login.panel);

        login_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login_frame.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    class ImagePanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            String imageString = ResourceManager.class.getClassLoader().getResource("water.jpg").toString();
            ImageIcon icon = new ImageIcon(imageString);
            g.drawImage(icon.getImage(), 0, 0, null);
        }
    }
}
