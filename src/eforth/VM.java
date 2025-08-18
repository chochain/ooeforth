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
    Dict   dict;
    IO     io;
    Random rnd = new Random();      ///< random number generator
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
        io.words(dict);
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
        Code w = dict.find(idiom, compile);             ///> search dictionary

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
                dict.compile(new Code(_dolit, n));      ///>> append literal to latest defined word
            else ss.push(n);                            ///>> or, add number to top of stack
        }                                            
        catch (NumberFormatException ex) {              ///> if it's not a number
            io.pstr(idiom + " ?");                      ///> * show not found sign
            compile = false; 
        }
    }
    Code word(boolean existed) {
        String s = io.next_token();
        Code   w = dict.find(s, compile);
        if (existed) {
            if (w==null) io.pstr(s+"?");
        }
        else {
            if (w!=null) io.pstr(s+" reDef?");
            w = new Code(s);                            ///> create new Code
        }
        return w;
    }
    Code word() { return word(false); }                 ///> read token
    Code tick() { return word(true); }                  ///> find existed word
    ///
    ///> ALU funtions (aka. macros)
    ///
    int  BOOL(boolean f) { return f ? -1 : 0;     }
    int  UINT(int v)     { return v & 0x7fffffff; }
    void ALU(Function<Integer, Integer> m) { 
        int n = ss.pop(); ss.push(m.apply(n));           
    }
    void ALU(BiFunction<Integer, Integer, Integer> m) { 
        int n = ss.pop(); ss.push(m.apply(ss.pop(), n)); 
    }
    ///
    ///> built-in words and macros
    ///
    Consumer<Code> _tmp    = c -> { /* do nothing */ };
    Consumer<Code> _dolit  = c -> ss.push(c.qf.head());
    Consumer<Code> _dostr  = c -> ss.push(c.token);
    Consumer<Code> _dotstr = c -> io.pstr(c.str);
    Consumer<Code> _branch = c -> c.branch(ss);
    Consumer<Code> _begin  = c -> c.begin(ss);
    Consumer<Code> _loop   = c -> c.loop(rs);
    Consumer<Code> _tor    = c -> rs.push(ss.pop());
    Consumer<Code> _dovar  = c -> ss.push(c.token);
    Consumer<Code> _dodoes = c -> {
        var hit = false;
        for(var w : dict.get(c.token).pf) {
            if (hit) dict.compile(w);
            else if (w.name=="dodoes") hit = true;
        }
        c.unnest();
    };
    void ADD_W(Code w)                    { dict.compile(w);                 }
    void CODE(String n, Consumer<Code> f) {
        System.out.printf(" [%d]%s\n", dict.size(), n);
        dict.add(new Code(n, f, false)); }
    void IMMD(String n, Consumer<Code> f) {
        System.out.printf("*[%d]%s\n", dict.size(), n);
        dict.add(new Code(n, f, true));  }
    void BRAN(FV<Code> pf) { Code tmp=dict.tail(); pf.merge(tmp.pf); tmp.pf.clear(); }
    ///
    ///> create dictionary - built-in words
    ///
    void dict_init() {
        CODE("bye",   c -> run = false                      );
        ///
        /// @defgroup ALU ops
        /// @{
        CODE("+",     c -> ALU((a,b) -> a + b)       );
        CODE("*",     c -> ALU((a,b) -> a * b)       );
        CODE("-",     c -> ALU((a,b) -> a - b)       );
        CODE("/",     c -> ALU((a,b) -> a / b)       );
        CODE("mod",   c -> ALU((a,b) -> a % b)       );
        CODE("*/",    c -> {
            int n = ss.pop();
            ss.push(ss.pop() * ss.pop() / n);
        });
        CODE("*/mod", c -> { 
            int n = ss.pop(), m = ss.pop()*ss.pop();
            ss.push(m % n);
            ss.push(m / n);
        });
        CODE("and",   c -> ALU((a,b) -> a & b)       );
        CODE("or",    c -> ALU((a,b) -> a | b)       );
        CODE("xor",   c -> ALU((a,b) -> a ^ b)       );
        CODE("abs",   c -> ALU(a -> Math.abs(a))     );
        CODE("negate",c -> ALU(a -> -a)              );
        CODE("invert",c -> ALU(a -> ~UINT(a))        );
        CODE("rshift",c -> ALU((a,b) -> a >>> b)     );
        CODE("lshift",c -> ALU((a,b) -> a << b)      );
        CODE("max",   c -> ALU((a,b) -> Math.max(a,b)));
        CODE("min",   c -> ALU((a,b) -> Math.min(a,b)));
        CODE("2*",    c -> ALU(a -> a *= 2)          );
        CODE("2/",    c -> ALU(a -> a /= 2)          );
        CODE("1+",    c -> ALU(a -> a += 1)          );
        CODE("1-",    c -> ALU(a -> a -= 1)          );
        /// @}
        /// @defgroup Logic ops
        /// @{
        CODE("0=",    c -> ALU(a -> BOOL(a==0))      );
        CODE("0<",    c -> ALU(a -> BOOL(a < 0))     );
        CODE("0>",    c -> ALU(a -> BOOL(a > 0))     );
        CODE("=",     c -> ALU((a,b) -> BOOL(a==b))  );
        CODE(">",     c -> ALU((a,b) -> BOOL(a > b)) );
        CODE("<",     c -> ALU((a,b) -> BOOL(a < b)) );
        CODE("<>",    c -> ALU((a,b) -> BOOL(a!=b))  );
        CODE(">=",    c -> ALU((a,b) -> BOOL(a>=b))  );
        CODE("<=",    c -> ALU((a,b) -> BOOL(a>=b))  );
        CODE("u<",    c -> ALU((a,b) -> BOOL(UINT(a) < UINT(b))));
        CODE("u>",    c -> ALU((a,b) -> BOOL(UINT(a) > UINT(b))));
        /// @}
        /// @defgroup Data Stack ops
        /// @brief - opcode sequence can be changed below this line
        /// @{
        CODE("dup",   c -> ss.push(ss.peek())               );
        CODE("drop",  c -> ss.pop()                         );
        CODE("over",  c -> ss.push(ss.get(ss.size()-2))     );
        CODE("swap",  c -> ss.add(ss.size()-2,ss.pop())     );
        CODE("rot",   c -> ss.push(ss.remove(ss.size()-3))  );
        CODE("-rot",  c -> {
            ss.push(ss.remove(ss.size()-3));
            ss.push(ss.remove(ss.size()-3));
        });
        CODE("pick",  c -> {
            int i = ss.pop(), n = ss.get(ss.size()-i-1);
            ss.push(n);
        });
        CODE("roll",  c -> {
            int i = ss.pop(), n = ss.remove(ss.size()-i-1);
            ss.push(n);
        });
        CODE("nip",   c -> ss.remove(ss.size()-2)                          );
        CODE("?dup",  c -> { if (ss.peek()!=0) ss.push(ss.peek()); }       );
        /// @}
        /// @defgroup Data Stack ops - double
        /// @{
        CODE("2dup",  c -> ss.addAll(ss.subList(ss.size()-2, ss.size()))   );
        CODE("2drop", c -> { ss.pop(); ss.pop(); }                         );
        CODE("2swap", c -> {
            ss.push(ss.remove(ss.size()-4));
            ss.push(ss.remove(ss.size()-4));
        });
        CODE("2over", c -> ss.addAll(ss.subList(ss.size()-4, ss.size()-2)) );
        /// @}
        /// @defgroup Return Stack ops
        /// @{
        CODE(">r",    c -> rs.push(ss.pop())               );
        CODE("r>",    c -> ss.push(rs.pop())               );
        CODE("r@",    c -> ss.push(rs.peek())              );
        /// @}
        /// @defgroup Return Stack ops - Extra
        /// @{
        CODE("push",  c -> rs.push(ss.pop())               );
        CODE("pop",   c -> ss.push(rs.pop())               );
        /// @}
        /// @defgroup IO ops
        /// @{
        CODE("base@", c -> ss.push(base)                   );
        CODE("base!", c -> base = ss.pop()                 );
        CODE("hex",   c -> base = 16                       );
        CODE("decimal",c-> base = 10                       );
        CODE("cr",    c -> io.cr()                         );
        CODE("bl",    c -> io.bl()                         );
        CODE(".",     c -> io.dot(IO.OP.DOT, ss.pop(), base) );
        CODE("u.",    c -> io.dot(IO.OP.UDOT, ss.pop(), base));
        CODE(".r",    c -> {
            int n = ss.pop(), r = ss.pop();
            io.dot(IO.OP.DOTR, n, r, base);
        });
        CODE("u.r",   c -> {
            int n = ss.pop(), r = ss.pop();
            io.dot(IO.OP.UDOTR, n, r, base);
        });
        CODE("type",  c -> { ss.pop(); io.pstr(c.str); }  );
        CODE("key",   c -> io.key()                       );
        CODE("emit",  c -> io.dot(IO.OP.EMIT, ss.pop())   );
        CODE("space", c -> io.spaces(1)                   );
        CODE("spaces",c -> io.spaces(ss.pop())            );
        /// @}
        /// @defgroup Literal ops
        /// @{
        IMMD("(",     c -> io.scan("\\)")            );
        IMMD(".(",    c -> io.scan("\\)")            );
        IMMD("\\",    c -> io.scan("\n")             );
        IMMD("s\"",   c -> {                               /// -- w a
            Code last = dict.tail();                       /// last defined word
            ss.push(last.token);
            ss.push(last.pf.size());
            
            String s = io.scan("\"");
            ADD_W(new Code(_dostr, s));                    /// literal=s
        });
        IMMD(".\"",   c -> {
            String s = io.scan("\"");
            ADD_W(new Code(_dotstr, s));                   /// literal=s
        });
        /// @}
        /// @defgroup Branching ops
        /// @brief - if...then, if...else...then
        ///     dict[-2]->pf[0,1,2,...,-1] as *last
        ///                              \--->pf[...] if  <--+ merge
        ///                               \-->p1[...] else   |
        ///     dict[-1]->pf[...] as *tmp -------------------+
        /// @{
        IMMD("if",    c -> { 
            ADD_W(new Code(_branch));                      /// literal=s
            dict.add(new Code(_tmp));
        });
        IMMD("else",  c -> {
            Code b = dict.bran();
            BRAN(b.pf);
            b.stage = 1; 
        });
        IMMD("then",  c -> {
            Code b = dict.bran();                          ///< branching target
            int  s = b.stage;                              ///< branching state
            if (s==0) {                                    /// * if..{pf}..then
                BRAN(b.pf); dict.drop();
            } 
            else {                                         /// * else..{p1}..then, or
                BRAN(b.p1);                                /// * then..{p1}..next
                if (s==1) dict.drop();                     /// * if..else..then
            }
        });
        /// @}
        /// @defgroup Loops
        /// @brief  - begin...again, begin...f until, begin...f while...repeat
        /// @{
        IMMD("begin", c -> { 
            ADD_W(new Code(_begin));                       /// * branch targer
            dict.add(new Code(_tmp));                    
        });
        IMMD("while", c -> {
            Code b = dict.bran();
            BRAN(b.pf);                                    /// * begin..{pf}..f.while
            b.stage = 2; 
        });
        IMMD("repeat",c -> {
            Code b = dict.bran();
            BRAN(b.p1); dict.drop();                       /// * while..{p1}..repeat
        });
        IMMD("again", c -> {
            Code b = dict.bran();
            BRAN(b.pf); dict.drop();                       /// * begin..{pf}..again
            b.stage=1;
        });
        IMMD("until", c -> {
            Code b = dict.bran();
            BRAN(b.pf); dict.drop();                       /// * begin..{pf}..f.until
        });
        /// @}
        /// @defgrouop FOR loops
        /// @brief  - for...next, for...aft...then...next
        /// @{
        IMMD("for",  c -> {
            ADD_W(new Code(_tor));
            ADD_W(new Code(_loop));
            dict.add(new Code(_tmp));
        });
        IMMD("aft",  c -> {
            Code b = dict.bran();
            BRAN(b.pf);
            b.stage = 3; 
        });
        IMMD("next", c -> {
            Code b = dict.bran();                         /// * for..{pf}..next, or
            BRAN(b.stage==0 ? b.pf : b.p2); dict.drop();  /// * then..{p2}..next
        });
        /// @}
        /// @defgrouop Compiler ops
        /// @{
        CODE("[",     c -> compile = false                );
        CODE("]",     c -> compile = true                 );
        CODE(":",     c -> {
            dict.add(word()); compile = true;
        });
        IMMD(";",     c -> compile = false                );
        CODE("variable", c -> {
            dict.add(word());
            Code v  = new Code(_dovar, 0);
            v.token = dict.tail().token;
            dict.compile(v);
        });
        CODE("constant", c -> {                                    /// n --
            dict.add(word());
            Code v = new Code(_dolit, ss.pop());
            v.token = dict.tail().token;
            dict.compile(v);
        });
        CODE("postpone", c -> {
            Code w = tick(); if (w!=null) ADD_W(w);
        });
        CODE("immediate",c -> dict.tail().immediate()     );
        CODE("exit",  c -> c.unnest()                     );       /// marker to exit interpreter
        CODE("exec",  c -> dict.get(ss.pop()).nest()      );
        CODE("create",c -> {
            dict.add(word());
            Code v  = new Code(_dovar, 0);
            v.token = dict.tail().token;
            v.qf.drop();
            ADD_W(v);
        });
        IMMD("does>", c -> {                                       /// n --
            Code w = new Code(_dodoes);
            w.token = dict.tail().token;
            ADD_W(w);
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
        /// @}
        /// @defgroup Memory Access ops
        /// @{
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
        /// @}
        /// @defgroup Debug ops
        /// @{
        CODE("here",  c -> ss.push(Code.fence)                     );
        CODE("'",     c -> { 
            Code w = tick(); if (w!=null) ss.push(w.token);
        });
        CODE(".s",    c -> io.ss_dump(ss, base)                    );
        CODE("words", c -> io.words(dict)                          );
        CODE("see",   c -> io.see(tick())                          );
        CODE("clock", c -> ss.push((int)System.currentTimeMillis()));
        CODE("rnd",   c -> ALU(a -> rnd.nextInt(a))                );
        CODE("depth", c -> ss.push(dict.size())                    );
        CODE("ms",    c -> {                                       /// n -- delay n ms
            try { Thread.sleep(ss.pop()); } 
            catch (Exception e) { io.err(e); }
        });
        CODE("forget", c -> {
            Code m = dict.find("boot", compile);
            Code w = tick(); if (w==null) return;
            dict.forget(Math.max(w.token, m.token + 1));
        });
        CODE("boot",   c -> {
            int t = dict.find("boot", compile).token + 1;
            dict.forget(t);
        });
    }
}
