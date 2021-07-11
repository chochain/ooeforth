import java.util.*;
import java.time.LocalTime;

public class Eforth_VM {
	public int      base = 10;
	public boolean  run  = true;
	public boolean  comp = false;
	
	Stack<Integer>  ss   = new Stack<>();
	Stack<Integer>  rs   = new Stack<>();
	Eforth_List<Eforth_Code> dict = new Eforth_List<Eforth_Code>();
	
	int wp, ip;

	StringTokenizer tok = null;
    
	interface XT<Eforth_Code> {
		void run(Eforth_Code c);
	}
	
	Eforth_VM() { _setup_dic(); } 
	
	public Eforth_Code find(String str) {
		Iterator<Eforth_Code> i = dict.iterator();
		while (i.hasNext()) {
			Eforth_Code w = i.next();
			if (str.equals(w.name)) return w;
		}
		return null;
	}
	public void useTok(StringTokenizer tok0) { tok = tok0; }
    public void colon_add(Eforth_Code w)     { dict.tail().pf.add(w); }		// add to new word
    public void ss_push(Integer n)      	 { ss.push(n); }
    public void ss_dump() {
		if (comp) {
			System.out.print("> ");
		}
		else {
			System.out.println();
			for (int n:ss) System.out.print(Integer.toString(n, base)+" ");
			System.out.print("OK ");
		}
    }
    public void xt(Eforth_Code w) {
        if (_vtable.containsKey(w.name)) {	// primitives
            _vtable.get(w.name).run(w);
        } 
        else run_inner(w);					// colon words
    }
	public void run_inner(Eforth_Code colon_w) {	// 
        rs.push(wp); 
        rs.push(ip);
        wp = colon_w.idx;       			// wp points to current colon object
        ip = 0;	            
        for (Eforth_Code w:colon_w.pf) {	// inner interpreter
            try   { 
            	xt(w); 
            	ip++; 
            } 
            catch (ArithmeticException e) {}
        }
        ip = rs.pop(); 
        wp = rs.pop();
	}
	
	private void _setup_dic() {
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
        //for (String s: words) { dict.add(new Eforth_Code(s)); 				}
        //
        // double check whether we have them all listed
        //
        //lookUp.forEach((k, v) -> {
        //	Eforth_Code w = find(dict, k);
        //	if (w==null) System.out.println("not found:"+k);
        //});
        //
        // double check whether we have them all listed
        //
        _vtable.forEach( (k, v) -> dict.add(new Eforth_Code(k)) );
        
		final String immd[] = {
			"if",    "else",  "then",
			"begin", "again", "until", "while", "repeat", 
			"for",   "next",  "aft",
			";",    "$\"",    ".\"",  "(",    "\\"    
		};
        for (String s: immd)  {	
        	//dict.add(new Eforth_Code(s).immediate());
        	find(s).immediate();
        }
        for (Eforth_Code w:dict) {
        	System.out.println(w.name+"=>"+w.idx);
        }
	}
	
