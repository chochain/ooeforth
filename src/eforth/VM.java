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
        io   = io0;
        dict = Dict.get_instance();
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
    ///> create dictionary - built-in words
    ///
    void CODE(String n, Consumer<Code> f) { dict.add(new Code(n, f, false)); }
    void IMMD(String n, Consumer<Code> f) { dict.add(new Code(n, f, true));  }
    
    void dict_init() {
        CODE("bye",   c -> run = false                      );
        /// stack ops
        CODE("dup",   c -> ss.push(ss.peek())               );
        CODE("over",  c -> ss.push(ss.get(ss.size()-2))     );
        CODE("swap",  c -> ss.add(ss.size()-2,ss.pop())     );
        CODE("rot",   c -> ss.push(ss.remove(ss.size()-3))  );
        CODE("drop",  c -> ss.pop()                         );
        CODE("nip",   c -> ss.remove(ss.size()-2)           );
        /// rstack opos
        CODE(">r",    c -> rs.push(ss.pop())                );
        CODE("r>",    c -> ss.push(rs.pop())                );
        CODE("r@",    c -> ss.push(rs.peek())               );
        /// extra rstack ops
        CODE("push",  c -> rs.push(ss.pop())                );
        CODE("pop",   c -> ss.push(rs.pop())                );
        /// extra stack ops
        CODE("2drop", c -> { ss.pop(); ss.pop(); }          );
        CODE("2dup",  c -> ss.addAll(ss.subList(ss.size()-2, ss.size()))     );
        CODE("2over", c -> ss.addAll(ss.subList(ss.size()-4, ss.size()-2))   );
        CODE("4dup",  c -> ss.addAll(ss.subList(ss.size()-4, ss.size()))     );
        CODE("-rot",  c -> {
            ss.push(ss.remove(ss.size()-3));
            ss.push(ss.remove(ss.size()-3));
        });
        CODE("2swap", c -> {
            ss.push(ss.remove(ss.size()-4));
            ss.push(ss.remove(ss.size()-4));
        });
        CODE("pick",  c -> {
            int i = ss.pop(), n = ss.get(ss.size()-i-1);
            ss.push(n);
        });
        CODE("roll",  c -> {
            int i = ss.pop(), n = ss.remove(ss.size()-i-1);
            ss.push(n);
        });
        /// ALU arithmetic ops
        CODE("+",     c -> alu((a,b) -> a + b)       );
        CODE("*",     c -> alu((a,b) -> a * b)       );
        CODE("-",     c -> alu((a,b) -> a - b)       );
        CODE("/",     c -> alu((a,b) -> a / b)       );
        CODE("mod",   c -> alu((a,b) -> a % b)       );
        CODE("*/",    c -> {
            int n = ss.pop();
            ss.push(ss.pop() * ss.pop() / n);
        });
        CODE("*/mod", c -> { 
            int n = ss.pop(), m = ss.pop()*ss.pop();
            ss.push(m % n);
            ss.push(m / n);
        });
        /// ALU binary ops
        CODE("and",   c -> alu((a,b) -> a & b)       );
        CODE("or",    c -> alu((a,b) -> a | b)       );
        CODE("xor",   c -> alu((a,b) -> a ^ b)       );
        CODE("negate",c -> alu(a -> -a)              );
        /// ALU logic ops
        CODE("0=",    c -> alu(a -> bool(a==0))      );
        CODE("0<",    c -> alu(a -> bool(a < 0))     );
        CODE("0>",    c -> alu(a -> bool(a > 0))     );
        CODE("=",     c -> alu((a,b) -> bool(a==b))  );
        CODE(">",     c -> alu((a,b) -> bool(a > b)) );
        CODE("<",     c -> alu((a,b) -> bool(a < b)) );
        CODE("<>",    c -> alu((a,b) -> bool(a!=b))  );
        CODE(">=",    c -> alu((a,b) -> bool(a>=b))  );
        CODE("<=",    c -> alu((a,b) -> bool(a>=b))  );
        /// IO ops
        CODE("base@", c -> ss.push(base)             );
        CODE("base!", c -> base = ss.pop()           );
        CODE("hex",   c -> base = 16                 );
        CODE("decimal",c-> base = 10                 );
        CODE("cr",    c -> io.cr()                   );
        CODE("bl",    c -> io.bl()                   );
        CODE(".",     c ->
            io.dot(IO.OP.DOT, ss.pop(), base)        );
        CODE(".r",    c -> {
            int n = ss.pop(), r = ss.pop();
            io.dot(IO.OP.DOTR, n, r, base);
        });
        CODE("u.r",   c -> {
            int n = ss.pop(), r = ss.pop();
            io.dot(IO.OP.UDOTR, n, r, base);
        });
        CODE("key",   c -> io.key()                  );
        CODE("emit",  c ->
            io.dot(IO.OP.EMIT, ss.pop())             );
        CODE("space", c -> io.spaces(1)              );
        CODE("spaces",c -> io.spaces(ss.pop())       );
        /// Compiler words
        CODE("[",     c -> compile = false           );
        CODE("]",     c -> compile = true            );
        CODE("'",     c -> { 
            var w = tick(); if (w!=null) ss.push(w.token);
        });
        /// Primitives
        CODE("dolit", c -> ss.push(c.qf.head())      );    /// integer literal
        CODE("dostr", c -> ss.push(c.token)          );    /// string literals
        IMMD("s\"",   c -> {                               /// -- w a
            var last = dict.tail();                        /// last defined word
            ss.push(last.token);
            ss.push(last.pf.size());
            
            String s = io.scan("\"");
            dict.add(new Code("dostr", s));                /// literal=s
        });
        CODE("dotstr",c -> io.pstr(c.str)            );
        IMMD(".\"",   c -> {
            String s = io.scan("\"");
            dict.add(new Code("dotstr", s)           );    /// literal=s
        });
        IMMD("(",     c -> io.scan("\\)")            );
        IMMD(".(",    c -> io.scan("\\)")            );
        IMMD("\\",    c -> io.scan("\n")             );
        ///
        ///> Branching - if else then
        ///
        CODE("branch",c -> c.branch(ss)              );
        IMMD("if",    c -> { 
            dict.add(new Code("branch", false));            /// literal=s
            dict.add(new Code(" tmp"));
        });
        IMMD("else",  c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf);
            b.stage = 1; 
            tmp.pf.clear();
        });
        IMMD("then",  c -> {
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
        CODE("loop",  c -> c.loop(ss));
        IMMD("begin", c -> { 
            dict.add(new Code("loop"));
            dict.add(new Code(" tmp"));
        });
        IMMD("while", c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf);
            b.stage = 2; 
            tmp.pf.clear();
        });
        IMMD("repeat",c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.p1.merge(tmp.pf);
        });
        IMMD("again", c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf);
            b.stage=1;
        });
        IMMD("until", c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf); dict.drop();
        });
        ///
        ///> Loop - for next
        ///
        CODE("cycles", c -> c.cycles(rs));
        IMMD("for",  c -> {
            dict.add(new Code(">r"));
            dict.add(new Code("cycles"));
            dict.add(new Code(" tmp"));
        });
        IMMD("aft",  c -> {
            Code tmp = dict.tail(), b = dict.bran();
            b.pf.merge(tmp.pf);
            b.stage = 3; 
            tmp.pf.clear();
        });
        IMMD("next", c -> {
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
        CODE("exit",  c -> c.unnest()             );   /// marker to exit interpreter
        CODE("exec",  c -> { int n=ss.pop(); dict.get(n).nest(); });
        CODE(":",     c -> { dict.add(word()); compile = true; });
        IMMD(";",     c -> compile = false );
        CODE("docon", c -> ss.push(c.qf.head())   );   /// integer literal
        CODE("dovar", c -> ss.push(c.token)       );   /// string literals
        CODE("create",c -> {
            dict.add(word());
            Code v  = new Code("dovar", 0);
            v.token = dict.tail().token;
            v.qf.drop();
            dict.compile(v);
        });
        CODE("variable", c -> {
            dict.add(word());
            Code v  = new Code("dovar", 0);
            v.token = dict.tail().token;
            dict.compile(v);
        });
        CODE("constant", c -> {                                    /// n --
            dict.add(word());
            Code v = new Code("docon", ss.pop());
            v.token = dict.tail().token;
            dict.compile(v);
        });
        CODE("@",  c -> {                                          /// w -- n
            Code w = dict.get(ss.pop());
            ss.push(w.get_var(0));
        });
        CODE("!",  c -> {                                          /// n w -- 
            Code w = dict.get(ss.pop());
            w.set_var(0, ss.pop());
        });
        CODE("+!", c -> {                                          /// n w -- 
            Code w = dict.get(ss.pop());
            int  n = w.get_var(0) + ss.pop();
            w.set_var(0, n);
        });
        CODE("?",  c -> {                                          /// w -- 
            Code w = dict.get(ss.pop());
            io.dot(IO.OP.DOT, w.get_var(0));
        });
        CODE("array@", c -> {                                      /// w a -- n
            int  a = ss.pop();
            Code w = dict.get(ss.pop());
            ss.push(w.get_var(a));
        });
        CODE("array!", c -> {                                      /// n w a -- 
            int  a = ss.pop();
            Code w = dict.get(ss.pop());
            w.set_var(a, ss.pop());
        });
        CODE(",",    c -> {                                        /// n --
            Code w = dict.tail();
            w.comma(ss.pop());
        });
        CODE("allot",c -> {                                        /// n --
            int  n = ss.pop(); 
            Code w = dict.tail();
            for (int i=0; i < n; i++) w.comma(0);
        });
        CODE("dodoes",c -> {
            var hit = false;
            for(var w : dict.get(c.token).pf) {
                if (hit) dict.compile(w);
                else if (w.name=="dodoes") hit = true;
            }
            c.unnest();
        });
        IMMD("does>", c -> {                                       /// n --
            Code w = new Code("dodoes", false);
            w.token = dict.tail().token;
            dict.compile(w);
        });
        CODE("to",   c -> {                                        /// n -- , compile only
            Code w = tick(); if (w==null) return;
            w.set_var(0, ss.pop());
        });
        CODE("is",   c -> {                                        /// w -- , execute only
            Code w   = tick(); if (w==null) return;
            Code src = dict.get(ss.pop());                         /// source word
            dict.get(w.token).pf = src.pf; 
        });
        //// tools
        CODE("here",  c -> ss.push(Code.fence));
        CODE("words", c -> io.words(dict));
        CODE(".s",    c -> io.ss_dump(ss, base));
        CODE("see",   c -> io.see(tick()));
        CODE("clock", c -> ss.push((int)System.currentTimeMillis()));
        CODE("ms",    c -> {                                       /// n -- delay n milliseconds
            try { Thread.sleep(ss.pop()); } 
            catch (Exception e) { io.err(e); }
        });
        CODE("forget", c -> {
            Code m = dict.find("boot");
            Code w = tick(); if (w==null) return;
            int  t = Math.max(w.token, m.token + 1);
            dict.subList(t, dict.size()).clear();
        });
        CODE("boot",   c -> {
            int t = dict.find("boot").token + 1;
            dict.subList(t, dict.size()).clear();
        });
    }
}
