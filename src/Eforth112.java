import java.util.*;
import java.io.*;
import java.time.LocalTime;

public class Eforth112 {	// ooeforth
	static Stack<Integer>  stack  = new Stack<>();
	static Stack<Integer>  rstack = new Stack<>();
	static MyList<Code>    dict   = new MyList<Code>();
	static StringTokenizer tok    = null;

	static boolean  vm_comp = false;
	static boolean  vm_run  = true;
	static int 		base    = 10;
	static int 		fence   = 0;
	static int 		wp, ip;

	interface XT<Code> {
		void run(Code c);
	}
	static class MyList<Code> extends ArrayList<Code> {
		public Code last() 			 { return get(size() - 1); 		}
		public Code last(int offset) { return get(size() - offset); }
	}
	// primitive words
	static class Code {
		public String 	name;
		public int 		idx       = 0;
		public boolean 	immediate = false;
		public int      stage     = 0;
		public String 	literal;

		public List<Code>    pf   = new ArrayList<>();
		public List<Code>    pf1  = new ArrayList<>();
		public List<Code>    pf2  = new ArrayList<>();
		public List<Integer> qf   = new ArrayList<>() ;

		public Code(String n) {
			name = n;
			fence++;
			immediate = false;
		}
		public Code(String n, int d)    { name=n;  qf.add(d); }
		public Code(String n, String l) { name=n;  literal=l; }

		public Code immediate()         { immediate=true; return this; }
		public void xt() {
			if (lookUp.containsKey(name)) {
				lookUp.get(name).run(this);
			} 
			else { 
				rstack.push(wp); 
				rstack.push(ip);
				wp=idx; ip = 0;	// wp points to current colon object
				for (Code w:pf) {
					try { w.xt(); ip++; } 
					catch (ArithmeticException e) {}
				}
				ip=rstack.pop(); 
				wp=rstack.pop();
			}
		}
		public void addCode(Code w) { this.pf.add(w);}
	}
	
