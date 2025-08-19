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
        io = new IO(APP_NAME, in, out);                     ///< instantiate IO
        vm = new VM(io);                                    ///< instantiate VM
    }
    
    public void run() {
        vm.ok(true);                                        /// * prompt VM ready
        while (io.readline()) {                             /// * fetch from input 
            if (!vm.outer()) break;                         /// * outer interpreter
        }
        io.pstr("\n"+APP_NAME+" Done.\n");                  /// * exit prompt
    }

    public static void main(String args[]) {                /// main app
        new Eforth(System.in, System.out).run();
    }
}
