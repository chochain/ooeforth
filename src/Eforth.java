///
/// @file 
/// @brief - Java eForth main
///
import java.io.*;
import eforth.*;

public class Eforth implements Runnable {                   /// ooeforth
    static final String APP_NAME = "ooeForth2.0\n";
    static final String GREET    = "Thank you.\n";
    IO           io;
    VM           vm;                                        ///< eForth virtual machine

    public Eforth(InputStream in, PrintStream out) {
        io = new IO(in, out);
        vm = new VM(io);
    }
    
    public void run() {
        io.pstr(APP_NAME);
        while (io.readline()) {
            vm.outer();
        }
        io.pstr(GREET);
    }

    public static void main(String args[]) {                /// main app
        new Eforth(System.in, System.out).run();
    }
}
