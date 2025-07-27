///
/// @file 
/// @brief - Java eForth main
///
import java.util.*;
import java.io.*;
import eforth.*;

public class Eforth implements Runnable {                       /// ooeforth
    static final String VERSION = "ooeForth2.0";
    static final String GREET   = "Thank you.";
    VM           vm;
    InputStream  input;
    PrintWriter  output;

    Eforth(InputStream in0, PrintStream out0) {
        input  = in0;
        output = new PrintWriter(out0, true);
        vm     = new VM();
        vm.setOutput(output);
    }
    
    public void run() {
        try (Scanner sc = new Scanner(input)) {                /// auto close
            output.println(VERSION);
            while (vm.ok()) {
                String tib = sc.nextLine();
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
