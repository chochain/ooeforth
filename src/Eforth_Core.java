import java.util.*;
import java.io.*;

class Eforth_Core implements Runnable {						// ooeforth
	Eforth_VM 	 vm;
	InputStream  input;
	PrintWriter  output;

	Eforth_Core(InputStream in0, PrintWriter out0) { 
		input  = in0;
		output = out0;
		vm     = new Eforth_VM(); 
		vm.setOutput(out0);
	}

	public void run() {
		try (Scanner sc = new Scanner(input)) {
			// outer interpreter
			output.println("ooeForth2");
			while (vm.run) {
				String tib = sc.nextLine();
			
				_outer_interp(tib);
				
				if (vm.comp) {
					output.print("> ");
				}
				else vm.ss_dump();
			}
		}
		catch (Exception e) { 
			output.println(e.getMessage()); 
		}
		finally {
			output.println("Thank you.");
		}
	}

	private void _outer_interp(String tib)
	{
		StringTokenizer tok = new StringTokenizer(tib);
		
		vm.setInput(tok);									// set input stream

		while (vm.run && tok.hasMoreTokens()) {
			String idiom = tok.nextToken().trim();

			Eforth_Code w = vm.find(idiom);					// search dictionary
			if (w != null) {  								// word found
				if (!vm.comp || w.immd) {
					try { 									// immediate mode
						vm.xt(w);							// execute word
					}
					catch (Exception e) { 
						output.print(e); 
					}
				}
				else vm.colon_add(w);						// or in compile mode
			}
			else { 											// word not found
				try {
					int n=Integer.parseInt(idiom, vm.base); // not word, try number
					if (vm.comp) {  						// compile integer literal
						vm.colon_add(new Eforth_Code("dolit", n));	// append literal to latest defined word
					}
					else {
						vm.ss_push(n);
					}
				}											// or push number on stack
				catch (NumberFormatException  ex) {			// catch number errors
					output.println(idiom + " ?");
					vm.comp = false; 
				}
			}
		}
	}

	public static void main(String args[]) {				// ooeforth 1.12
		InputStream in  = System.in;
		
		try (PrintWriter out = new PrintWriter(System.out, true)) {
			new Eforth_Core(in, out).run();
		}
		catch (Exception e) {}
	}
}