	Hashtable<String, XT<Eforth_Code>> _vtable = new Hashtable<>() {{
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
		// extra rstack
		put( "push",  c -> rs.push(ss.pop())  				);
		put( "pop",   c -> ss.push(rs.pop())  				);
		// extra stack
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
		// binary
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
		put( "[",     c -> comp = false	);
		put( "]",     c -> comp = true	);
		put( "'",     c -> { 
			Eforth_Code  w = find(tok.nextToken());
			ss.push(w==null ? -1 : w.idx);
		});
		
		put( "dolit", c -> ss.push(((Eforth_Code)c).qf.head()) );			// integer literal
		put( "dostr", c -> ss.push(((Eforth_Code)c).idx)		);			// string literals
		put( "$\"",   c -> {  	// -- w a
			Eforth_Code last = dict.tail();
			ss.push(last.idx);
			ss.push(last.pf.size());
			
			String s = tok.nextToken("\"");
			colon_add(new Eforth_Code("dostr", s));							// literal=s
			tok.nextToken();
		});
		put( "dotstr",c -> System.out.print(((Eforth_Code)c).literal)		);
		put( ".\"",   c -> {
			String s = tok.nextToken("\"");
			colon_add(new Eforth_Code("dotstr", s));						// literal=s
			tok.nextToken(" ");
		});
		put( "(",     c -> {
			tok.nextToken("\\)");
			tok.nextToken(" ");
		});
		put( ".(",    c -> {
			System.out.print(tok.nextToken("\\)"));
			tok.nextToken(" ");
		});
		put( "\\",    c -> tok.nextToken("\n")						);

		// structure: if else then
		put( "branch",c -> {
			for (var w: (ss.pop()!=0 ? ((Eforth_Code)c).pf : ((Eforth_Code)c).pf1)) xt(w);
		});
		put( "if",    c -> { 
			Eforth_Code   last = dict.tail();
			colon_add(new Eforth_Code("branch"));						// literal=s
			dict.add(new Eforth_Code("temp"));
		});
		put( "else",  c -> {
			Eforth_Code last = dict.tail(2);
			Eforth_Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.tail().stage=1; 
		});
		put( "then",  c -> {
			Eforth_Code last = dict.tail(2);
			Eforth_Code temp = dict.tail();
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
			Eforth_Code cc = (Eforth_Code)c;
			if (cc.stage==1) {	// again
				while(true) { for (var w:cc.pf) xt(w); }
			}
			if (cc.stage==2) {	// while repeat
				while (true) {
					for (var w:cc.pf) xt(w);
					if (ss.pop()==0) break;
					for (var w:cc.pf1) xt(w);
				}
			} 
			else {
				while(true) {	// until
					for (var w:cc.pf) xt(w);
					if (ss.pop()!=0) break;
				}
			}
		});
		put( "begin", c -> { 
			colon_add(new Eforth_Code("loops"));
			dict.add(new Eforth_Code("temp"));
		});
		put( "while", c -> {
			Eforth_Code last = dict.tail(2);
			Eforth_Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.tail().stage=2; 
		});
		put( "repeat",c -> {
			Eforth_Code last = dict.tail(2);
			Eforth_Code temp = dict.tail();
			last.pf.tail().pf1.addAll(temp.pf);
			dict.drop_tail();
		});
		put( "again", c -> {
			Eforth_Code last = dict.tail(2);
			Eforth_Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			last.pf.tail().stage=1;
			dict.drop_tail();
		});
		put( "until", c -> {
			Eforth_Code last = dict.tail(2);
			Eforth_Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			dict.drop_tail();
		});
		// for next
		put( "cycles", c -> {
			Eforth_Code cc = (Eforth_Code)c;
			int i=0;
			if (cc.stage==0) {
				while(true){
					for (var w:cc.pf) xt(w);
					i=rs.pop();i--;
					if (i<0) break;
					rs.push(i);
				}
			} 
			else {
				for (var w:cc.pf) xt(w);
				while (true) {
					for (var w:cc.pf2) xt(w);
					i = rs.pop();
					if (--i<0) break;
					rs.push(i);
					for (var w:cc.pf1) xt(w);
				}
			}
		});
		put( "for",  c -> {
			Eforth_Code last = dict.tail();
			colon_add(new Eforth_Code(">r"));
			colon_add(new Eforth_Code("cycles"));
			dict.add(new Eforth_Code("temp"));
		});
		put( "aft",  c -> {
			Eforth_Code last = dict.tail(2);
			Eforth_Code temp = dict.tail();
			last.pf.tail().pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.tail().stage=3; 
		});
		put( "next", c -> {
			Eforth_Code last = dict.tail(2);
			Eforth_Code temp = dict.tail();
			if (last.pf.tail().stage==0) {
				 last.pf.tail().pf.addAll(temp.pf);
			}
			else last.pf.tail().pf2.addAll(temp.pf);
			dict.drop_tail();
		});
		// defining words
		put( "exit", c -> { throw new ArithmeticException(); });								// marker to exit interpreter
		put( "exec", c -> { int n=ss.pop(); xt(dict.get(n)); });
		put( ":",    c -> {          								// -- box
			dict.add(new Eforth_Code(tok.nextToken()));
			comp = true;
		});
		put( ";", c -> comp = false );
		put( "docon", c -> ss.push(((Eforth_Code)c).qf.head()) );			// integer literal
		put( "dovar", c -> ss.push(((Eforth_Code)c).idx)       );			// string literals
		put( "create",c -> {
			dict.add(new Eforth_Code(tok.nextToken()));
			Eforth_Code last = dict.tail();
			colon_add(new Eforth_Code("dovar",0));
			last.pf.head().idx = last.idx;
			last.pf.head().qf.drop_head();
		});
		put( "variable", c -> {  
			dict.add(new Eforth_Code(tok.nextToken()));
			Eforth_Code last = dict.tail();
			colon_add(new Eforth_Code("dovar",0));
			last.pf.head().idx = last.idx;
		});
		put( "constant", c -> {   // n --
			dict.add(new Eforth_Code(tok.nextToken()));
			Eforth_Code last = dict.tail();
			colon_add(new Eforth_Code("docon",ss.pop()));
			last.pf.head().idx = last.idx;
		});
		put( "@",  c -> {   // w -- n
			Eforth_Code last = dict.get(ss.pop());
			ss.push(last.pf.head().qf.head());
		});
		put( "!",  c -> {   // n w -- 
			Eforth_Code last = dict.get(ss.pop());
			last.pf.head().qf.set_head(ss.pop());
		});
		put( "+!", c -> {   // n w -- 
			Eforth_Code last = dict.get(ss.pop());
			int n = last.pf.head().qf.head(); 
			n+= ss.pop();
			last.pf.head().qf.set_head(n);
		});
		put( "?",  c -> {   // w -- 
			Eforth_Code last = dict.get(ss.pop());
			System.out.print(last.pf.head().qf.head());
		});
		put( "array@", c -> {   // w a -- n
			int a = ss.pop();
			Eforth_Code last = dict.get(ss.pop());
			ss.push(last.pf.head().qf.get(a));
		});
		put( "array!", c -> {   // n w a -- 
			int a = ss.pop();
			Eforth_Code last = dict.get(ss.pop());
			last.pf.head().qf.set(a, ss.pop());
		});
		put( ",",    c -> {  // n --
			Eforth_Code last = dict.tail();
			last.pf.head().qf.add(ss.pop());
		});
		put( "allot",c -> {   // n --
			int n = ss.pop(); 
			Eforth_Code last = dict.tail();
			for (int i=0;i<n;i++) last.pf.head().qf.add(0);
		});
		put( "does", c -> {  // n --
			Eforth_Code last = dict.tail();
			Eforth_Code src  = dict.get(wp);
			last.pf.addAll(src.pf.subList(ip+2, src.pf.size()));
		});
		put( "to",   c -> {   										// n -- , compile only 
			Eforth_Code last = dict.get(wp);
			ip++;													// current colon word
			last.pf.get(ip++).pf.head().qf.set_head(ss.pop());		// next constant
		});
		put( "is",   c -> {   										// w -- , execute only
			Eforth_Code   src = dict.get(ss.pop());						// source word
			String s   = tok.nextToken(); 
			Eforth_Code   w   = find(s);

			if (w==null) System.out.print(s+" ?");
			else {
				dict.get(w.idx).pf = src.pf; 
			}
		});
		// tools
		put( "here",  c -> ss.push(((Eforth_Code)c).fence) );
		put( "words", c -> { 
			int i=0;
			for (Eforth_Code w:dict) {
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
			Eforth_Code   w = find(s);
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
		put( "bye",   c -> run = false );
	}};
}
