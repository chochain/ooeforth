///
/// @file 
/// @brief - Java eForth main
///
import java.io.*;
import eforth.*;

public class Eforth implements Runnable {                   /// ooeforth
    static final String APP_NAME = "ooeForth v2";
    IO           io;
    VM           vm;                                        ///< eForth virtual machine

    public Eforth(InputStream in, PrintStream out) {
        io = new IO(APP_NAME, in, out);
        vm = new VM(io);
    }
    
    public void run() {
        io.mstat();
        while (io.readline()) {
            if (!vm.outer()) break;
        }
        io.pstr("\n"+APP_NAME+" Done.\n");
    }

    public static void main(String args[]) {                /// main app
        new Eforth(System.in, System.out).run();
    }
}
