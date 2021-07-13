import java.util.*;
import java.io.*;
import java.time.LocalTime;
import java.util.function.*;

public class EforthVM {
	//
	// Forth stacks and dictionary
	//
	Stack<Integer>      ss   = new Stack<>();
	Stack<Integer>  	rs   = new Stack<>();
	EforthList<EforthCode>  dict = new EforthList<>();
	//
	// console input/output
	//
	StringTokenizer			 tok  = null;
	PrintWriter 			 out  = new PrintWriter(System.out, true);
	//
	// Forth internal variables
	//
	int     base   = 10;
	boolean run    = true;
	boolean comp   = false;
	int 	wp, ip;
    //
	// functional interfaces
	//
	EforthVM() { _setup_dict(); } 

	public void setOutput(PrintWriter out0) { out = out0; }
	/**
	 * find - Forth dictionary search 
	 *
	 * @param  str  input string to be search against dictionary words
	 * @return      EforthCode found; null - if not found
	 */
	private EforthCode find(String str) {
		Iterator<EforthCode> i = dict.iterator();			///> iterator sequential search
		while (i.hasNext()) {
			var w = i.next();
			if (str.equals(w.name)) return w;				///> * return Code if found
		}
		return null;										///> * return null if not found
	}
	/**
	 * xt - Forth execution unit, execute a given Code
	 *
	 * @param w Code to be executed
	 */
    public void xt(EforthCode w) {							// execution unit
        if (_vtable.containsKey(w.name)) {					///> primitives - execute lambda, or
            _vtable.get(w.name).accept(w);
        } 
        else _run_inner(w);									///> a colon word - run inner interpreter
    }
    /**
     * ok - stack dump and OK prompt
     */
    public boolean ok() {
		if (comp) {											///> if it's in compile mode
			out.print("> ");								///> * print > continue prompt
		}
		else {												///> in interpreter mode
			out.println();									///> * dump stack contents, and
			for (int n:ss) out.print(Integer.toString(n, base)+" ");
			out.print("OK ");								///> * OK prompt
		}
		return run;
    }
    /**
     * Forth outer interpreter - process one line a time
     *
     * @param tib - input string to be parsed
     */
	public void parse(String tib)							// outer interpreter (one line a time)
	{
		tok = new StringTokenizer(tib);						///> create tokenizer
		
		while (run && tok.hasMoreTokens()) {
			String idiom = tok.nextToken().trim();			///> fetch next token

			EforthCode w = find(idiom);					///> search dictionary
			if (w != null) {  								///> if word found
				if (!comp || w.immd) {						///> * check whether in immediate mode 
					try 				{ xt(w); 		}	///>> execute immediately
					catch (Exception e) { out.print(e); }	// just-in-case it failed
				}
				else _colon_add(w);							///> * add to dictionary if in compile mode
			}
			else { 											///> when word not found
				try {
					int n=Integer.parseInt(idiom, base);    ///> * try process as a number
					if (comp) {  							///>> in compile mode 
						_colon_add(
							new EforthCode("dolit", n));	///>> append literal to latest defined word
					}
					else _ss_push(n);						///>> or, add number to top of stack
				}											
				catch (NumberFormatException  ex) {			///> if it's not a number
					out.println(idiom + " ?");				///> * show not found sign
					comp = false; 
				}
			}
		}
	}
	//
	// private methods
	//
    private void _colon_add(EforthCode w) 	{ dict.add(w); }	// add to new word
    private void _ss_push(Integer n)    { ss.push(n);  }	// add number to top of stack
    private void _run_inner(EforthCode colon_w) {				///> inner interpreter
        rs.push(wp); 
        rs.push(ip);
        wp = colon_w.idx;       					// wp points to current colon object
        ip = 0;	            
        for (var w:colon_w.pf) {					// inner interpreter
            try { 
            	xt(w); 
            	ip++; 
            } 
            catch (ArithmeticException e) {}
        }
        ip = rs.pop(); 
        wp = rs.pop();
	}
    //
    // stack function operators
    //
    public void op1(Function<Integer, Integer> m) { 
    	int n=ss.pop(); ss.push(m.apply(n));           
    }
    public void op2(BiFunction<Integer, Integer, Integer> m) { 
    	int n=ss.pop(); ss.push(m.apply(ss.pop(), n)); 
    }
    /**
     * create dictionary with given word list
     */
    private void _setup_dict0() {
		final String words[] = {
			//--------+--------+--------+--------+--------+--------+--------+--------+--------+--------
            "dup",	  "over",  "swap",  "rot",   "drop",  "nip",   "2drop", ">r",    "r>",    "r@",
            "push",   "pop",   "2dup",  "2over", "4dup",  "-rot",  "2swap", "pick",  "roll",  "+",
            "-",      "*",     "/",     "*/",    "*/mod", "mod",   "and",   "or",    "xor",   "negate",
            "0=",     "0<",    "0>",    "=",     ">",     "<",     "<>",    ">=",    "<=",    "base@",
            "base!",  "hex",   "decimal","cr",   ".",     ".r",    "u.r",   "key",   "emit",  "space",
            "spaces", "[",     "]",     "'",     "dolit", "dostr", "$\"",   "dotstr",".\"",   "repeat",
            "(",      ".(",    "\\",    "branch","if",    "else",  "then",  "loops", "begin", "while",
            "again",  "until", "cycles","for",   "aft",   "next",  "exit",  "exec",  ":",     ";",
            "docon",  "dovar", "create","variable","constant","@", "!",     "+!",    "?",     "array@",
            "array!", ",",     "allot", "does",  "to",    "is",    "here",  "words", ".s",    "see",
            "time",   "ms",    "bye"
		};
        for (String s: words) { dict.add(new EforthCode(s)); 				}
        //
        // double check whether we have them all listed
        //
        _vtable.forEach((k, v) -> {
        	EforthCode w = find(k);
        	if (w==null) out.println("not found:"+k);
        });
    }
	private void _setup_dict() {
		// _setup_dict0();
        _vtable.forEach((k, v) -> dict.add(new EforthCode(k)));	// create primitive words
        
		final String immd[] = {
			"if",    "else",  "then",
			"begin", "again", "until", "while", "repeat", 
			"for",   "next",  "aft",
			";",    "$\"",    ".\"",  "(",    "\\"    
		};
        for (String s: immd)  {	
        	//dict.add(new EforthCode(s).immediate());
        	find(s).immediate();
        }
	}
	final Hashtable<String, Consumer<EforthCode>> _vtable = new Hashtable<>() {{
		// stacks
		put( "dup",   c -> ss.push(ss.peek()) 				);
		put( "over",  c -> ss.push(ss.get(ss.size()-2)) 	);
		put( "swap",  c -> ss.add(ss.size()-2,ss.pop()) 	);
		put( "rot",   c -> ss.push(ss.remove(ss.size()-3))  );
		put( "drop",  c -> ss.pop() 						);
		put( "nip",   c -> ss.remove(ss.size()-2)  			);
		put( ">r",    c -> rs.push(ss.pop())  				);
		put( "r>",    c -> ss.push(rs.pop())  				);
		put( "r@",    c -> ss.push(rs.peek()) 				);
		// extra rstack
		put( "push",  c -> rs.push(ss.pop())  				);
		put( "pop",   c -> ss.push(rs.pop())  				);
		// extra stack
		put( "2drop", c -> { ss.pop(); ss.pop(); } 			);
		put( "2dup",  c -> ss.addAll(ss.subList(ss.size()-2, ss.size())) 	);
		put( "2over", c -> ss.addAll(ss.subList(ss.size()-4, ss.size()-2)) 	);
		put( "4dup",  c -> ss.addAll(ss.subList(ss.size()-4, ss.size())) 	);
		put( "-rot",  c -> { ss.push(ss.remove(ss.size()-3)); ss.push(ss.remove(ss.size()-3)); });
		put( "2swap", c -> { ss.push(ss.remove(ss.size()-4)); ss.push(ss.remove(ss.size()-4)); });
		put( "pick",  c -> { int i=ss.pop(); int n=ss.get(ss.size()-i-1);    ss.push(n);       });
		put( "roll",  c -> { int i=ss.pop(); int n=ss.remove(ss.size()-i-1); ss.push(n);       });
		// math
		put( "+",     c -> op2((a,b)->a+b));
		put( "*",     c -> op2((a,b)->a*b));
		put( "-",     c -> op2((a,b)->a-b));
		put( "/",     c -> op2((a,b)->a/b));
		put( "mod",   c -> op2((a,b)->a%b));
		put( "*/",    c -> { int n=ss.pop(); ss.push(ss.pop()*ss.pop()/n); });
		put( "*/mod", c -> { 
			int n=ss.pop();
			int m=ss.pop()*ss.pop();
			ss.push(m%n);
			ss.push(m/n);
		});
		// binary
		put( "and",   c -> op2((a,b)->a&b));
		put( "or",    c -> op2((a,b)->a|b));
		put( "xor",   c -> op2((a,b)->a^b));
		put( "negate",c -> op1((a)->-a));
		// logic
		put( "0=",    c -> op1(a->a==0?-1:0));
		put( "0<",    c -> op1(a->a <0?-1:0));
		put( "0>",    c -> op1(a->a >0?-1:0));
		put( "=",     c -> op2((a,b)->(a==b)?-1:0));
		put( ">",     c -> op2((a,b)->(a >b)?-1:0));
		put( "<",     c -> op2((a,b)->(a <b)?-1:0));
		put( "<>",    c -> op2((a,b)->(a!=b)?-1:0));
		put( ">=",    c -> op2((a,b)->(a>=b)?-1:0));
		put( "<=",    c -> op2((a,b)->(a>=b)?-1:0));
		// output
		put( "base@", c -> ss.push(base)		);
		put( "base!", c -> base = ss.pop()		);
		put( "hex",   c -> base = 16			);
		put( "decimal",c-> base = 10			);
		put( "cr",    c -> out.println()	);
		put( ".",     c -> out.print(Integer.toString(ss.pop(), base)+" ") );
		put( ".r",    c -> { 
			int n=ss.pop();
			String s=Integer.toString(ss.pop(), base);
			for (int i=0; i+s.length()<n; i++) out.print(" ");
			out.print(s+" ");
		});
		put( "u.r",   c -> {
			int n=ss.pop();
			String s=Integer.toString(ss.pop()&0x7fffffff, base);
			for (int i=0;i+s.length()<n; i++) out.print(" ");
			out.print(s+" ");
		});
		put( "key",   c -> ss.push((int)tok.nextToken().charAt(0)) 			);
		put( "emit",  c -> out.print(Character.toChars(ss.pop()))	);
		put( "space", c -> out.print(" ") );
		put( "spaces",c -> {
			int n=ss.pop();
			for (int i=0; i<n; i++) out.print(" ");
		});
		// literals
		put( "[",     c -> comp = false	);
		put( "]",     c -> comp = true	);
		put( "'",     c -> { 
			var  w = find(tok.nextToken());
			ss.push(w==null ? -1 : w.idx);
		});
		
		put( "dolit", c -> ss.push(c.qf.head()) );			// integer literal
		put( "dostr", c -> ss.push(c.idx)		);			// string literals
		put( "$\"",   c -> {  	// -- w a
			var last = dict.tail();
			ss.push(last.idx);
			ss.push(last.pf.size());
			
			String s = tok.nextToken("\"");
			_colon_add(new EforthCode("dostr", s));						// literal=s
			tok.nextToken();
		});
		put( "dotstr",c -> out.print(c.str));
		put( ".\"",   c -> {
			String s = tok.nextToken("\"");
			_colon_add(new EforthCode("dotstr", s));						// literal=s
			tok.nextToken(" ");
		});
		put( "(",     c -> {
			tok.nextToken("\\)");
			tok.nextToken(" ");
		});
		put( ".(",    c -> {
			out.print(tok.nextToken("\\)"));
			tok.nextToken(" ");
		});
		put( "\\",    c -> tok.nextToken("\n"));

		// structure: if else then
		put( "branch",c -> {
			for (var w: (ss.pop()!=0 ? c.pf : c.pf1)) xt(w);
		});
		put( "if",    c -> { 
			_colon_add(new EforthCode("branch", false));						// literal=s
			dict.add(new EforthCode("temp"));
		});
		put( "else",  c -> {
			var temp = dict.tail(1);
			var last = dict.tail(2).pf.tail();
			last.add(temp.pf);
			last.stage=1; 
			temp.pf.clear();
		});
		put( "then",  c -> {
			var temp = dict.tail(1);
			var last = dict.tail(2).pf.tail();
			if (last.stage==0) {
				last.add(temp.pf);
				dict.drop_tail();
			} 
			else {
				last.add1(temp.pf);
				if (last.stage!=1) {
					temp.pf.clear();
				}
			}
		});
		// loops
		put( "loops", c -> {
			switch (c.stage) {
			case 1:		// again 
				while (true) {
					for (var w: c.pf) xt(w);
				}
				// never comes here?
				// break;
			case 2:		// repeat
				while (true) {
					for (var w: c.pf) xt(w);
					if (ss.pop()==0) break;
					for (var w: c.pf1) xt(w);
				}
				break;
			default:	// until
				while (true) {
					for (var w: c.pf) xt(w);
					if (ss.pop()!=0) break;
				}
			}
		});
		put( "begin", c -> { 
			_colon_add(new EforthCode("loops"));
			dict.add(new EforthCode("temp"));
		});
		put( "while", c -> {
			var temp = dict.tail();
			var last = dict.tail(2).pf.tail();
			last.add(temp.pf);
			last.stage=2; 
			temp.pf.clear();
		});
		put( "repeat",c -> {
			var temp = dict.tail();
			var last = dict.tail(2).pf.tail();
			last.add1(temp);
		});
		put( "again", c -> {
			var temp = dict.tail();
			var last = dict.tail(2).pf.tail();
			last.add(temp);
			last.stage=1;
		});
		put( "until", c -> {
			var temp = dict.tail();
			var last = dict.tail(2).pf.tail();
		});
		// for next
		put( "cycles", c -> {
			int i=0;
			if (c.stage==0) {
				while(true){
					for (var w: c.pf) xt(w);
					i=rs.pop();
					if (--i<0) break;
					rs.push(i);
				}
			} 
			else {
				for (var w:c.pf) xt(w);
				while (true) {
					for (var w: c.pf2) xt(w);
					i = rs.pop();
					if (--i<0) break;
					rs.push(i);
					for (var w: c.pf1) xt(w);
				}
			}
		});
		put( "for",  c -> {
			_colon_add(new EforthCode(">r"));
			_colon_add(new EforthCode("cycles"));
			dict.add(new EforthCode("temp"));
		});
		put( "aft",  c -> {
			var temp = dict.tail();
			var last = dict.tail(2).pf.tail();
			last.add(temp.pf);
			last.stage=3; 
			temp.pf.clear();
		});
		put( "next", c -> {
			var temp = dict.tail();
			var last = dict.tail(2).pf.tail();
			if (last.stage==0) {
				 last.add(temp.pf);
			}
			else last.add2(temp.pf);
			dict.drop_tail();
		});

		// defining words
		put( "exit", c -> { throw new ArithmeticException(); });	// marker to exit interpreter
		put( "exec", c -> { int n=ss.pop(); xt(dict.get(n)); });
		put( ":",    c -> {          								// -- box
			dict.add(new EforthCode(tok.nextToken()));
			comp = true;
		});
		put( ";", c -> comp = false );
		put( "docon", c -> ss.push(c.qf.head()) );					// integer literal
		put( "dovar", c -> ss.push(c.idx)       );					// string literals
		put( "create",c -> {
			dict.add(new EforthCode(tok.nextToken()));
			var last = dict.tail();
			_colon_add(new EforthCode("dovar",0));
			last.pf.head().idx = last.idx;
			last.pf.head().qf.drop_head();
		});
		put( "variable", c -> {  
			dict.add(new EforthCode(tok.nextToken()));
			var last = dict.tail();
			_colon_add(new EforthCode("dovar",0));
			last.pf.head().idx = last.idx;
		});
		put( "constant", c -> { // n --
			dict.add(new EforthCode(tok.nextToken()));
			var last = dict.tail();
			_colon_add(new EforthCode("docon",ss.pop()));
			last.pf.head().idx = last.idx;
		});
		put( "@",  c -> {   	// w -- n
			var last = dict.get(ss.pop());
			ss.push(last.pf.head().qf.head());
		});
		put( "!",  c -> {   	// n w -- 
			var last = dict.get(ss.pop());
			last.pf.head().qf.set_head(ss.pop());
		});
		put( "+!", c -> {   	// n w -- 
			var last = dict.get(ss.pop());
			int n = last.pf.head().qf.head(); 
			n+= ss.pop();
			last.pf.head().qf.set_head(n);
		});
		put( "?",  c -> {   	// w -- 
			var last = dict.get(ss.pop());
			out.print(last.pf.head().qf.head());
		});
		put( "array@", c -> {   // w a -- n
			int a = ss.pop();
			var last = dict.get(ss.pop());
			ss.push(last.pf.head().qf.get(a));
		});
		put( "array!", c -> {   // n w a -- 
			int a = ss.pop();
			var last = dict.get(ss.pop());
			last.pf.head().qf.set(a, ss.pop());
		});
		put( ",",    c -> {  	// n --
			var last = dict.tail();
			last.pf.head().qf.add(ss.pop());
		});
		put( "allot",c -> {   	// n --
			int n = ss.pop(); 
			var last = dict.tail();
			for (int i=0;i<n;i++) last.pf.head().qf.add(0);
		});
		put( "does", c -> {  	// n --
			var last = dict.tail();
			var src  = dict.get(wp);
			last.pf.addAll(src.pf.subList(ip+2, src.pf.size()));
		});
		put( "to",   c -> {   										// n -- , compile only 
			var last = dict.get(wp);
			ip++;													// current colon word
			last.pf.get(ip++).pf.head().qf.set_head(ss.pop());		// next constant
		});
		put( "is",   c -> {   										// w -- , execute only
			String s = tok.nextToken(); 
			var    w = find(s);

			if (w==null) out.print(s+" ?");
			else {
				var src = dict.get(ss.pop());						// source word
				dict.get(w.idx).pf = src.pf; 
			}
		});
		// tools
		put( "here",  c -> ss.push(EforthCode.fence) );
		put( "words", c -> { 
			int i=0;
			for (var w: dict) {
				out.print(w.name + " ");
				if (++i>15) {
					out.println();
					i=0;
				}
			}
		});
		put( ".s",    c -> { for (int n:ss) out.print(Integer.toString(n, base)+" "); });
		put( "see",   c -> { 
			String s = tok.nextToken();
			var    w = find(s);
			if (w==null) out.print(s+" ?");
			else {
				out.println(w.name+", "+w.idx+", "+w.qf.toString());
				for (var p: w.pf) out.print(p.name+", "+p.idx+", "+p.qf.toString()+"| ");       
			}
		});
		put( "time",  c -> { 
			LocalTime now = LocalTime.now();
			out.println(now); 
		});
		put( "ms",    c -> {  // n --
			try { Thread.sleep(ss.pop());} 
			catch (Exception e) { out.println(e); }
		});
		put( "bye",   c -> run = false );
	}};
}
