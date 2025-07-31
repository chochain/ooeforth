import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.function.*;
import java.time.LocalTime;
import java.util.List;
import java.io.*;

public class EforthTing {
    static Scanner         in;
    static PrintStream     out;
    static Stack<Integer>  ss   = new Stack<>();
    static Stack<Integer>  rs   = new Stack<>();
    static ForthList<Code> dict = null;
    static boolean         compi= false;
    static int             base = 10;
    static String          pad;                             // temp string holder
    static class ForthList<T> extends ArrayList<T> {
        ForthList()          {}                             // empty list
        ForthList(List<T> a) { super(a); }                  // initialize with a List (for primitives)
        T tail()             { return get(size()-1); }
        T scan(String s, Predicate<T> m) {
            for (int i=size()-(compi ? 2 : 1); i>=0; i--) { // search array from tail to head
                T w = get(i); if (m.test(w)) return w;}
            return null;}
        void drop() { remove(size()-1); }}                  // remove last element
    // forth words constructors
    static class Code {                                      // one size fits all objects
        static int fence=0;
        static Consumer<Code> get_xt(String n) {
            Code w=dict.scan(n,wx->n.equals(wx.name));
            return w==null ? null : w.xt; }
        public int token=0;
        public String name;
        public Consumer<Code>     xt  = null;
        public ForthList<Code>    pf  = new ForthList<>();
        public ForthList<Code>    pf1 = new ForthList<>();
        public ForthList<Code>    pf2 = new ForthList<>();
        public ForthList<Integer> qf  = new ForthList<>();
        public int     stage = 0;
        public boolean immd  = false;
        public String  str;
        public Code(String n) {name=n;token=fence++;}       // colon word
        public Code(String n, Consumer<Code> f) {name=n; token=fence++; xt=f;}
        public Code(String n, boolean f) {xt=get_xt(name=n); if (f) token=fence++;}
        public Code(String n, String s) {xt=get_xt(name=n);str=s;}
        public void nest() {
            if (xt!=null) {xt.accept(this); return;}        // execute primitive word
            for(Code w:pf) {
                try { w.nest(); }                           // inner interpreter
                catch (ArithmeticException e) {break;} }}   // exit, 
        public void see(int dp) {
            Consumer<String> tab = s->{
                int i=dp; out.print("\n"); 
                while (i-->0) { out.print("  ");} out.print(s);};
            tab.accept((dp == 0 ? ": " : "")+name+" "); pf.forEach(w->w.see(dp+1));
            if (pf1.size()>0) { tab.accept("( 1-- )"); pf1.forEach(w->w.see(dp+1));}
            if (pf2.size()>0) { tab.accept("( 2-- )"); pf2.forEach(w->w.see(dp+1));}
            if (qf.size()>0)  { out.print(" \\ ="); qf.forEach(i->out.print(i.toString()+" "));}
            if (str!=null)    out.print(" \\ =\""+str.substring(1)+"\" ");
            if (dp == 0) out.print("\n;"); }}
    static class Immd extends Code {
        public Immd(String n, Consumer<Code> f) {super(n, f); immd=true;}}
    static class Var extends Code {
        public Var(int d, boolean var) {
            super(var ? "var" : "lit", false); qf.add(d);
            Consumer<Code> dovar = c->ss.push(c.token), dolit = c->ss.push(c.qf.get(0));
            xt = var ? dovar : dolit; }}
    // primitive methods
    static void unnest() {throw new ArithmeticException();}
    static Code find(String n) {return dict.scan(n,wx->n.equals(wx.name));}
    static Code create() {
        String n=in.next(); if (find(n)!=null) out.print(n+" reDef?");
        Code w=new Code(n); dict.add(w); return w;}
    static Code compile(Code w) {dict.tail().pf.add(w); return w;}
    static Code tgt() {return dict.get(dict.size()-2).pf.tail();}
    static ForthList<Integer> va(int i) {                   // variable array
        return dict.get(i<0?dict.size()+i:i).pf.get(0).qf;}
    static String to_s(int n) {return Integer.toString(n,base);}
    static void spaces(int n) {for(int i=0;i<Math.max(1,n);i++)out.print(" ");}
    static void ss_dump() {for(int i:ss) out.print(to_s(i)+" ");}
    // outer interpreter
    static int bool(boolean f) {return f?-1:0;}
    static void alu(Function<Integer, Integer> m) {ss.push(m.apply(ss.pop()));}
    static void alu(BiFunction<Integer, Integer, Integer> m) {
        int n=ss.pop(); ss.push(m.apply(ss.pop(),n));}
    static void outerInterpreter() {
        while (in.hasNext()) {
            String idiom=in.next();
            Code w=find(idiom);
            if(w!=null) {                                   // word found
                if(!compi || w.immd) {
                    try {w.nest();}                         // execute
                    catch (Exception e) {out.print(e.toString());}}
                else compile(w);}
            else {
                try {int n=Integer.parseInt(idiom, base);   // not word, try number
                    if (compi) compile(new Var(n, false));  // compile integer literal
                    else ss.push(n);}                       // or push number on stack
                catch (NumberFormatException  e) {          // catch number errors
                    out.print(idiom + "? ");
                    compi=false; ss.clear();}}}
        if (!compi) {out.print("< ");ss_dump();out.print(">ok\n");}}
    static String word(String delim) {
        var d=in.delimiter();
        in.useDelimiter(delim); pad=in.next();              // read upto delimiter
        in.useDelimiter(d); in.next();                      // skip off delimiter
        return pad;}
    static Code next_word() {
        Code w=find(in.next());
        if (w==null) throw new NumberFormatException();
        return w;}
    static ForthList<Code> primitives = new ForthList<>(Arrays.asList(
        // Stack ops
        new Code("dup",  c->ss.push(ss.peek())),
        new Code("drop", c->ss.pop()),
        new Code("swap", c->ss.add(ss.size()-2,ss.pop())),
        new Code("over", c->ss.push(ss.get(ss.size()-2))),
        new Code("rot",  c->ss.push(ss.remove(ss.size()-3))),
        new Code("-rot", c->{ss.push(ss.remove(ss.size()-3));ss.push(ss.remove(ss.size()-3));}),
        new Code("roll", c->{int i=ss.pop();int n=ss.remove(ss.size()-i-1);ss.push(n);}),
        new Code("pick", c->{int i=ss.pop();int n=ss.get(ss.size()-i-1);ss.push(n);}),
        new Code("nip",  c->ss.remove(ss.size()-2)),
        new Code("?dup", c->{int i=ss.peek(); if (i!=0) ss.push(i);}),
        // return stack
        new Code(">r",   c->rs.push(ss.pop())),
        new Code("r>",   c->ss.push(rs.pop())),
        new Code("r@",   c->ss.push(rs.peek())),
        new Code("i",    c->ss.push(rs.peek())),
		// double stack ops
        new Code("2dup", c->ss.addAll(ss.subList(ss.size()-2,ss.size()))),
        new Code("2over",c->ss.addAll(ss.subList(ss.size()-4,ss.size()-2))),
        new Code("2swap",c->{ss.push(ss.remove(ss.size()-4));ss.push(ss.remove(ss.size()-4));}),
        new Code("2drop",c->{ss.pop();ss.pop();}),
        // ALU arithmetic
        new Code("+",    c->alu((a,b)->a+b)),
        new Code("-",    c->alu((a,b)->a-b)),
        new Code("*",    c->alu((a,b)->a*b)),
        new Code("/",    c->alu((a,b)->a/b)),
        new Code("mod",  c->alu((a,b)->a%b)),
        new Code("*/",   c->{int n=ss.pop();ss.push(ss.pop()*ss.pop()/n);}),
        new Code("*/mod",c->{int n=ss.pop();int m=ss.pop()*ss.pop();
            ss.push(m%n);ss.push(m/n);}),
        // ALU binary
        new Code("and",  c->alu((a,b)->a&b)),
        new Code("or",   c->alu((a,b)->a|b)),
        new Code("xor",  c->alu((a,b)->a^b)),
        new Code("negate",c->alu(a->-a)),
        new Code("invert",c->alu(a->~a)),
        new Code("abs",  c->alu(a->Math.abs(a))),
        // ALU logic
        new Code("0=",   c->alu(a->bool(a==0))),
        new Code("0<",   c->alu(a->bool(a<0))),
        new Code("0>",   c->alu(a->bool(a>0))),
        new Code("=",    c->alu((a,b)->bool(a==b))),
        new Code(">",    c->alu((a,b)->bool(a>b))),
        new Code("<",    c->alu((a,b)->bool(a<b))),
        new Code("<>",   c->alu((a,b)->bool(a!=b))),
        new Code(">=",   c->alu((a,b)->bool(a>=b))),
        new Code("<=",   c->alu((a,b)->bool(a<=b))),
        // output
        new Code("base", c->ss.push(0)),
        new Code("hex",  c->va(0).set(0,base=16)),
        new Code("decimal",c->va(0).set(0,base=10)),
        new Code("bl",   c->ss.push(32)),
        new Code("cr",   c->out.print("\n")),
        new Code(".",    c->out.print(to_s(ss.pop())+" ")),
        new Code(".r",   c->{
            int n=ss.pop(); String s=to_s(ss.pop());
            spaces(n-s.length()); out.print(s);}),
        new Code("u.r",  c->{
            int n=ss.pop(); String s=to_s(ss.pop()&0x7fffffff);
            spaces(n-s.length()); out.print(s);}),
        new Code("type", c->{ss.pop(); int i = ss.pop();                  // str index
            out.print(i < 0 ? pad : dict.get(i).pf.get(0).str);}),
        new Code("key",  c->ss.push((int)word(" ").charAt(0))),
        new Code("emit", c->{char b=(char)(int)ss.pop();out.print(""+b);}),
        new Code("space",c->spaces(1)),
        new Code("spaces",c->spaces(ss.pop())),
        // literals
        new Code("[",    c->compi=false),
        new Code("]",    c->compi=true),
        new Code("'",    c->{Code w=next_word(); ss.push(w.token);}),
        new Code("dostr",c->{ss.push(c.token);ss.push(c.str.length());}), // string literal
        new Immd("s\"",  c->{                                             // -- w a
            String s=word("\""); if (s==null) return;
            if (!compi) {ss.push(-1); ss.push(s.length());}
            else compile(new Code("dostr",s)).token=dict.tail().token;}), // literal=s
        new Code("dotstr",c->{out.print(c.str);}),
        new Immd(".\"",  c->compile(new Code("dotstr",word("\"")))),      // literal=s
        new Immd("(",    c->word("\\)")),
        new Immd(".(",   c->out.print(word("\\)"))),
        new Immd("\\",   c->word("\n")),
        // structure: if else then
        new Code("branch",c->{
            for(var w:(ss.pop()!=0) ? c.pf : c.pf1) w.nest();}),
        new Immd("if",   c->{
            compile(new Code("branch", false));
            dict.add(new Code("tmp", false));}),
        new Immd("else", c->{
            Code b=tgt(), tmp=dict.tail();
            b.pf.addAll(tmp.pf); tmp.pf.clear(); b.stage=1;}),
        new Immd("then", c->{
            Code b=tgt(), tmp=dict.tail();
            if (b.stage==0) {b.pf.addAll(tmp.pf); dict.drop();}
            else {
                b.pf1.addAll(tmp.pf);
                if (b.stage==1) { dict.drop();}
                else tmp.pf.clear();}}),
        // loops
        new Code("loops",c->{
            while (true) {
                for(var w:c.pf) w.nest();
                if (c.stage==0 && ss.pop()!=0) break;   // until
                if (c.stage==1) continue;               // again
                if (c.stage==2 && ss.pop()==0) break;   // while repeat
                for(var w:c.pf1) w.nest();}}),
        new Immd("begin",c->{
            compile(new Code("loops", false));
            dict.add(new Code("tmp", false));}),
        new Immd("while",c->{
            Code b=tgt(), tmp=dict.tail();
            b.pf.addAll(tmp.pf); tmp.pf.clear(); b.stage=2;}),
        new Immd("repeat",c->{
            Code b=tgt(), tmp=dict.tail();
            b.pf1.addAll(tmp.pf); dict.drop();}),
        new Immd("again",c->{
            Code b=tgt(), tmp=dict.tail();
            b.pf.addAll(tmp.pf); dict.drop(); b.stage=1;}),
        new Immd("until",c->{
            Code b=tgt(), tmp=dict.tail();
            b.pf.addAll(tmp.pf); dict.drop();}),
        // for next
        new Code("cycles",c->{
            do { for(var w:c.pf) w.nest();
            } while (c.stage==0 && rs.push(rs.pop()-1)>=0);
            while (c.stage>0) {
                for(var w:c.pf2) w.nest();
                if (rs.push(rs.pop()-1)<0) break;
                for(var w:c.pf1) w.nest();}
            rs.pop();}),
        new Immd("for",c->{
            compile(new Code(">r", false));
            compile(new Code("cycles", false));
            dict.add(new Code("tmp", false));}),
        new Immd("aft",c->{
            Code b=tgt(), tmp=dict.tail();
            b.pf.addAll(tmp.pf); tmp.pf.clear(); b.stage=3;}),
        new Immd("next",c->{
            Code b=tgt(), tmp=dict.tail();
            if (b.stage==0) b.pf.addAll(tmp.pf);
            else b.pf2.addAll(tmp.pf);
            dict.drop();}),
        // defining words
        new Code("dodoes", c->{
            var hit=false;
            for(var w:dict.get(c.token).pf) {
                if (hit) compile(w);
                else if (w.name=="dodoes") hit=true;}
            unnest();}),
        new Code("exit",c->unnest()),                                // exit interpreter
        new Code("exec",c->{int n=ss.pop();dict.get(n).nest();}),
        new Code(":",   c->{create(); compi=true;}),                 // colon
        new Immd(";",   c->compi=false),                             // semicolon
        new Code("variable",c->{
            create();
            compile(new Var(0, true)).token = dict.tail().token;}),
        new Code("constant",c->{  // n --
            create();
            compile(new Var(ss.pop(), false)).token = dict.tail().token;}),
		// memory access
        new Code("@",c->ss.push(va(ss.pop()).get(0))),               // w -- n
        new Code("!",c->{                                            // n w --
            int i=ss.pop(), v=ss.pop(); va(i).set(0,v);
            if (i==0) base=v;}),                                     // no ptr, too bad
        new Code("+!",c->{
            int i=ss.pop(); va(i).set(0,va(i).get(0)+ss.pop());}),   // n w --
        new Code("?",c->out.print(to_s(va(ss.pop()).get(0)))),
        new Code("array@",c->{                                       // a i -- n
            int i=ss.pop(); ss.push(va(ss.pop()).get(i));}),
        new Code("array!",c->{                                       // n a i --
            int i=ss.pop(), a=ss.pop(); va(a).set(i,ss.pop());}),
        new Code(",",c->va(-1).add(ss.pop())),
        new Code("allot",c->{
           for(int i=0, n=ss.pop();i<n;i++) va(-1).add(0);}),
        // metacompiler
        new Code("create",c->{
            create();                                                // create variable
            compile(new Var(0, true)).token = dict.tail().token;
            va(-1).drop();}),                                        // no value
        new Immd("does>",c->
            compile(new Code("dodoes", false)).token=dict.tail().token),
        new Code("to",   c->va(next_word().token).set(0,ss.pop())),
        new Code("is",c->{                                           // w -- , execute only
            Code src=dict.get(ss.pop());                             // source word
            Code w=next_word(); dict.get(w.token).pf=src.pf;}),
        // tools
        new Code("here", c->{ss.push(dict.tail().token);}),
        new Code("words",c->{
            for(int i=dict.size()-1,sz=0; i>=0; --i) {
                Code w=dict.get(i);
                out.print(w.name+"  ");
                sz += w.name.length() + 2;
                if (sz>64) {out.print("\n");sz=0;}}}),
        new Code(".s",   c->ss_dump()),
        new Code("see",  c->next_word().see(0)),
        new Code("time", c->out.print(LocalTime.now().toString())),
        new Code("clock",c->ss.push((int)System.currentTimeMillis())),
        new Code("ms",   c->{                                       // n -- sleep millisec
            try {Thread.sleep(ss.pop());}
            catch (Exception e) {out.print(e.toString());}}),
        new Code("forget",c->{
            int n=Math.max(next_word().token, find("boot").token+1);
            dict.subList(n, dict.size()).clear();}),
        new Code("boot",c->dict.subList(find("boot").token+1, dict.size()).clear())));
	public static void main(String args[]) {
		dict = primitives;							 // setup dictionary
        Var b = new Var(base=10, true); b.token = 0; // use dict[0] as base storage
        dict.get(0).pf.add(b);
        String APP_NAME  = "ooeForth 2.08";
        JTextArea input  = new JTextArea("",10,80);  // GUI section
        JTextArea output = new JTextArea(APP_NAME+'\n',10,80);
        JScrollPane iscl = new JScrollPane(input);
        JScrollPane oscl = new JScrollPane(output);
        Font font = new Font("Monospaced", Font.PLAIN, 14);
		input.setFont(font); output.setFont(font);
        JFrame frame = new JFrame(APP_NAME);
        frame.setLayout(new BorderLayout());
        frame.add(iscl, BorderLayout.EAST);	frame.add(oscl, BorderLayout.WEST); 
		frame.pack(); frame.setMinimumSize(new Dimension(1000,700));
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {System.exit(0);}});
        input.requestFocusInWindow();
		input.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent ke) {
                char c = ke.getKeyChar(); if (c > 13 || c == 8) return;
                try (Scanner sc = in = new Scanner(input.getText())) { // auto-close
                    outerInterpreter(); input.setText(""); }}});
        class GuiPrintStream extends PrintStream {   // redirect to TextArea
            GuiPrintStream(OutputStream o) {super(o);}
            @Override public void print(String s) {
                output.append(s);
                output.setCaretPosition(output.getDocument().getLength());}};
        out = new GuiPrintStream(System.out);}}      // pipe output to GUI
