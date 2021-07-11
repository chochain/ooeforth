import java.util.*;
import java.io.*;
import java.time.LocalTime;

public class Eforth112 {	// ooeforth
	static StringTokenizer tok  = null;
	static Eforth_VM       vm   = new Eforth_VM();

	public static void run_outer(String tib)
	{
		tok = new StringTokenizer(tib);
		vm.useTok(tok);
		
		String idiom = "";
		while (vm.run && tok.hasMoreTokens()) {
			idiom = tok.nextToken().trim();

			Eforth_Code w = vm.find(idiom);					// search dictionary
			if (w != null) {  								// word found
				if (!vm.comp || w.immd) {
					try { 									// immediate mode
						vm.xt(w);							// execute word
					}
					catch (Exception e) { 
						System.out.print(e); 
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
					System.out.println(idiom + " ?");
					vm.comp = false; 
				}
			}
		}
	}

	public static void main(String args[]) {				// ooeforth 1.12
		System.out.println("ooeForth1.12");

		// outer interpreter
		Scanner in = new Scanner(System.in);
		while (vm.run) {
			String tib = in.nextLine();
			run_outer(tib);
			
			if (vm.comp) {
				System.out.print("> ");
			}
			else vm.ss_dump();
		}
		in.close();
		System.out.println("Thank you.");
	}
	
}

