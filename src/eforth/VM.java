///
/// @file 
/// @brief - Virtual Machine class
///
package eforth;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

public class VM {
    Dict   dict;
    IO     io;
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
        Code b = new Code(_dolit, "lit", 10);            ///< use dict[0] as base store
        b.token = 0;
        dict.get(0).pf.add(b);
    }
    ///
    ///> Forth outer interpreter - process one line a time
    ///
    public void ok(boolean stat) {
        if (stat) io.mstat();
        if (io.load_depth() > 0) return;                /// * skip when loading
        if (compile) io.pstr("> ");                     ///> compile mode prompt
        else {
            io.pstr("< ");
            io.ss_dump(ss, base);
            io.pstr(">ok ");                            ///> * OK prompt (interpreter)
        }
    }
    public boolean outer() {
        String idiom = io.next_token();
        while (run && idiom!=null) {                    ///> parse next token
            parse(idiom);
            idiom = io.next_token();
        }
        ok(false);
        return run;                                     ///> * return VM status
    }
    void parse(String idiom) {                          ///> outer interpreter (one line a time)
        io.debug("find "+idiom);
        Code w = dict.find(idiom, compile);             ///< search dictionary
        if (w != null) {                                ///> found word?
            io.debug(" => [" + w.token + "]" + w.name + "\n");
            if (!compile || w.immd) {                   ///> in interpreter mode?
                try                 { w.nest();  }      ///> * execute immediately
                catch (Exception e) { io.err(e); }      ///> * just-in-case it failed
            }
            else dict.compile(w);                       ///> add to dictionary if in compile mode
            return;
        }
        else io.debug(" => not found");
        ///> word not found, try as a number
        try {
            int n=Integer.parseInt(idiom, base);        ///> * try process as a number
            io.debug(" => "+n+"\n");
            if (compile)                                ///>> in compile mode 
                dict.compile(new Code(_dolit, "lit", n));  ///> add to latest defined word
            else ss.push(n);                            ///> or, add number to top of stack
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
    Consumer<Code> _dostr  = c -> {
        ss.push(c.token);
        ss.push(dict.str(c.token).length());
    };
    Consumer<Code> _dotstr = c -> io.pstr(c.str);
    Consumer<Code> _branch = c -> c.branch(ss);
    Consumer<Code> _begin  = c -> c.begin(ss);
    Consumer<Code> _for    = c -> c.dofor(rs);
    Consumer<Code> _loop   = c -> c.loop(rs);
    Consumer<Code> _tor    = c -> rs.push(ss.pop());
    Consumer<Code> _tor2   = c -> { rs.push(ss.pop()); rs.push(ss.pop()); };
    Consumer<Code> _dovar  = c -> ss.push(c.token);
    Consumer<Code> _dodoes = c -> {
        var hit = false;
        for(var w : dict.get(c.token).pf) {    /// * scan through defining word
            if (w==c) hit = true;              /// does> ...
            else if (hit) dict.compile(w);     /// capture words
        }
        c.unnest();                            /// exit nest
    };
    void ADDW(Code w) { dict.compile(w); }
    void LIT(Code w)  { dict.compile(w); w.token = dict.idx();               }
    void CODE(String n, Consumer<Code> f) { dict.add(new Code(n, f, false)); }
    void IMMD(String n, Consumer<Code> f) { dict.add(new Code(n, f, true));  }
    void BRAN(FV<Code> pf) { Code t=dict.tail(); pf.merge(t.pf); t.pf.clear(); }
    ///
    ///> create dictionary - built-in words
    ///
    void dict_init() {
        CODE("bye",   c -> run = false               );
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
        CODE("i",     c -> ss.push(rs.peek())              );
        /// @}
        /// @defgroup Return Stack ops - Extra
        /// @{
        CODE("push",  c -> rs.push(ss.pop())               );
        CODE("pop",   c -> ss.push(rs.pop())               );
        /// @}
        /// @defgroup IO ops
        /// @{
        CODE("base",  c->ss.push(0)                        );
        CODE("hex",   c -> dict.get(0).set_var(0, base=16) );
        CODE("decimal",c-> dict.get(0).set_var(0, base=10) );
        CODE("cr",    c -> io.cr()                         );
        CODE("bl",    c -> io.bl()                         );
        CODE(".",     c -> io.dot(IO.OP.DOT, ss.pop(), base) );
        CODE("u.",    c -> io.dot(IO.OP.UDOT, ss.pop(), base));
        CODE(".r",    c -> {
            int r = ss.pop(), n = ss.pop();
            io.dot(IO.OP.DOTR, n, r, base);
        });
        CODE("u.r",   c -> {
            int r = ss.pop(), n = ss.pop();
            io.dot(IO.OP.UDOTR, n, r, base);
        });
        CODE("type",  c-> {
            int n = ss.pop(), i_w = ss.pop();              ///< drop len, get index
            io.pstr(i_w < 0 ? io.pad() : dict.str(i_w));
        });
        CODE("key",   c -> io.key()                       );
        CODE("emit",  c -> io.dot(IO.OP.EMIT, ss.pop())   );
        CODE("space", c -> io.spaces(1)                   );
        CODE("spaces",c -> io.spaces(ss.pop())            );
        /// @}
        /// @defgroup Literal ops
        /// @{
        IMMD("(",     c -> io.scan("\\)")                 );
        IMMD(".(",    c -> {
            io.scan("\\)"); io.pstr(io.pad());
        });
        IMMD("\\",    c -> io.scan("\n")                  );
        IMMD("s\"",   c -> {                               /// -- w a
            String s = io.scan("\""); if (s==null) return;
            if (compile) LIT(new Code(_dostr, "s\"", s));  /// literal=s
            else { ss.push(-1); ss.push(s.length()); }     /// use pad
        });
        IMMD(".\"",   c -> {
            String s = io.scan("\""); if (s==null) return;
            if (compile) LIT(new Code(_dotstr, ".\"", s)); /// literal=s
            else io.pstr(s);
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
            ADDW(new Code(_branch, "if"));                 /// literal=s
            dict.add(new Code(_tmp, ""));
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
            ADDW(new Code(_begin, "begin"));               /// * branch targer
            dict.add(new Code(_tmp, ""));                    
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
            ADDW(new Code(_tor, "tor"));
            ADDW(new Code(_for, "for"));
            dict.add(new Code(_tmp, ""));
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
        /// @defgrouop DO loops
        /// @brief  - do...loop, do..leave..loop
        /// @{
        IMMD("do",   c -> {
             ADDW(new Code(_tor2, "tor2"));              ///< ( limit first -- )
             ADDW(new Code(_loop, "do"));
             dict.add(new Code(_tmp, ""));
        });
        CODE("leave", c -> c.unnest());                   /// * exit loop
        IMMD("loop", c -> {
             Code b = dict.bran();
             BRAN(b.pf);                                  /// * do..{pf}..loop
             dict.drop();
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
            LIT(new Code(_dovar, "var", 0));
        });
        CODE("constant", c -> {                                    /// n --
            dict.add(word());
            LIT(new Code(_dolit, "lit", ss.pop()));
        });
        CODE("postpone", c -> {
            Code w = tick(); if (w!=null) ADDW(w);
        });
        CODE("immediate",c -> dict.tail().immediate()     );
        CODE("exit",  c -> c.unnest()                     );       /// marker to exit interpreter
        CODE("exec",  c -> dict.get(ss.pop()).nest()      );
        CODE("create",c -> {
            dict.add(word());
            Code w = new Code(_dovar, "var", 0);
            LIT(w);
            w.qf.drop();
        });
        IMMD("does>", c -> {                                       /// n --
            Code w = new Code(_dodoes, "does>");
            ADDW(w);
            w.token = dict.tail().token;                           /// * point to new word
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
        /// @note:
        ///   allot allocate elements in a word's q[] array
        ///   to access, both indices to word itself and to q array are needed
        ///   'th' a word that compose i_w, a 32-bit value, the 16 high bits
        ///   serves as the q index and lower 16 lower bit as word index
        ///   so a variable (array with 1 element) can be access as usual
        ///
        /// @{
        CODE("@",  c -> ss.push(dict.getv(ss.pop()))      );       /// w -- n
        CODE("!",  c -> {                                          /// n w --
            int i_w = ss.pop(), n = ss.pop();
            dict.setv(i_w, n);
            if (i_w == 0) base = n;
        });
        CODE("+!", c -> {                                          /// n w --
            int  i_w = ss.pop(), n = ss.pop();
            dict.setv(i_w, dict.getv(i_w) + n);
        });
        CODE("?",  c -> io.dot(IO.OP.DOT, dict.getv(ss.pop())));   /// w --
        CODE(",",  c -> dict.tail().comma(ss.pop())       );       /// n -- 
        CODE("cells",c -> { /* backward compatible */ }   );       /// --
        CODE("allot",c -> {                                        /// n --
            int  n = ss.pop(); 
            Code w = dict.tail();
            for (int i=0; i < n; i++) w.comma(0);
        });
        CODE("th", c -> {                                          /// w i -- i_w
            int i = ss.pop() << 16; ss.push(i | ss.pop());         /// i.e. 4 v 2 th !
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
        CODE("see",   c -> io.see(tick(), base, 0)                 );
        CODE("clock", c -> ss.push((int)System.currentTimeMillis()));
        CODE("rnd",   c -> ALU(a -> io.rnd(a))                     );
        CODE("depth", c -> ss.push(ss.size())                      );
        CODE("r",     c -> ss.push(rs.size())                      );
        IMMD("include",                                            /// include an OS file
             c -> io.load(io.next_token(), ()->{ return outer(); })
        );
        CODE("included",c -> {                                     /// include a file (programmable)
             int n = ss.pop(), i_w = ss.pop();
             String s = i_w < 0 ? io.pad() : dict.str(i_w);
             io.load(s, ()->{ return outer(); });
        });
        CODE("ok",    c -> io.mstat()                              );
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