	static Hashtable<String, XT<Code>> lookUp = new Hashtable<>() {{
		// stacks
		put( "dup",   c -> stack.push(stack.peek()) 				);
		put( "over",  c -> stack.push(stack.get(stack.size()-2)) 	);
		put( "swap",  c -> stack.add(stack.size()-2,stack.pop()) 	);
		put( "rot",   c -> stack.push(stack.remove(stack.size()-3)) );
		put( "drop",  c -> stack.pop() 								);
		put( "nip",   c -> stack.remove(stack.size()-2)  			);
		put( "2drop", c -> { stack.pop(); stack.pop(); } 			);
		put( ">r",    c -> rstack.push(stack.pop())  				);
		put( "r>",    c -> stack.push(rstack.pop())  				);
		put( "r@",    c -> stack.push(rstack.peek()) 				);
		put( "push",  c -> rstack.push(stack.pop())  				);
		put( "pop",   c -> stack.push(rstack.pop())  				);
		put( "2dup",  c -> stack.addAll(stack.subList(stack.size()-2, stack.size())) 	);
		put( "2over", c -> stack.addAll(stack.subList(stack.size()-4, stack.size()-2)) 	);
		put( "4dup",  c -> stack.addAll(stack.subList(stack.size()-4, stack.size())) 	);
		put( "-rot",  c -> { stack.push(stack.remove(stack.size()-3)); stack.push(stack.remove(stack.size()-3)); });
		put( "2swap", c -> { stack.push(stack.remove(stack.size()-4)); stack.push(stack.remove(stack.size()-4)); });
		put( "pick",  c -> { int i=stack.pop(); int n=stack.get(stack.size()-i-1);    stack.push(n); });
		put( "roll",  c -> { int i=stack.pop(); int n=stack.remove(stack.size()-i-1); stack.push(n); });
		// math
		put( "+",     c -> stack.push(stack.pop()+stack.pop()) );
		put( "-",     c -> { int n= stack.pop(); stack.push(stack.pop()-n); });
		put( "*",     c -> stack.push(stack.pop()*stack.pop()) );
		put( "/",     c -> { int n= stack.pop(); stack.push(stack.pop()/n); });
		put( "*/",    c -> { int n=stack.pop();  stack.push(stack.pop()*stack.pop()/n); });
		put( "*/mod", c -> { 
			int n=stack.pop();
			int m=stack.pop()*stack.pop();
			stack.push(m%n);
			stack.push(m/n);
		});
		put( "mod",   c -> { int n= stack.pop(); stack.push(stack.pop()%n); });
		put( "and",   c -> stack.push(stack.pop()&stack.pop()) 	);
		put( "or",    c -> stack.push(stack.pop()|stack.pop()) 	);
		put( "xor",   c -> stack.push(stack.pop()^stack.pop()) 	);
		put( "negate",c -> stack.push(-stack.pop())            	);
		// logic
		put( "0=",    c -> stack.push((stack.pop()==0)?-1:0)   	);
		put( "0<",    c -> stack.push((stack.pop()< 0)?-1:0)	);
		put( "0>",    c -> stack.push((stack.pop()> 0)?-1:0)	);
		put( "=",     c -> { int n= stack.pop(); stack.push((stack.pop()==n)?-1:0); });
		put( ">",     c -> { int n= stack.pop(); stack.push((stack.pop()>n )?-1:0); });
		put( "<",     c -> { int n= stack.pop(); stack.push((stack.pop()<n )?-1:0); });
		put( "<>",    c -> { int n= stack.pop(); stack.push((stack.pop()!=n)?-1:0); });
		put( ">=",    c -> { int n= stack.pop(); stack.push((stack.pop()>=n)?-1:0); });
		put( "<=",    c -> { int n= stack.pop(); stack.push((stack.pop()<=n)?-1:0); });
		// output
		put( "base@", c -> stack.push(base)		);
		put( "base!", c -> base = stack.pop()	);
		put( "hex",   c -> base = 16			);
		put( "decimal",c-> base = 10			);
		put( "cr",    c -> System.out.println()	);
		put( ".",     c -> System.out.print(Integer.toString(stack.pop(),base)+" ") );
		put( ".r",    c -> { 
			int n=stack.pop();
			String s=Integer.toString(stack.pop(),base);
			for (int i=0; i+s.length()<n; i++) System.out.print(" ");
			System.out.print(s+" ");
		});
		put( "u.r",   c -> { 
			int n=stack.pop();
			String s=Integer.toString(stack.pop()&0x7fffffff,base);
			for (int i=0;i+s.length()<n; i++) System.out.print(" ");
			System.out.print(s+" ");
		});
		put( "key",   c -> stack.push((int)tok.nextToken().charAt(0)) 		);
		put( "emit",  c -> System.out.print(Character.toChars(stack.pop()))	);
		put( "space", c -> System.out.print(" ") );
		put( "spaces",c -> { 
			int n=stack.pop();
			for (int i=0; i<n; i++) System.out.print(" "); 
		});
		// literals
		put( "[",     c -> vm_comp = false	);
		put( "]",     c -> vm_comp = true	);
		put( "'",     c -> { 
			String s = tok.nextToken(); 
			boolean found=false;
			for (var w:dict) {
				if (s.equals(w.name)) { 
					stack.push(w.idx); 
					found = true;break;
				}
			}
			if (!found) stack.push(-1);
		});
		
		put( "dolit", c -> stack.push(((Code)c).qf.get(0))			);	// integer literal
		put( "dostr", c -> stack.push(((Code)c).idx)				);	// string literals
		put( "$\"",   c -> {  // -- w a
			String s=tok.nextToken("\"");
			Code last = dict.get(dict.size()-1);
			last.addCode(new Code("dostr",s));			// literal=s
			tok.nextToken();
			stack.push(last.idx);stack.push(last.pf.size()-1);
		});
		put( "dotstr",c -> System.out.print(((Code)c).literal)		);
		put( ".\"",   c -> {
			String s=tok.nextToken("\"");
			Code last = dict.get(dict.size()-1);
			last.addCode(new Code("dotstr", s));		// literal=s
		});
		put( "(",     c -> tok.nextToken("\\)")						);
		put( ".(",    c -> System.out.print(tok.nextToken("\\)"))	);
		put( "\\",    c -> tok.nextToken("\n")						);

		// structure: if else then
		put( "branch",c -> {
			Code cc = (Code)c;
			if(!(stack.pop()==0)) {
				for (var w:cc.pf) w.xt();
			}
			else {
				for (var w:cc.pf1) w.xt();
			}
		});
		put( "if",    c -> { 
			Code last = dict.get(dict.size()-1);
			last.addCode(new Code("branch"));
			dict.add(new Code("temp"));
		});
		put( "else",  c -> {
			Code last = dict.get(dict.size()-2);
			Code temp = dict.get(dict.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.get(last.pf.size()-1).stage=1; 
		});
		put( "then",  c -> {
			Code last = dict.get(dict.size()-2);
			Code temp = dict.get(dict.size()-1);
			if (last.pf.get(last.pf.size()-1).stage==0) {
				last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
				dict.remove(dict.size()-1);
			} 
			else {
				last.pf.get(last.pf.size()-1).pf1.addAll(temp.pf);
				if (last.pf.get(last.pf.size()-1).stage==1) {
					dict.remove(dict.size()-1);
				}
				else temp.pf.clear();
			}
		});
		// loops
		put( "loops", c -> {
			Code cc = (Code)c;
			if (cc.stage==1) {	// again
				while(true) { for (var w:cc.pf) w.xt(); }
			}
			if (cc.stage==2) {	// while repeat
				while (true) {
					for (var w:cc.pf) w.xt();
					if (stack.pop()==0) break;
					for (var w:cc.pf1) w.xt();
				}
			} 
			else {
				while(true) {	// until
					for (var w:cc.pf) w.xt();
					if(stack.pop()!=0) break;
				}
			}
		});
		put( "begin", c -> { 
			Code last = dict.get(dict.size()-1);
			last.addCode(new Code("loops"));
			dict.add(new Code("temp"));
		});
		put( "while", c -> {
			Code last = dict.get(dict.size()-2);
			Code temp = dict.get(dict.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.get(last.pf.size()-1).stage=2; 
		});
		put( "repeat",c -> {
			Code last = dict.get(dict.size()-2);
			Code temp = dict.get(dict.size()-1);
			last.pf.get(last.pf.size()-1).pf1.addAll(temp.pf);
			dict.remove(dict.size()-1);
		});
		put( "again", c -> {
			Code last = dict.get(dict.size()-2);
			Code temp = dict.get(dict.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			last.pf.get(last.pf.size()-1).stage=1;
			dict.remove(dict.size()-1);
		});
		put( "until", c -> {
			Code last = dict.get(dict.size()-2);
			Code temp = dict.get(dict.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			dict.remove(dict.size()-1);
		});
		// for next
		put( "cycles", c -> {
			Code cc = (Code)c;
			int i=0;
			if (cc.stage==0) {
				while(true){
					for (var w:cc.pf) w.xt();
					i=rstack.pop();i--;
					if (i<0) break;
					rstack.push(i);
				}
			} 
			else if (cc.stage>0) {
				for (var w:cc.pf) w.xt();
				while(true){
					for (var w:cc.pf2) w.xt();
					i=rstack.pop();i--;
					if (i<0) break;
					rstack.push(i);
					for (var w:cc.pf1) w.xt();
				}
			}
		});
		put( "for",  c -> {
			Code last = dict.get(dict.size()-1);
			last.addCode(new Code(">r"));
			last.addCode(new Code("cycles"));
			dict.add(new Code("temp"));
		});
		put( "aft",  c -> {
			Code last = dict.get(dict.size()-2);
			Code temp = dict.get(dict.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.get(last.pf.size()-1).stage=3; 
		});
		put( "next", c -> {
			Code last = dict.get(dict.size()-2);
			Code temp = dict.get(dict.size()-1);
			if (last.pf.get(last.pf.size()-1).stage==0) {
				 last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			}
			else last.pf.get(last.pf.size()-1).pf2.addAll(temp.pf);
			dict.remove(dict.size()-1);
		});
		// defining words
		put( "exit", c -> { throw new ArithmeticException(); });								// marker to exit interpreter
		put( "exec", c -> { int n=stack.pop();dict.get(n).xt(); });
		put( ":",    c -> {          								// -- box
			String s = tok.nextToken();
			dict.add(new Code(s));
			Code last = dict.get(dict.size()-1);
			last.idx=fence++;
			vm_comp = true;
		});
		put( ";", c -> {          								
			Code last = dict.get(dict.size()-1);
			vm_comp = false;
		});
		put( "docon", c -> stack.push(((Code)c).qf.get(0))  );			// integer literal
		put( "dovar", c -> stack.push(((Code)c).idx)      	);			// string literals
		put( "create",c -> {
			String s = tok.nextToken();
			dict.add(new Code(s));
			Code last = dict.get(dict.size()-1);
			last.idx=fence++;
			last.addCode(new Code("dovar",0));
			last.pf.get(0).idx = last.idx;
			last.pf.get(0).qf.remove(0);
		});
		put( "variable", c -> {  
			String s = tok.nextToken();
			dict.add(new Code(s));
			Code last = dict.get(dict.size()-1);
			last.idx=fence++;
			last.addCode(new Code("dovar",0));
			last.pf.get(0).idx = last.idx;
		});
		put( "constant", c -> {   // n --
			String s = tok.nextToken();
			dict.add(new Code(s));
			Code last = dict.get(dict.size()-1);
			last.idx = fence++;
			last.addCode(new Code("docon",stack.pop()));
			last.pf.get(0).idx = last.idx;
		});
		put( "@",  c -> {   // w -- n
			Code last = dict.get(stack.pop());
			stack.push(last.pf.get(0).qf.get(0));
		});
		put( "!",  c -> {   // n w -- 
			Code last = dict.get(stack.pop());
			last.pf.get(0).qf.set(0,stack.pop());
		});
		put( "+!", c -> {   // n w -- 
			Code last = dict.get(stack.pop());
			int n=last.pf.get(0).qf.get(0); 
			n+= stack.pop();
			last.pf.get(0).qf.set(0,n);
		});
		put( "?",  c -> {   // w -- 
			Code last = dict.get(stack.pop());
			System.out.print(last.pf.get(0).qf.get(0));
		});
		put( "array@", c -> {   // w a -- n
			int a = stack.pop();
			Code last = dict.get(stack.pop());
			stack.push(last.pf.get(0).qf.get(a));
		});
		put( "array!", c -> {   // n w a -- 
			int a = stack.pop();
			Code last = dict.get(stack.pop());
			last.pf.get(0).qf.set(a,stack.pop());
		});
		put( ",",    c -> {  // n --
			Code last = dict.get(dict.size()-1);
			last.pf.get(0).qf.add(stack.pop());
		});
		put( "allot",c -> {   // n --
			int n = stack.pop(); 
			Code last = dict.get(dict.size()-1);
			for (int i=0;i<n;i++) last.pf.get(0).qf.add(0);
		});
		put( "does", c -> {  // n --
			Code last = dict.get(dict.size()-1);
			Code source = dict.get(wp);
			last.pf.addAll(source.pf.subList(ip+2,source.pf.size()));
		});
		put( "to",   c -> {   									// n -- , compile only 
			Code last = dict.get(wp);	ip++;		// current colon word
			last.pf.get(ip++).pf.get(0).qf.set(0,stack.pop());	// next constant
		});
		put( "is",   c -> {   									// w -- , execute only
			Code source = dict.get(stack.pop());	// source word
			String s = tok.nextToken(); boolean found=false;
			for (var w:dict) {
				if (s.equals(w.name)) { 				// target word
					Code target = dict.get(w.idx); 
					target.pf=source.pf; 				// copy pf 
					found = true;break;
				}
			}
			if (!found) System.out.print(s+" ?");
		});
		// tools
		put( "here",  c -> stack.push(fence) );
		put( "words", c -> { 
			int i=0;
			for (var w:dict) {
				System.out.print(w.name + " ");
				i++;
				if (i>15) {
					System.out.println();
					i=0;
				}
			}
		});
		put( ".s",    c -> { for (int n:stack) System.out.print(Integer.toString(n,base)+" "); });
		put( "see",   c -> { 
			String s = tok.nextToken(); 
			boolean found=false;
			for (var word:dict) {
				if (s.equals(word.name)) { 
					System.out.println(word.name+", "+word.idx+", "+word.qf.toString());
					for ( var w:word.pf) System.out.print(w.name+", "+w.idx+", "+w.qf.toString()+"| ");       
					found = true; 
					break;
				}
			}
			if (!found) System.out.print(s+" ?");
		});
		put( "time",  c -> { 
			LocalTime now = LocalTime.now();
			System.out.println(now); 
		});
		put( "ms",    c -> {  // n --
			try { Thread.sleep(stack.pop());} 
			catch (Exception e) { System.out.println(e); }
		});
		put( "bye",   c -> vm_run = false );
	}};
	
	public static void parser(String tib)
	{
		tok = new StringTokenizer(tib);
		
		String idiom = "";
		while (vm_run && tok.hasMoreTokens()) {
			idiom = tok.nextToken().trim();

			Code newWordObject = null;
			for (var w:dict) {  							// search dictionary
				if (w.name.equals(idiom)) {
					newWordObject = w;
					break;
				}
			}
			if(newWordObject != null) {  					// word found
				if(!vm_comp || newWordObject.immediate) {
					try { newWordObject.xt(); } 			// execute
					catch (Exception e) { System.out.print(e); }
				}
				else {  									// or compile
					Code latestWord = dict.get(dict.size()-1);
					latestWord.addCode(newWordObject);
				}
			}
			else { 
				try {
					int n=Integer.parseInt(idiom, base); 	// not word, try number
					if (vm_comp) {  						// compile integer literal
						Code latestWord = dict.get(dict.size()-1);
						latestWord.addCode(new Code("dolit",n));
					}
					else { stack.push(n);}
				}											// or push number on stack
				catch (NumberFormatException  ex) {			// catch number errors
					System.out.println(idiom + " ?");
					vm_comp = false; stack.clear();
				}
			}
		}
	}
	public static void setup_dic() {
		final String words[] = {
			":",    "dup",    "over", "4dup", "swap",  "rot",  "-rot", "2swap", "pick",   "roll", 
			"2dup", "2over",  "drop", "nip",  "2drop", ">r",   "r>",   "r@",    "+",      "-",    
			"*",    "/",      "mod",  "*/",   "*/mod", "and",  "or",   "xor",   "negate", "0=",   
			"0<",   "0>",     "=",    "<",    ">",     "<>",   ">=",   "<=",    "base@",  "base!",
			"hex",  "decimal","cr",   ".",    ".r",    "u.r",  "key",  "emit",  "space",  "spaces",
			"]",    "[",      "'",    "exit", "exec",  "create","variable","constant","@","!",    
			"+!",   "?",      "array@","array!",",",   "allot","does", "to",    "is",     "here", 
			"words",".s",     "see",  "time", "ms",    "bye"
		};
		final String immd[] = {
				";",    "$\"",    ".\"",  "(",    "\\",    "if",   "else", "then",  "begin",  "again",
				"until","while",  "repeat","for", "next",  "aft"
			};
		for (String w: words) { dict.add(new Code(w)); }
		for (String w: immd)  {	dict.add(new Code(w).immediate()); }
	}

	public static void main(String args[]) {	// ooeforth 1.12
		System.out.println("ooeForth1.12");
		setup_dic();

		// outer interpreter
		Scanner in = new Scanner(System.in);
		while (vm_run) {
			String tib = in.nextLine();
			parser(tib);
			if (vm_comp) {
				System.out.print("> ");
			}
			else {
				System.out.println();
				for (int n:stack) System.out.print(Integer.toString(n,base)+" ");
				System.out.print("OK ");
			}
		}
		in.close();
		System.out.println("Thank you.");
	}
	
}

