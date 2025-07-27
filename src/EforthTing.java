import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.util.function.*;
import java.util.List;

public class EforthTing {
    final static String VERSION="2.08";
    static final Font font = new Font("Monospaced", Font.PLAIN, 14);
    static TextArea input  = new TextArea("",10,60);
    static TextArea output = new TextArea("ooeForth "+VERSION+"\n",10,120);
    static Frame frame     = new Frame("ooeForth v"+VERSION);
    static Scanner in;
    static Stack<Integer>  ss   = new Stack<>();
    static Stack<Integer>  rs   = new Stack<>();
    static ForthList<Code> dict = null;
    static boolean         compiling = false;
    static int             base = 10;
    static public class ForthList<T> extends ArrayList<T> {
        ForthList()          {}                             // empty list
        ForthList(List<T> a) { super(a); }                  // initialize with a List (for primitives)
        T head()             { return get(0);        }
        T tail()             { return get(size()-1); }
        T tail(int offset)   { return get(size()-offset); }
        T find(String s, Predicate<T> m) {
            for (int i=size()-1; i>=0; i--) {               // search array from tail to head
                T w = get(i);
                if (m.test(w)) return w;}
            return null;}
        ForthList<T> set_head(T w) { set(0, w); return this; }
        ForthList<T> remove_head() { remove(0); return this; }
        ForthList<T> remove_tail() { remove(size()-1); return this; }}
    // forth words constructor
    static class Code {                                      // one size fits all objects
        static int fence=0;
        static Consumer<Code> get_xt(String n) {
            Code w=dict.find(n,wx->n.equals(wx.name));
            return w==null ? null : w.xt;}
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
        public Code(String n, int d) {xt=get_xt(name=n);qf.add(d);}
        public Code(String n, String s) {xt=get_xt(name=n);str=s;}
        public void nest() {
            if (xt!=null) {xt.accept(this); return;}        // execute primitive word
            for(Code w:pf) {
                try { w.nest(); }                           // inner interpreter
                catch (ArithmeticException e) {}}}
        public void see(int dp) {
            Consumer<String> tab = s->{
                int i=dp; output.append("\n"); 
                while (i-->0) { output.append("  ");} output.append(s);};
            tab.accept("[ "+name+" "); pf.forEach(w->w.see(dp+1));
            if (pf1.size()>0) { tab.accept("1--"); pf1.forEach(w->w.see(dp+1));}
            if (pf2.size()>0) { tab.accept("2--"); pf2.forEach(w->w.see(dp+1));}
            if (qf.size()>0)  { output.append(" ="); qf.forEach(i->output.append(i.toString()+" "));}
            if (str!=null)    output.append(" =\""+str.substring(1)+"\" ");
            output.append("]");}}
    static class Immd extends Code {
        public Immd(String n, Consumer<Code> f) { super(n, f); immd=true; }}
    // outer interpreter
    static void ss_dump() { output.append("< ");
    	for (int i:ss) output.append(Integer.toString(i,base)+" ");
    	output.append(">ok\n"); }
    static Code compile(Code w) {dict.tail().pf.add(w); return w;}
    public static void outerInterpreter() {
        while(in.hasNext()) {                               // parse input
            String idiom=in.next();
            Code w=dict.find(idiom,wx->idiom.equals(wx.name));
            if(w !=null) {                                  // word found
                if((!compiling) || w.immd) {
                    try { w.nest(); }                       // execute
                    catch (Exception e) {output.append(e.toString());}}
                else compile(w);}
            else {
                try {int n=Integer.parseInt(idiom, base);        // not word, try number
                    if (compiling) compile(new Code("dolit",n)); // compile integer literal
                    else ss.push(n);}                            // or push number on stack
                catch (NumberFormatException  ex) {         // catch number errors
                    output.append(idiom + "? ");
                    compiling=false; ss.clear();}}}
        if (!compiling) ss_dump(); }
    static public String word(String delim) {
        var d=in.delimiter(); in.useDelimiter(delim);       // change delimiter
        String s=in.next();   in.useDelimiter(d); in.next();// restore delimiter
        return s;}
    static public Code find_next() {
        String s=in.next();
        Code   w= dict.find(s, wx->s.equals(wx.name));
        if (w==null) throw new NumberFormatException();
        return w;
    }
    static public ForthList<Code> primitives = new ForthList<>(Arrays.asList(
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
        new Code("+",    c->ss.push(ss.pop()+ss.pop())),
        new Code("-",    c->{int n=ss.pop();ss.push(ss.pop()-n);}),
        new Code("*",    c->ss.push(ss.pop()*ss.pop())),
        new Code("/",    c->{int n=ss.pop();ss.push(ss.pop()/n);}),
        new Code("*/",   c->{int n=ss.pop();ss.push(ss.pop()*ss.pop()/n);}),
        new Code("*/mod",c->{int n=ss.pop();int m=ss.pop()*ss.pop();
            ss.push(m%n);ss.push(m/n);}),
        new Code("mod",  c->{int n=ss.pop();ss.push(ss.pop()%n);}),
        // ALU binary
        new Code("and",  c->ss.push(ss.pop()&ss.pop())),
        new Code("or",   c->ss.push(ss.pop()|ss.pop())),
        new Code("xor",  c->ss.push(ss.pop()^ss.pop())),
        new Code("negate",c->ss.push(-ss.pop())),
        new Code("invert",c->ss.push(~ss.pop())),
        new Code("abs",  c->ss.push(Math.abs(ss.pop()))),
        // ALU logic
        new Code("0=",   c->ss.push((ss.pop()==0)?-1:0)),
        new Code("0<",   c->ss.push((ss.pop()<0)?-1:0)),
        new Code("0>",   c->ss.push((ss.pop()>0)?-1:0)),
        new Code("=",    c->{int n=ss.pop();ss.push((ss.pop()==n)?-1:0);}),
        new Code(">",    c->{int n=ss.pop();ss.push((ss.pop()>n)?-1:0);}),
        new Code("<",    c->{int n=ss.pop();ss.push((ss.pop()<n)?-1:0);}),
        new Code("<>",   c->{int n=ss.pop();ss.push((ss.pop()!=n)?-1:0);}),
        new Code(">=",   c->{int n=ss.pop();ss.push((ss.pop()>=n)?-1:0);}),
        new Code("<=",   c->{int n=ss.pop();ss.push((ss.pop()<=n)?-1:0);}),
        // output
        new Code("base", c->ss.push(0)),
        new Code("hex",  c->dict.head().pf.head().qf.set_head(base=16)),
        new Code("decimal",c->dict.head().pf.head().qf.set_head(base=10)),
        new Code("cr",   c->output.append("\n")),
        new Code(".",    c->output.append(Integer.toString(ss.pop(),base)+" ")),
        new Code(".r",   c->{
            int n=ss.pop(); String s=Integer.toString(ss.pop(),base);
            for(int i=0;i+s.length()<n;i++)output.append(" ");
            output.append(s+" ");}),
        new Code("u.r",  c->{int n=ss.pop();
            String s=Integer.toString(ss.pop()&0x7fffffff,base);
            for(int i=0;i+s.length()<n;i++)output.append(" ");
            output.append(s+" ");}),
        new Code("key",  c->ss.push((int) in.next().charAt(0))),
        new Code("emit", c->{char b=(char)(int)ss.pop();output.append(""+b);}),
        new Code("space",c->{output.append(" ");}),
        new Code("spaces",c->{int n=ss.pop();for(int i=0;i<n;i++)output.append(" ");}),
        // literals
        new Code("[",    c->compiling=false),
        new Code("]",    c->compiling=true),
        new Code("'",    c->{Code w= find_next(); ss.push(w.token);}),
        new Code("dolit",c->ss.push(c.qf.head())),                        // integer literal
        new Code("dostr",c->{ss.push(c.token);ss.push(c.str.length());}), // string literal
        new Immd("s\"",  c->{                                             // -- w a
            String s=word("\""); if (s==null) return;
            if (compiling) {
                compile(new Code("dostr",s)).token = dict.tail().token;   // literal=s
            }
            else {ss.push(-1); ss.push(s.length());}}),
        new Code("type", c->{
            ss.pop(); Code w = dict.get(ss.pop()).pf.head();
            output.append(w.str);}),
        new Code("dotstr",c->{output.append(c.str);}),
        new Immd(".\"",  c->compile(new Code("dotstr",word("\"")))),      // literal=s
        new Immd("(",    c->word("\\)")),
        new Immd(".(",   c->output.append(word("\\)"))),
        new Immd("\\",   c->word("\n")),
        // structure: if else then
        new Code("branch",c->{
            for (var w:(ss.pop()!=0) ? c.pf : c.pf1) w.nest();}),
        new Immd("if",   c->{
            compile(new Code("branch", false));
            dict.add(new Code("temp", false));}),
        new Immd("else", c->{
            Code last=dict.tail(2).pf.tail(), temp=dict.tail();
            last.pf.addAll(temp.pf);
            temp.pf.clear();
            last.stage=1;}),
        new Immd("then", c->{
            Code last=dict.tail(2).pf.tail(), temp=dict.tail();
            if (last.stage==0) {
                last.pf.addAll(temp.pf);
                dict.remove_tail();}
            else {
                last.pf1.addAll(temp.pf);
                if (last.stage==1) { dict.remove_tail();}
                else temp.pf.clear();}}),
        // loops
        new Code("loops",c->{
            while (true) {
                for (var w:c.pf) w.nest();
                if (c.stage==0 && ss.pop()!=0) break;   // until
                if (c.stage==1) continue;               // again
                if (c.stage==2 && ss.pop()==0) break;   // while repeat
                for (var w:c.pf1) w.nest();}}),
        new Immd("begin",c->{
            compile(new Code("loops", false));
            dict.add(new Code("temp", false));}),
        new Immd("while",c->{
            Code last=dict.tail(2).pf.tail(), temp=dict.tail();
            last.pf.addAll(temp.pf);
            temp.pf.clear();
            last.stage=2;}),
        new Immd("repeat",c->{
            Code last=dict.tail(2).pf.tail(), temp=dict.tail();
            last.pf1.addAll(temp.pf);
            dict.remove_tail();}),
        new Immd("again",c->{
            Code last=dict.tail(2).pf.tail(), temp=dict.tail();
            last.pf.addAll(temp.pf);
            last.stage=1;
            dict.remove_tail();}),
        new Immd("until",c->{
            Code last=dict.tail(2).pf.tail(), temp=dict.tail();
            last.pf.addAll(temp.pf);
            dict.remove_tail();}),
        // for next
        new Code("cycles",c->{
            do { for (var w:c.pf) w.nest();
            } while (c.stage==0 && rs.push(rs.pop()-1)>=0);
            while (c.stage>0) {
                for(var w:c.pf2) w.nest();
                if (rs.push(rs.pop()-1)<0) break;
                for(var w:c.pf1) w.nest();}
            rs.pop();}),
        new Immd("for",c->{
            compile(new Code(">r", false));
            compile(new Code("cycles", false));
            dict.add(new Code("temp", false));}),
        new Immd("aft",c->{
            Code last=dict.tail(2).pf.tail(), temp=dict.tail();
            last.pf.addAll(temp.pf);
            temp.pf.clear();
            last.stage=3;}),
        new Immd("next",c->{
            Code last=dict.tail(2).pf.tail(), temp=dict.tail();
            if (last.stage==0) last.pf.addAll(temp.pf);
            else last.pf2.addAll(temp.pf);
            dict.remove_tail();}),
        // defining words
        new Code("dodoes", c->{
            boolean hit=false;
            for (var w : dict.get(c.token).pf) {
                output.append("\n"+w.name+" h="+hit);
                if (hit) compile(w);
                else if (w.name=="dodoes") hit=true;
            }}),
        new Code("exit",c->{throw new ArithmeticException();}),      // exit interpreter
        new Code("exec",c->{int n=ss.pop();dict.get(n).nest();}),
        new Code(":",c->{                                            // colon
            String s=in.next();
            dict.add(new Code(s));
            compiling=true;}),
        new Immd(";",     c->compiling=false),                       // semicolon
        new Code("docon", c->ss.push(c.qf.head())),                  // integer literal
        new Code("dovar", c->ss.push(c.token)),                      // string literal
        new Code("variable",c->{
            String s=in.next(); dict.add(new Code(s));
            compile(new Code("dovar",0)).token = dict.tail().token;}),
        new Code("constant",c->{  // n --
            String s=in.next(); dict.add(new Code(s));
            compile(new Code("docon",ss.pop())).token = dict.tail().token;}),
		// memory access
        new Code("@",c->{  // w -- n
            Code v=dict.get(ss.pop()).pf.head();
            ss.push(v.qf.head());}),
        new Code("!",c->{  // n w --
            Code v=dict.get(ss.pop()).pf.head();
            v.qf.set_head(ss.pop());}),
        new Code("+!",c->{  // n w --
            Code v=dict.get(ss.pop()).pf.head();
            int n=v.qf.head(); n+=ss.pop();
            v.qf.set_head(n);}),
        new Code("?",c->{
            Code v=dict.get(ss.pop()).pf.head();
            output.append(Integer.toString(v.qf.head()));}),
        new Code("array@",c->{  // w i -- n
            int i=ss.pop(); Code v=dict.get(ss.pop()).pf.head();
            ss.push(v.qf.get(i));}),
        new Code("array!",c->{  // n w i --
            int i=ss.pop(); Code v=dict.get(ss.pop()).pf.head();
            v.qf.set(i,ss.pop());}),
        new Code(",",c->dict.tail().pf.head().qf.add(ss.pop())),
        new Code("allot",c->{  // n --
            int  n=ss.pop();
            Code v=dict.tail().pf.head();
            for(int i=0;i<n;i++) v.qf.add(0);}),
        // metacompiler
        new Code("create",c->{
            String s=in.next(); dict.add(new Code(s));               // create variable
            compile(new Code("dovar",0)).token = dict.tail().token;
            dict.tail().pf.head().qf.remove_head();}),               // no value
        new Immd("does>",c->{ // n --
            compile(new Code("dodoes", false)).token=dict.tail().token;}),
        new Code("to",   c->{
            Code w=find_next();
            dict.get(w.token).pf.head().qf.set_head(ss.pop());}),
        new Code("is",c->{                                           // w -- , execute only
            Code src=dict.get(ss.pop());                             // source word
            Code w=find_next(); dict.get(w.token).pf=src.pf;}),
        // tools
        new Code("here", c->{ss.push(dict.tail().token);}),
        new Code("words",c->{
            for (int i=dict.size()-1,sz=0; i>=0; --i) {
                Code w=dict.get(i);
                output.append(w.name+"  ");
                sz += w.name.length() + 2;
                if (sz>64) {output.append("\n");sz=0;}}}),
        new Code(".s",   c->{
            for(int n:ss) output.append(Integer.toString(n,base)+" ");}),
        new Code("see",  c->{Code w=find_next(); w.see(0);}),
        new Code("time", c->{
            LocalTime now=LocalTime.now();
            output.append(now.toString());}),
        new Code("clock",c->{ss.push((int)System.currentTimeMillis());}),
        new Code("ms",   c->{                                       // n -- sleep millisec
            try { Thread.sleep(ss.pop());}
            catch (Exception e) { output.append(e.toString());}}),
        new Code("forget",c->{
            Code w=find_next();
            Code b=dict.find("boot", wx->"boot".equals(wx.name));
            int  n=Math.max(w.token, b.token+1);
            dict.subList(n, dict.size()).clear();}),
        new Code("boot",c->{
            Code b = dict.find(null, wx->"boot".equals(wx.name));
            dict.subList(b.token+1, dict.size()).clear();})));
	public static void main(String args[]) {
		dict = primitives;							// setup dictionary
        Code b = new Code("dovar", base=10);        // use dict[0] as base storage
        b.token = 0;
        dict.head().pf.add(b);                      
		System.out.println("ooeForth"+VERSION+"\n");
		input.setFont(font);
		output.setFont(font);
		frame.add(input, BorderLayout.EAST);
		frame.add(output, BorderLayout.WEST);
		frame.setSize(1000, 700);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {System.exit(0);}});
		input.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent ke) {
                char c = ke.getKeyChar();
				if (c <= 13 && c != 8) {
					in = new Scanner(input.getText());
					outerInterpreter();
					input.setText("");
					in.close();}}});}}
