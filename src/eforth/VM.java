///
/// @file 
/// @brief - Virtual Machine class
///
package eforth;

import java.util.*;
import java.io.*;
import java.time.*;
import java.util.function.*;

public class VM {
    Dict dict;
    IO   io;
    ///
    ///> Forth stacks and dictionary
    ///
    Stack<Integer>  ss = new Stack<>();
    Stack<Integer>  rs = new Stack<>();
    ///
    ///> Forth internal variables
    ///
    int     base    = 10;           ///< numeric radix
    boolean run     = true;         ///< VM execution flag
    boolean compile = false;        ///< state: intepreter or compiling
    ///
    ///> functional interfaces
    ///
    public VM(IO io0) {
        io = io0;
        dict_init();
    }
    ///
    ///> Forth outer interpreter - process one line a time
    ///
    public void outer() {
        String idiom;
        while (run && (idiom=io.next_token()) != null) { ///> fetch next token
            parse(idiom);
            if (compile) io.pstr("> ");                 ///> if it's in compile mode
            else {                                      ///> in interpreter mode
                io.ss_dump(ss, base);
                io.pstr("OK ");                         ///> * OK prompt
            }
        }
    }
    void parse(String idiom) {                          /// outer interpreter (one line a time)
        io.pstr("idiom="+idiom);
        Code w = dict.find(idiom);                      ///> search dictionary

        if (w != null) {                                ///> if word found
            io.pstr(" >> " + w + "\n");
            if (!compile || w.immd) {                   ///> * check whether in immediate mode 
                try                 { w.nest();  }      ///> execute immediately
                catch (Exception e) { io.err(e); }      /// just-in-case it failed
            }
            else dict.compile(w);                       ///> * add to dictionary if in compile mode
            return;
        }
        ///> word not found, try as a number
        try {
            int n=Integer.parseInt(idiom, base);        ///> * try process as a number
            io.pstr(" >> "+n);
            if (compile)                                ///>> in compile mode 
                dict.compile(new Code("dolit", n));     ///>> append literal to latest defined word
            else ss.push(n);                            ///>> or, add number to top of stack
        }                                            
        catch (NumberFormatException ex) {              ///> if it's not a number
            io.pstr(idiom + " ?");                      ///> * show not found sign
            compile = false; 
        }
    }
    Code word(boolean existed) {
        String s = io.next_token();
        Code   w = dict.find(s);
        if (existed) {
            if (w==null) io.pstr(s+"?");
        }
        else {
            if (w==null) io.pstr(s+" reDef?");
            w = new Code(s);                            ///> create new Code
        }
        return w;
    }
    Code word() { return word(false); }                 ///> read token
    Code tick() { return word(true); }                  ///> find existed word
    ///
    ///> ALU function operators
    ///
    int bool(boolean f) { return f ? -1 : 0; }
    void alu(Function<Integer, Integer> m) { 
        int n=ss.pop(); ss.push(m.apply(n));           
    }
    void alu(BiFunction<Integer, Integer, Integer> m) { 
        int n=ss.pop(); ss.push(m.apply(ss.pop(), n)); 
    }
    ///
    ///> create dictionary with given word list
    ///
    private void dict_init() {
        dict = Dict.get_instance();
        dict.init(_vtable);
        
        final String immd[] = {
            "if",    "else",  "then",
            "begin", "again", "until", "while", "repeat", 
            "for",   "next",  "aft",
            ";",    "$\"",    ".\"",  "(",    "\\"    
        };
        for (String s : immd)  {
            // dict.add(new Code(s).immediate());
            dict.find(s).immediate();
        }
    }
    private final LinkedHashMap<String, Consumer<Code>> _vtable = new LinkedHashMap<>() {{
        put( "bye",   c -> run = false                      );
        /// stack ops
        put( "dup",   c -> ss.push(ss.peek())               );
        put( "over",  c -> ss.push(ss.get(ss.size()-2))     );
        put( "swap",  c -> ss.add(ss.size()-2,ss.pop())     );
        put( "rot",   c -> ss.push(ss.remove(ss.size()-3))  );
        put( "drop",  c -> ss.pop()                         );
        put( "nip",   c -> ss.remove(ss.size()-2)           );
        /// rstack opos
        put( ">r",    c -> rs.push(ss.pop())                );
        put( "r>",    c -> ss.push(rs.pop())                );
        put( "r@",    c -> ss.push(rs.peek())               );
        /// extra rstack ops
        put( "push",  c -> rs.push(ss.pop())                );
        put( "pop",   c -> ss.push(rs.pop())                );
        /// extra stack ops
        put( "2drop", c -> { ss.pop(); ss.pop(); }          );
        put( "2dup",  c -> ss.addAll(ss.subList(ss.size()-2, ss.size()))     );
        put( "2over", c -> ss.addAll(ss.subList(ss.size()-4, ss.size()-2))   );
        put( "4dup",  c -> ss.addAll(ss.subList(ss.size()-4, ss.size()))     );
        put( "-rot",  c -> {
            ss.push(ss.remove(ss.size()-3));
            ss.push(ss.remove(ss.size()-3));
        });
        put( "2swap", c -> {
            ss.push(ss.remove(ss.size()-4));
            ss.push(ss.remove(ss.size()-4));
        });
        put( "pick",  c -> {
            int i = ss.pop(), n = ss.get(ss.size()-i-1);
            ss.push(n);
        });
        put( "roll",  c -> {
            int i = ss.pop(), n = ss.remove(ss.size()-i-1);
            ss.push(n);
        });
        /// ALU arithmetic ops
        put( "+",     c -> alu((a,b) -> a + b)       );
        put( "*",     c -> alu((a,b) -> a * b)       );
        put( "-",     c -> alu((a,b) -> a - b)       );
        put( "/",     c -> alu((a,b) -> a / b)       );
        put( "mod",   c -> alu((a,b) -> a % b)       );
        put( "*/",    c -> {
            int n = ss.pop();
            ss.push(ss.pop() * ss.pop() / n);
        });
        put( "*/mod", c -> { 
            int n = ss.pop(), m = ss.pop()*ss.pop();
            ss.push(m % n);
            ss.push(m / n);
        });
        /// ALU binary ops
        put( "and",   c -> alu((a,b) -> a & b)       );
        put( "or",    c -> alu((a,b) -> a | b)       );
        put( "xor",   c -> alu((a,b) -> a ^ b)       );
        put( "negate",c -> alu(a -> -a)              );
        /// ALU logic ops
        put( "0=",    c -> alu(a -> bool(a==0))      );
        put( "0<",    c -> alu(a -> bool(a < 0))     );
        put( "0>",    c -> alu(a -> bool(a > 0))     );
        put( "=",     c -> alu((a,b) -> bool(a==b))  );
        put( ">",     c -> alu((a,b) -> bool(a > b)) );
        put( "<",     c -> alu((a,b) -> bool(a < b)) );
        put( "<>",    c -> alu((a,b) -> bool(a!=b))  );
        put( ">=",    c -> alu((a,b) -> bool(a>=b))  );
        put( "<=",    c -> alu((a,b) -> bool(a>=b))  );
        /// IO ops
        put( "base@", c -> ss.push(base)             );
        put( "base!", c -> base = ss.pop()           );
        put( "hex",   c -> base = 16                 );
        put( "decimal",c-> base = 10                 );
        put( "cr",    c -> io.cr()                   );
        put( "bl",    c -> io.bl()                   );
        put( ".",     c ->
            io.dot(IO.OP.DOT, ss.pop(), base)        );
        put( ".r",    c -> {
            int n = ss.pop(), r = ss.pop();
            io.dot(IO.OP.DOTR, n, r, base);
        });
        put( "u.r",   c -> {
            int n = ss.pop(), r = ss.pop();
            io.dot(IO.OP.UDOTR, n, r, base);
        });
        put( "key",   c -> io.key()                  );
        put( "emit",  c ->
            io.dot(IO.OP.EMIT, ss.pop())             );
        put( "space", c -> io.spaces(1)              );
        put( "spaces",c -> io.spaces(ss.pop())       );
        /// Compiler words
        put( "[",     c -> compile = false           );
        put( "]",     c -> compile = true            );
        put( "'",     c -> { 
            var w = tick(); if (w!=null) ss.push(w.token);
        });
        /// Primitives
        put( "dolit", c -> ss.push(c.qf.head())      );    /// integer literal
        put( "dostr", c -> ss.push(c.token)          );    /// string literals
        put( "s\"",   c -> {                               /// -- w a
            var last = dict.tail();                        /// last defined word
            ss.push(last.token);
            ss.push(last.pf.size());
            
            String s = io.scan("\"");
            dict.add(new Code("dostr", s));                /// literal=s
        });
        put( "dotstr",c -> io.pstr(c.str)            );
        put( ".\"",   c -> {
            String s = io.scan("\"");
            dict.add(new Code("dotstr", s)           );    /// literal=s
        });
        put( "(",     c -> io.scan("\\)")            );
        put( ".(",    c -> io.scan("\\)")            );
        put( "\\",    c -> io.scan("\n")             );
        ///
        ///> Branching - if else then
        ///
        put( "branch",c -> c.branch(ss)              );
        put( "if",    c -> { 
            dict.add(new Code("branch", false));            /// literal=s
            dict.add(new Code(" tmp"));
        });
        put( "else",  c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf);
            b.stage = 1; 
            tmp.pf.clear();
        });
        put( "then",  c -> {
            Code tmp = dict.tail(), b = dict.bran();
            if (b.stage==0) {
                b.pf.merge(tmp.pf);
                dict.drop();
            } 
            else {
                b.p1.merge(tmp.pf);
                if (b.stage != 1) {
                    tmp.pf.clear();
                }
            }
        });
        ///
        ///> Loop - begin-while-repeat again
        ///
        put( "loop",  c -> c.loop(ss));
        put( "begin", c -> { 
            dict.add(new Code("loop"));
            dict.add(new Code(" tmp"));
        });
        put( "while", c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf);
            b.stage = 2; 
            tmp.pf.clear();
        });
        put( "repeat",c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.p1.merge(tmp.pf);
        });
        put( "again", c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf);
            b.stage=1;
        });
        put( "until", c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf); dict.drop();
        });
        ///
        ///> Loop - for next
        ///
        put( "cycles", c -> c.cycles(rs));
        put( "for",  c -> {
            dict.add(new Code(">r"));
            dict.add(new Code("cycles"));
            dict.add(new Code(" tmp"));
        });
        put( "aft",  c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf);
            b.stage = 3; 
            tmp.pf.clear();
        });
        put( "next", c -> {
            Code tmp = dict.tail(), b = dict.bran();
            if (b.stage==0) {
                 b.pf.merge(tmp.pf);
            }
            else b.p2.merge(tmp.pf);
            dict.drop();
        });
        ///
        ///> Defining words
        ///
        put( "exit",  c -> c.unnest()             );   /// marker to exit interpreter
        put( "exec",  c -> { int n=ss.pop(); dict.get(n).nest(); });
        put( ":",     c -> { dict.add(word()); compile = true; });
        put( ";",     c -> compile = false );
        put( "docon", c -> ss.push(c.qf.head())   );   /// integer literal
        put( "dovar", c -> ss.push(c.token)       );   /// string literals
        put( "create",c -> {
            dict.add(word());
            Code v  = new Code("dovar", 0);
            v.token = dict.tail().token;
            v.qf.drop();
            dict.compile(v);
        });
        put( "variable", c -> {
            dict.add(word());
            Code v  = new Code("dovar", 0);
            v.token = dict.tail().token;
            dict.compile(v);
        });
        put( "constant", c -> {                                    /// n --
            dict.add(word());
            Code v = new Code("docon", ss.pop());
            v.token = dict.tail().token;
            dict.compile(v);
        });
        put( "@",  c -> {                                          /// w -- n
            Code w = dict.get(ss.pop());
            ss.push(w.get_var(0));
        });
        put( "!",  c -> {                                          /// n w -- 
            Code w = dict.get(ss.pop());
            w.set_var(0, ss.pop());
        });
        put( "+!", c -> {                                          /// n w -- 
            Code w = dict.get(ss.pop());
            int  n = w.get_var(0) + ss.pop();
            w.set_var(0, n);
        });
        put( "?",  c -> {                                          /// w -- 
            Code w = dict.get(ss.pop());
            io.dot(IO.OP.DOT, w.get_var(0));
        });
        put( "array@", c -> {                                      /// w a -- n
            int  a = ss.pop();
            Code w = dict.get(ss.pop());
            ss.push(w.get_var(a));
        });
        put( "array!", c -> {                                      /// n w a -- 
            int  a = ss.pop();
            Code w = dict.get(ss.pop());
            w.set_var(a, ss.pop());
        });
        put( ",",    c -> {                                        /// n --
            Code w = dict.tail();
            w.comma(ss.pop());
        });
        put( "allot",c -> {                                        /// n --
            int  n = ss.pop(); 
            Code w = dict.tail();
            for (int i=0; i < n; i++) w.comma(0);
        });
        put( "dodoes",c -> {
            var hit = false;
            for(var w : dict.get(c.token).pf) {
                if (hit) dict.compile(w);
                else if (w.name=="dodoes") hit = true;
            }
            c.unnest();
        });
        put( "does>", c -> {                                       /// n --
            Code w = new Code("dodoes", false);
            w.token = dict.tail().token;
            dict.compile(w);
        });
        put( "to",   c -> {                                        /// n -- , compile only
            Code w = tick(); if (w==null) return;
            w.set_var(0, ss.pop());
        });
        put( "is",   c -> {                                        /// w -- , execute only
            Code w   = tick(); if (w==null) return;
            Code src = dict.get(ss.pop());                         /// source word
            dict.get(w.token).pf = src.pf; 
        });
        //// tools
        put( "here",  c -> ss.push(Code.fence));
        put( "words", c -> io.words(dict));
        put( ".s",    c -> io.ss_dump(ss, base));
        put( "see",   c -> io.see(tick()));
        put( "clock", c -> ss.push((int)System.currentTimeMillis()));
        put( "ms",    c -> {                                       /// n -- delay n milliseconds
            try { Thread.sleep(ss.pop()); } 
            catch (Exception e) { io.err(e); }
        });
        put( "forget", c -> {
            Code m = dict.find("boot");
            Code w = tick(); if (w==null) return;
            int  t = Math.max(w.token, m.token + 1);
            dict.subList(t, dict.size()).clear();
        });
        put( "boot",   c -> {
            int t = dict.find("boot").token + 1;
            dict.subList(t, dict.size()).clear();
        });
    }};
}
