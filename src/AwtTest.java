import java.awt.*; 
import java.awt.event.*;
import java.util.*;

public class AwtTest
{  
	Frame    frame 	= new Frame("ooeForth");
	TextArea input  = new TextArea("input");
	TextArea output = new TextArea("output");

    AwtTest() {
    	frame.add(input,  BorderLayout.NORTH);
    	frame.add(output, BorderLayout.SOUTH);
    	frame.setSize(400, 400);
    	frame.setVisible(true);

    	frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });
    	input.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent ke) {
                char keyChar = ke.getKeyChar();
                if (keyChar <= 13) {
                    Scanner in = new Scanner(input.getText());
                	output.append(">>"+in.nextLine());
                	input.setText("");
                }
                else {
                	System.out.print(keyChar);
                }
            }
        });
    }
    public void print(String str) {
    	output.append(str);
    }
}  
