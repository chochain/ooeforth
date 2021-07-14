import java.awt.*; 
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class AwtTest
{  
	Frame    frm   = new Frame("ooeForth");
	TextArea ta_i  = new TextArea("words");
	TextArea ta_o  = new TextArea("");
    AwtTest() {
    	ta_i.setFont(new Font("monospaced", Font.PLAIN, 20));
    	ta_i.setPreferredSize(new Dimension(800, 100));
    	ta_o.setFont(new Font("monospaced", Font.PLAIN, 20));
    	ta_o.setPreferredSize(new Dimension(800, 700));
    	frm.add(ta_i, BorderLayout.NORTH);
    	frm.add(ta_o, BorderLayout.SOUTH);
    	frm.setSize(840, 800);
    	frm.setVisible(true);

    	frm.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });
    	ta_i.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke) {
                char keyChar = ke.getKeyChar();
                if (keyChar <= 13) {
                    Scanner in = new Scanner(ta_i.getText());
                	ta_o.append(">>" + in.nextLine());
                	ta_i.setText("");
                }
                else {
                	System.out.print(keyChar);
                }
            }
        });
    }
    public void print(String str) {
    	ta_o.append(str);
    }
}  
