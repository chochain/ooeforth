import java.util.*;
import java.io.*;
import java.time.LocalTime;

public class Eforth112 {	// ooeforth
	static Stack<Integer>  ss   = new Stack<>();
	static Stack<Integer>  rs   = new Stack<>();
	static MyList<Code>    dict = new MyList<Code>();
	static StringTokenizer tok  = null;

	static boolean  vm_comp = false;
	static boolean  vm_run  = true;
	static int 		base    = 10;
	static int 		wp, ip;

	interface XT<Code> {
		void run(Code c);
	}
	static class MyList<Code> extends ArrayList<Code> {
		Code head()			  { return get(0);               }
		Code tail()           { return get(size() - 1);      }
		Code tail(int offset) { return get(size() - offset); }
		void set_head(Code w) { set(0, w);                   }
		void drop_head()      { remove(0);                   }
		void drop_tail()      { remove(size()-1);            }
	}
	// primitive words
	static class Code {
		static int      fence     = 0;
		
		public String 	name;
		public int 		idx       = 0;
		public boolean 	immd      = false;
		public int      stage     = 0;
		public String 	literal;

		public MyList<Code>  pf   = new MyList<>();
		public MyList<Code>  pf1  = new MyList<>();
		public MyList<Code>  pf2  = new MyList<>();
		public MyList<Integer> qf = new MyList<>();

		public Code(String n) {
			name = n;
			immd = false;
			idx  = fence++;
		}
		public Code(String n, int d)    { name=n;  qf.add(d); }
		public Code(String n, String l) { name=n;  literal=l; }

