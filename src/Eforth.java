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
    InputStream  input;                                     ///< console input
    PrintWriter  output;                                    ///< console output
    VM           vm;                                        ///< eForth virtual machine

    Eforth(InputStream in, PrintStream out) {
        input  = in;
        output = new PrintWriter(out, true);
        vm     = new VM();
        vm.setOutput(output);                               ///< pipe output stream
    }
    
    public void run() {
        try (Scanner cin = new Scanner(input)) {            ///< auto close
            output.println(VERSION);
            while (vm.ok()) {
                String tib = cin.nextLine();
                vm.parse(tib);
            }
        }
        catch (Exception e) { 
            output.println(e.getMessage()); 
        }
        finally {
            output.println(GREET);
        }
    }

    public static void main(String args[]) {                /// ooeforth 1.12
        new Eforth(System.in, System.out).run();
    }
}
