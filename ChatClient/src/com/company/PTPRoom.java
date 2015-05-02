package com.company;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

/**
 * Created by sony on 2015/4/28.
 */
public class PTPRoom {
    public JPanel panel;
    private JTextArea txt_content;
    private JTextField txt_send;
    private JButton btn_send;
    private JLabel txt_userinfo;

    public String user_from = "";
    public String user_to = "";
    Socket socket = null;

    BufferedReader bufferedReader = null;
    PrintWriter printWriter = null;

    public PTPRoom(final String from,String to,Socket s) {
        this.user_from = from;
        this.user_to = to;
        this.socket = s;

        String info = new String("from: "+from+" to: "+to);
        txt_userinfo.setText(info);
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        btn_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (socket != null) {
                    StringBuffer msg = new StringBuffer("PTP");
                    msg.append("|"+user_from);
                    msg.append("|"+user_to);
                    msg.append("|"+txt_send.getText());
                    printWriter.println(msg);
                    txt_content.append(user_from+": "+txt_send.getText()+"\n");
                    txt_send.setText("");
                }
            }
        });
    }

    public void show_msg(String msg)
    {
        txt_content.append(msg);
    }
}