		public Code immediate()         { immd=true; return this; }
		public void xt() {
			if (lookUp.containsKey(name)) {
				lookUp.get(name).run(this);
			} 
			else { 
				rs.push(wp); 
				rs.push(ip);
				wp = idx;       // wp points to current colon object
				ip = 0;	            
				for (Code w:pf) {
					try   { w.xt(); ip++; } 
					catch (ArithmeticException e) {}
				}
				ip = rs.pop(); 
				wp = rs.pop();
			}
		}
		public void addCode(Code w) { this.pf.add(w); }
	}
	static Hashtable<String, XT<Code>> lookUp = new Hashtable<>() {{
		// stacks
		put( "dup",   c -> ss.push(ss.peek()) 				);
		put( "over",  c -> ss.push(ss.get(ss.size()-2)) 	);
		put( "swap",  c -> ss.add(ss.size()-2,ss.pop()) 	);
		put( "rot",   c -> ss.push(ss.remove(ss.size()-3))  );
		put( "drop",  c -> ss.pop() 						);
		put( "nip",   c -> ss.remove(ss.size()-2)  			);
		put( "2drop", c -> { ss.pop(); ss.pop(); } 			);
		put( ">r",    c -> rs.push(ss.pop())  				);
		put( "r>",    c -> ss.push(rs.pop())  				);
		put( "r@",    c -> ss.push(rs.peek()) 				);
		put( "push",  c -> rs.push(ss.pop())  				);
		put( "pop",   c -> ss.push(rs.pop())  				);
		put( "2dup",  c -> ss.addAll(ss.subList(ss.size()-2, ss.size())) 	);
		put( "2over", c -> ss.addAll(ss.subList(ss.size()-4, ss.size()-2)) 	);
		put( "4dup",  c -> ss.addAll(ss.subList(ss.size()-4, ss.size())) 	);
		put( "-rot",  c -> { ss.push(ss.remove(ss.size()-3)); ss.push(ss.remove(ss.size()-3)); });
		put( "2swap", c -> { ss.push(ss.remove(ss.size()-4)); ss.push(ss.remove(ss.size()-4)); });
		put( "pick",  c -> { int i=ss.pop(); int n=ss.get(ss.size()-i-1);    ss.push(n);       });
		put( "roll",  c -> { int i=ss.pop(); int n=ss.remove(ss.size()-i-1); ss.push(n);       });
		// math
		put( "+",     c -> ss.push(ss.pop()+ss.pop()) );
		put( "*",     c -> ss.push(ss.pop()*ss.pop()) );
		put( "-",     c -> { int n= ss.pop(); ss.push(ss.pop()-n);          });
		put( "/",     c -> { int n= ss.pop(); ss.push(ss.pop()/n);          });
		put( "*/",    c -> { int n=ss.pop();  ss.push(ss.pop()*ss.pop()/n); });
		put( "*/mod", c -> { 
			int n=ss.pop();
			int m=ss.pop()*ss.pop();
			ss.push(m%n);
			ss.push(m/n);
		});
		put( "mod",   c -> { int n= ss.pop(); ss.push(ss.pop()%n); });
		put( "and",   c -> ss.push(ss.pop()&ss.pop()) 	);
		put( "or",    c -> ss.push(ss.pop()|ss.pop()) 	);
		put( "xor",   c -> ss.push(ss.pop()^ss.pop()) 	);
		put( "negate",c -> ss.push(-ss.pop())           );
		// logic
		put( "0=",    c -> ss.push((ss.pop()==0)?-1:0)  );
		put( "0<",    c -> ss.push((ss.pop()< 0)?-1:0)	);
		put( "0>",    c -> ss.push((ss.pop()> 0)?-1:0)	);
		put( "=",     c -> { int n= ss.pop(); ss.push((ss.pop()==n)?-1:0); });
		put( ">",     c -> { int n= ss.pop(); ss.push((ss.pop()>n )?-1:0); });
		put( "<",     c -> { int n= ss.pop(); ss.push((ss.pop()<n )?-1:0); });
		put( "<>",    c -> { int n= ss.pop(); ss.push((ss.pop()!=n)?-1:0); });
		put( ">=",    c -> { int n= ss.pop(); ss.push((ss.pop()>=n)?-1:0); });
		put( "<=",    c -> { int n= ss.pop(); ss.push((ss.pop()<=n)?-1:0); });
		// output
		put( "base@", c -> ss.push(base)		);
		put( "base!", c -> base = ss.pop()		);
		put( "hex",   c -> base = 16			);
		put( "decimal",c-> base = 10			);
		put( "cr",    c -> System.out.println()	);
		put( ".",     c -> System.out.print(Integer.toString(ss.pop(), base)+" ") );
		put( ".r",    c -> { 
			int n=ss.pop();
			String s=Integer.toString(ss.pop(), base);
			for (int i=0; i+s.length()<n; i++) System.out.print(" ");
			System.out.print(s+" ");
		});
		put( "u.r",   c -> {
			int n=ss.pop();
			String s=Integer.toString(ss.pop()&0x7fffffff, base);
			for (int i=0;i+s.length()<n; i++) System.out.print(" ");
			System.out.print(s+" ");
		});
		put( "key",   c -> ss.push((int)tok.nextToken().charAt(0)) 			);
		put( "emit",  c -> System.out.print(Character.toChars(ss.pop()))	);
		put( "space", c -> System.out.print(" ") );
		put( "spaces",c -> {
			int n=ss.pop();
			for (int i=0; i<n; i++) System.out.print(" ");
		});
		// literals
		put( "[",     c -> vm_comp = false	);
		put( "]",     c -> vm_comp = true	);
		put( "'",     c -> { 
			Code  w = find(dict, tok.nextToken());
			ss.push(w==null ? -1 : w.idx);
		});
		
		put( "dolit", c -> ss.push(((Code)c).qf.head()) );			// integer literal
		put( "dostr", c -> ss.push(((Code)c).idx)		);			// string literals
		put( "$\"",   c -> {  // -- w a
			String s    = tok.nextToken("\"");
			Code   last = dict.tail();
			last.addCode(new Code("dostr", s));						// literal=s
			tok.nextToken();
			ss.push(last.idx);
			ss.push(last.pf.size()-1);
		});
		put( "dotstr",c -> System.out.print(((Code)c).literal)		);
		put( ".\"",   c -> {
			String s = tok.nextToken("\"");
			dict.tail().addCode(new Code("dotstr", s));				// literal=s
		});
		put( "(",     c -> tok.nextToken("\\)")						);
		put( ".(",    c -> System.out.print(tok.nextToken("\\)"))	);
		put( "\\",    c -> tok.nextToken("\n")						);

		// structure: if else then
		put( "branch",c -> {
			for (var w: (ss.pop()!=0 ? ((Code)c).pf : ((Code)c).pf1)) w.xt();
		});
		put( "if",    c -> { 
			dict.tail().addCode(new Code("branch"));
			dict.add(new Code("temp"));
		});
		put( "else",  c -> {
			Code last = dict.tail(2);
			Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.tail().stage=1; 
		});
		put( "then",  c -> {
			Code last = dict.tail(2);
			Code temp = dict.tail();
			if (last.pf.tail().stage==0) {
				last.pf.tail().pf.addAll(temp.pf);
				dict.drop_tail();
			} 
			else {
				last.pf.tail().pf1.addAll(temp.pf);
				if (last.pf.tail().stage==1) {
					dict.drop_tail();
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
					if (ss.pop()==0) break;
					for (var w:cc.pf1) w.xt();
				}
			} 
			else {
				while(true) {	// until
					for (var w:cc.pf) w.xt();
					if(ss.pop()!=0) break;
				}
			}
		});
		put( "begin", c -> { 
			dict.tail().addCode(new Code("loops"));
			dict.add(new Code("temp"));
		});
		put( "while", c -> {
			Code last = dict.tail(2);
			Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.tail().stage=2; 
		});
		put( "repeat",c -> {
			Code last = dict.tail(2);
			Code temp = dict.tail();
			last.pf.tail().pf1.addAll(temp.pf);
			dict.drop_tail();
		});
		put( "again", c -> {
			Code last = dict.tail(2);
			Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			last.pf.tail().stage=1;
			dict.drop_tail();
		});
		put( "until", c -> {
			Code last = dict.tail(2);
			Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			dict.drop_tail();
		});
		// for next
		put( "cycles", c -> {
			Code cc = (Code)c;
			int i=0;
			if (cc.stage==0) {
				while(true){
					for (var w:cc.pf) w.xt();
					i=rs.pop();i--;
					if (i<0) break;
					rs.push(i);
				}
			} 
			else {
				for (var w:cc.pf) w.xt();
				while (true) {
					for (var w:cc.pf2) w.xt();
					i = rs.pop();
					if (--i<0) break;
					rs.push(i);
					for (var w:cc.pf1) w.xt();
				}
			}
		});
		put( "for",  c -> {
			Code last = dict.tail();
			last.addCode(new Code(">r"));
			last.addCode(new Code("cycles"));
			dict.add(new Code("temp"));
		});
		put( "aft",  c -> {
			Code last = dict.tail(2);
			Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.tail().stage=3; 
		});
		put( "next", c -> {
			Code last = dict.tail(2);
			Code temp = dict.tail();
			if (last.pf.tail().stage==0) {
				 last.pf.tail().pf.addAll(temp.pf);
			}
			else last.pf.tail().pf2.addAll(temp.pf);
			dict.drop_tail();
		});
		// defining words
		put( "exit", c -> { throw new ArithmeticException();  });								// marker to exit interpreter
		put( "exec", c -> { int n=ss.pop(); dict.get(n).xt(); });
		put( ":",    c -> {          								// -- box
			dict.add(new Code(tok.nextToken()));
			vm_comp = true;
		});
		put( ";", c -> vm_comp = false );
		put( "docon", c -> ss.push(((Code)c).qf.head()) );			// integer literal
		put( "dovar", c -> ss.push(((Code)c).idx)      	);			// string literals
		put( "create",c -> {
			dict.add(new Code(tok.nextToken()));
			Code last = dict.tail();
			last.addCode(new Code("dovar",0));
			last.pf.head().idx = last.idx;
			last.pf.head().qf.drop_head();
		});
		put( "variable", c -> {  
			dict.add(new Code(tok.nextToken()));
			Code last = dict.tail();
			last.addCode(new Code("dovar",0));
			last.pf.head().idx = last.idx;
		});
		put( "constant", c -> {   // n --
			dict.add(new Code(tok.nextToken()));
			Code last = dict.tail();
			last.addCode(new Code("docon",ss.pop()));
			last.pf.head().idx = last.idx;
		});
		put( "@",  c -> {   // w -- n
			Code last = dict.get(ss.pop());
			ss.push(last.pf.head().qf.head());
		});
		put( "!",  c -> {   // n w -- 
			Code last = dict.get(ss.pop());
			last.pf.head().qf.set_head(ss.pop());
		});
		put( "+!", c -> {   // n w -- 
			Code last = dict.get(ss.pop());
			int n = last.pf.head().qf.head(); 
			n+= ss.pop();
			last.pf.head().qf.set_head(n);
		});
		put( "?",  c -> {   // w -- 
			Code last = dict.get(ss.pop());
			System.out.print(last.pf.head().qf.head());
		});
		put( "array@", c -> {   // w a -- n
			int a = ss.pop();
			Code last = dict.get(ss.pop());
			ss.push(last.pf.head().qf.get(a));
		});
		put( "array!", c -> {   // n w a -- 
			int a = ss.pop();
			Code last = dict.get(ss.pop());
			last.pf.head().qf.set(a, ss.pop());
		});
		put( ",",    c -> {  // n --
			Code last = dict.tail();
			last.pf.head().qf.add(ss.pop());
		});
		put( "allot",c -> {   // n --
			int n = ss.pop(); 
			Code last = dict.tail();
			for (int i=0;i<n;i++) last.pf.head().qf.add(0);
		});
		put( "does", c -> {  // n --
			Code last = dict.tail();
			Code src  = dict.get(wp);
			last.pf.addAll(src.pf.subList(ip+2, src.pf.size()));
		});
		put( "to",   c -> {   										// n -- , compile only 
			Code last = dict.get(wp);
			ip++;													// current colon word
			last.pf.get(ip++).pf.head().qf.set_head(ss.pop());		// next constant
		});
		put( "is",   c -> {   										// w -- , execute only
			Code   src = dict.get(ss.pop());						// source word
			String s   = tok.nextToken(); 
			Code   w   = find(dict, s);

			if (w==null) System.out.print(s+" ?");
			else {
				dict.get(w.idx).pf = src.pf; 
			}
		});
		// tools
		put( "here",  c -> ss.push(((Code)c).fence) );
		put( "words", c -> { 
			int i=0;
			for (Code w:dict) {
				System.out.print(w.name + " ");
				if (++i>15) {
					System.out.println();
					i=0;
				}
			}
		});
		put( ".s",    c -> { for (int n:ss) System.out.print(Integer.toString(n,base)+" "); });
		put( "see",   c -> { 
			String s = tok.nextToken();
			Code   w = find(dict, s);
			if (w==null) System.out.print(s+" ?");
			else {
				System.out.println(w.name+", "+w.idx+", "+w.qf.toString());
				for (var p: w.pf) System.out.print(p.name+", "+p.idx+", "+p.qf.toString()+"| ");       
			}
		});
		put( "time",  c -> { 
			LocalTime now = LocalTime.now();
			System.out.println(now); 
		});
		put( "ms",    c -> {  // n --
			try { Thread.sleep(ss.pop());} 
			catch (Exception e) { System.out.println(e); }
		});
		put( "bye",   c -> vm_run = false );
	}};
	//
	// static functions
	//
	public static Code find(MyList<Code> lst, String str) {
		Iterator<Code> i = lst.iterator();
		while (i.hasNext()) {
			Code w = i.next();
			if (str.equals(w.name)) return w;
		}
		return null;
	}
	
