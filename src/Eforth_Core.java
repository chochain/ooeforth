import java.util.*;
import java.io.*;

public class Eforth_Core implements Runnable {				// ooeforth
	static final String VERSION = "ooeForth2.0";
	static final String GREET   = "Thank you.";
	Eforth_VM 	 vm;
	InputStream  input;
	PrintWriter  output;

	Eforth_Core(InputStream in0, PrintWriter out0) { 
		input  = in0;
		output = out0;
		vm     = new Eforth_VM(); 
		vm.setOutput(out0);
	}
	/**
	 * 
	 */
	public void run() {
		try (Scanner sc = new Scanner(input)) {				// auto close
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

	public static void main0(String args[]) {				// ooeforth 1.12
		InputStream in  = System.in;
		
		try (PrintWriter out = new PrintWriter(System.out, true)) {
			new Eforth_Core(in, out).run();
		}
		catch (Exception e) {}
	}
}

