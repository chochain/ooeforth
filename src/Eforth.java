///
/// @file 
/// @brief - Java eForth main
///
import java.util.*;
import java.io.*;
import eforth.*;

public class Eforth implements Runnable {                   /// ooeforth
    static final String VERSION = "ooeForth2.0";
    static final String GREET   = "Thank you.";
    IO           io;
    VM           vm;                                        ///< eForth virtual machine

    public Eforth(InputStream in, PrintStream out) {
        io = new IO(in, out);
        vm = new VM(io);
    }
    
    public void run() {
        io.pstr(VERSION+"\n");
        while (io.readline()) {
            vm.outer();
        }
        io.pstr(GREET);
    }

    public static void main(String args[]) {                /// ooeforth 1.12
        new Eforth(System.in, System.out).run();
    }
}