	public static void setup_dic() {
		//
		//final String words[] = {
		//	//--------+--------+--------+--------+--------+--------+--------+--------+--------+--------
        //    "dup",	  "over",  "swap",  "rot",   "drop",  "nip",   "2drop", ">r",    "r>",    "r@",
        //    "push",   "pop",   "2dup",  "2over", "4dup",  "-rot",  "2swap", "pick",  "roll",  "+",
        //    "-",      "*",     "/",     "*/",    "*/mod", "mod",   "and",   "or",    "xor",   "negate",
        //    "0=",     "0<",    "0>",    "=",     ">",     "<",     "<>",    ">=",    "<=",    "base@",
        //    "base!",  "hex",   "decimal","cr",   ".",     ".r",    "u.r",   "key",   "emit",  "space",
        //    "spaces", "[",     "]",     "'",     "dolit", "dostr", "$\"",   "dotstr",".\"",   "repeat",
        //    "(",      ".(",    "\\",    "branch","if",    "else",  "then",  "loops", "begin", "while",
        //    "again",  "until", "cycles","for",   "aft",   "next",  "exit",  "exec",  ":",     ";",
        //    "docon",  "dovar", "create","variable","constant","@", "!",     "+!",    "?",     "array@",
        //    "array!", ",",     "allot", "does",  "to",    "is",    "here",  "words", ".s",    "see",
        //    "time",   "ms",    "bye"
		//};
        //for (String s: words) { dict.add(new Code(s)); 				}
        //
        // double check whether we have them all listed
        //
        //lookUp.forEach((k, v) -> {
        //	Code w = find(dict, k);
        //	if (w==null) System.out.println("not found:"+k);
        //});
        //
        // double check whether we have them all listed
        //
        lookUp.forEach( (k, v) -> dict.add(new Code(k)) );
        
		final String immd[] = {
			"if",    "else",  "then",
			"begin", "again", "until", "while", "repeat", 
			"for",   "next",  "aft",
			";",    "$\"",    ".\"",  "(",    "\\"    
		};
        for (String s: immd)  {	
        	//dict.add(new Code(s).immediate());
        	find(dict, s).immediate();
        }
        for (Code w:dict) {
        	System.out.println(w.name+"=>"+w.idx);
        }
	}

