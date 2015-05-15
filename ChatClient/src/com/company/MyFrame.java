package com.company;

import javax.swing.*;
import java.awt.*;

/**
 * Created by sony on 2015/5/14.
 */
public class MyFrame extends JFrame{
    private Image image = null;
    private  ImageIcon icon = new ImageIcon("background.jpg");
    public MyFrame(String title) throws HeadlessException {
        super(title);
        this.setSize(500, 400);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        image = icon.getImage();
        g.drawImage(image,0,0,null);
    }
}