	public static void parser(String tib)
	{
		tok = new StringTokenizer(tib);
		
		String idiom = "";
		while (vm_run && tok.hasMoreTokens()) {
			idiom = tok.nextToken().trim();

			Code w = find(dict, idiom);					// search dictionary
			if(w != null) {  							// word found
				if(!vm_comp || w.immd) {
					try { w.xt(); } 						// execute
					catch (Exception e) { System.out.print(e); }
				}
				else {  									// or compile
					dict.tail().addCode(w);
				}
			}
			else { 
				try {
					int n=Integer.parseInt(idiom, base); 	// not word, try number
					if (vm_comp) {  						// compile integer literal
						dict.tail().addCode(new Code("dolit",n));
					}
					else { ss.push(n);}
				}											// or push number on stack
				catch (NumberFormatException  ex) {			// catch number errors
					System.out.println(idiom + " ?");
					vm_comp = false; ss.clear();
				}
			}
		}
	}

	public static void main(String args[]) {	// ooeforth 1.12
		System.out.println("ooeForth1.12");
		new AwtTest();  
		/*
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
				for (int n:ss) System.out.print(Integer.toString(n,base)+" ");
				System.out.print("OK ");
			}
		}
		in.close();
		*/
		System.out.println("Thank you.");
	}
	
}

