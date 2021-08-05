import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.util.function.*;
import java.util.List;

public class EforthTing {
    final static String VERSION="2.05";
    final static Font font= new Font("Monospaced", Font.PLAIN, 12);
    static Scanner in;
    static Stack<Integer> stack=new Stack<>();
    static Stack<Integer> rstack=new Stack<>();
    static ForthList<Code> dictionary=null;
    static boolean compiling=false;
    static int base=10;
    static int wp,ip;
    static TextArea input = new TextArea("words",10,50);
    static TextArea output = new TextArea("ooeForth "+VERSION+"\n",10,80);
    static Frame frame = new Frame("ooeForth v"+VERSION);
    static void setup_dictionary() {
        dictionary = primitives;
        final String immd[]= {
                ";","(","$\"","\\",".(",".\"",
                "aft","again","begin","else","for","if",
                "next","repeat","then","until","while"};
        for (String s: immd) {                                   // set immediate flag
            dictionary.find(s,w->s.equals(w.name)).immediate();}}
    public static void main(String args[]) {
        System.out.println("ooeForth"+VERSION+"\n");
        setup_dictionary();
        // GetKeyChar
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
                char keyChar = ke.getKeyChar();
                if (keyChar <= 13) {
                    in = new Scanner(input.getText());
                    outerInterpreter();
                    for(int n:stack) System.out.print(Integer.toString(n,base)+" ");
                    System.out.print(">ok\n");
                    input.setText("");
                    in.close();}}});}
    // outer interpreter
    public static void outerInterpreter() {
        while(in.hasNext()) {                               // parse input
            String idiom=in.next();
            Code newWordObject=dictionary.find(idiom,wx->idiom.equals(wx.name));
            if(newWordObject !=null) {                      // word found
                if((!compiling) || newWordObject.immediate) {
                    try { newWordObject.exec(); }           // execute
                    catch (Exception e) {output.append(e.toString());}}
                else dictionary.tail().addCode(newWordObject);}
            else {
                try {int n=Integer.parseInt(idiom, base);   // not word, try number
                    if (compiling) {                        // compile integer literal
                        dictionary.tail().addCode(new Code("dolit",n));}                
                    else stack.push(n);}                    // or push number on stack
                catch (NumberFormatException  ex) {         // catch number errors
                    output.append(idiom + "? ");
                    compiling=false; stack.clear();}}}
        for(int n:stack) output.append(Integer.toString(n,base)+" ");
        output.append(">ok\n");}
    static public String next_idiom(String delim) {
        var d=in.delimiter(); in.useDelimiter(delim);       // change delimiter
        String s=in.next();   in.useDelimiter(d); in.next(); // restore delimiter
        return s;}
    static public class ForthList<T> extends ArrayList<T> {
        ForthList()        {}                               // empty list
        ForthList(List a)  { super(a); }                    // initialize with a List (for primitives)
        T head()           { return get(0); }
        T tail()           { return get(size()-1); }
        T tail(int offset) { return get(size()-offset); }
        T find(String s, Predicate<T> m) {
            for (int i=size()-1; i>=0; i--) {               // search array from tail to head
                T w = get(i);
                if (m.test(w)) return w;}
            return null;}
        ForthList<T> set_head(T w) { set(0, w); return this; }
        ForthList<T> remove_head() { remove(0); return this; }
        ForthList<T> remove_tail() { remove(size()-1); return this; }}
    // forth words constructor
    static class Code {                                 // one size fits all objects
        static int fence=0;
        static Consumer<Code> get_xt(String n) {
            Code w=dictionary.find(n,wx->n.equals(wx.name));
            return w==null ? null : w.xt;}
        public int token=0;
        public String name;
        public Consumer<Code> xt = null;
        public ForthList<Code> pf=new ForthList<>();
        public ForthList<Code> pf1=new ForthList<>();
        public ForthList<Code> pf2=new ForthList<>();
        public ForthList<Integer> qf=new ForthList<>() ;
        public int stage=0;
        public boolean immediate=false;
        public String literal;
        public Code(String n) {name=n;token=fence++;}           // colon word
        public Code(String n, Consumer<Code> f) {name=n; token=fence++; xt=f;}
        public Code(String n, boolean f) {xt=get_xt(name=n); if (f) token=fence++;}
        public Code(String n, int d) {xt=get_xt(name=n);qf.add(d);}
        public Code(String n, String l) {xt=get_xt(name=n);literal=l;}
        public Code immediate() {immediate=true; return this;}
        public void exec() {
            if (xt!=null) {xt.accept(this); return;}           // execute primitive word
            rstack.push(wp); rstack.push(ip);                   // run colon words
            wp=token; ip=0;                                     // point to current object
            for(Code w:pf) {
                try { w.exec();ip++;}                           // inner interpreter
                catch (ArithmeticException e) {}}
            ip=rstack.pop(); wp=rstack.pop(); }
        public Code addCode(Code w) {this.pf.add(w);return this;}
        public void see(int dp) {
            Consumer<String> tab = s->{
                int i=dp; output.append("\n"); 
                while (i-->0) { output.append("  ");} output.append(s);};
            tab.accept("[ "+name+" "); pf.forEach(w->w.see(dp+1));
            if (pf1.size()>0) { tab.accept("1--"); pf1.forEach(w->w.see(dp+1));}
            if (pf2.size()>0) { tab.accept("2--"); pf2.forEach(w->w.see(dp+1));}
            if (qf.size()>0)  { output.append(" ="); qf.forEach(i->output.append(i.toString()+" "));}
            output.append("]");}}
    static public ForthList<Code> primitives = new ForthList<>(Arrays.asList(
        new Code("dup", c->stack.push(stack.peek())),
        new Code("over",c->stack.push(stack.get(stack.size()-2))),
        new Code("2dup",c->stack.addAll(stack.subList(stack.size()-2,stack.size()))),
        new Code("2over",c->stack.addAll(stack.subList(stack.size()-4,stack.size()-2))),
        new Code("4dup",c->stack.addAll(stack.subList(stack.size()-4,stack.size()))),
        new Code("swap",c->stack.add(stack.size()-2,stack.pop())),
        new Code("rot",c->stack.push(stack.remove(stack.size()-3))),
        new Code("-rot",c->{stack.push(stack.remove(stack.size()-3));stack.push(stack.remove(stack.size()-3));}),
        new Code("2swap",c->{stack.push(stack.remove(stack.size()-4));stack.push(stack.remove(stack.size()-4));}),
        new Code("pick",c->{int i=stack.pop();int n=stack.get(stack.size()-i-1);stack.push(n);}),
        new Code("roll",c->{int i=stack.pop();int n=stack.remove(stack.size()-i-1);stack.push(n);}),
        new Code("drop",c->stack.pop()),
        new Code("nip",c->stack.remove(stack.size()-2)),
        new Code("2drop",c->{stack.pop();stack.pop();}),
        new Code(">r",c->rstack.push(stack.pop())),
        new Code("r>",c->stack.push(rstack.pop())),
        new Code("r@",c->stack.push(rstack.peek())),
        new Code("push",c->rstack.push(stack.pop())),
        new Code("pop",c->stack.push(rstack.pop())),
        // math
        new Code("+",c->stack.push(stack.pop()+stack.pop())),
        new Code("-",c->{int n=stack.pop();stack.push(stack.pop()-n);}),
        new Code("*",c->stack.push(stack.pop()*stack.pop())),
        new Code("/",c->{int n=stack.pop();stack.push(stack.pop()/n);}),
        new Code("*/",c->{int n=stack.pop();stack.push(stack.pop()*stack.pop()/n);}),
        new Code("*/mod",c->{int n=stack.pop();int m=stack.pop()*stack.pop();
            stack.push(m%n);stack.push(m/n);}),
        new Code("mod",c->{int n=stack.pop();stack.push(stack.pop()%n);}),
        new Code("and",c->stack.push(stack.pop()&stack.pop())),
        new Code("or",c->stack.push(stack.pop()|stack.pop())),
        new Code("xor",c->stack.push(stack.pop()^stack.pop())),
        new Code("negate",c->stack.push(-stack.pop())),
        new Code("abs",c->stack.push(Math.abs(stack.pop()))),
        // logic
        new Code("0=",c->stack.push((stack.pop()==0)?-1:0)),
        new Code("0<",c->stack.push((stack.pop()<0)?-1:0)),
        new Code("0>",c->stack.push((stack.pop()>0)?-1:0)),
        new Code("=",c->{int n=stack.pop();stack.push((stack.pop()==n)?-1:0);}),
        new Code(">",c->{int n=stack.pop();stack.push((stack.pop()>n)?-1:0);}),
        new Code("<",c->{int n=stack.pop();stack.push((stack.pop()<n)?-1:0);}),
        new Code("<>",c->{int n=stack.pop();stack.push((stack.pop()!=n)?-1:0);}),
        new Code(">=",c->{int n=stack.pop();stack.push((stack.pop()>=n)?-1:0);}),
        new Code("<=",c->{int n=stack.pop();stack.push((stack.pop()<=n)?-1:0);}),
        // output
        new Code("base@",c->stack.push(base)),
        new Code("base!",c->base=stack.pop()),
        new Code("hex",c->base=16),
        new Code("decimal",c->base=10),
        new Code("cr",c->output.append("\n")),
        new Code(".",c->output.append(Integer.toString(stack.pop(),base)+" ")),
        new Code(".r",c->{
            int n=stack.pop(); String s=Integer.toString(stack.pop(),base);
            for(int i=0;i+s.length()<n;i++)output.append(" ");
            output.append(s+" ");}),
        new Code("u.r",c->{int n=stack.pop();
            String s=Integer.toString(stack.pop()&0x7fffffff,base);
            for(int i=0;i+s.length()<n;i++)output.append(" ");
            output.append(s+" ");}),
        new Code("key",c->stack.push((int) in.next().charAt(0))),
        new Code("emit",c->{char b=(char)(int)stack.pop();output.append(""+b);}),
        new Code("space",c->{output.append(" ");}),
        new Code("spaces",c->{int n=stack.pop();for(int i=0;i<n;i++)output.append(" ");}),
        // literals
        new Code("[",c->compiling=false),
        new Code("]",c->compiling=true),
        new Code("'",c->{String s=in.next();
            Code w = dictionary.find(s, wx->s.equals(wx.name));
            if (w==null) throw new  NumberFormatException();
            stack.push(w.token);}),
        new Code("dolit",c->stack.push(c.qf.head())),            // integer literal
        new Code("dostr",c->stack.push(c.token)),                // string literal
        new Code("$\"",c->{ // -- w a
            String s=next_idiom("\"");
            Code last=dictionary.tail().addCode(new Code("dostr",s)); // literal=s, 
            stack.push(last.token);stack.push(last.pf.size()-1);}),
        new Code("dotstr",c->{output.append(c.literal);}),
        new Code(".\"",c->
            dictionary.tail().addCode(new Code("dotstr",next_idiom("\"")))),// literal=s,
        new Code("(",c->next_idiom("\\)")),
        new Code(".(",c->output.append(next_idiom("\\)"))),
        new Code("\\",c->next_idiom("\n")),
        // structure: if else then
        new Code("branch",c->{
            for (var w:(stack.pop()!=0) ? c.pf : c.pf1) w.exec();}),
        new Code("if",c->{
            dictionary.tail().addCode(new Code("branch", false));
            dictionary.add(new Code("temp", false));}),
        new Code("else",c->{
            Code last=dictionary.tail(2).pf.tail(), temp=dictionary.tail();
            last.pf.addAll(temp.pf);
            temp.pf.clear();
            last.stage=1;}),
        new Code("then",c->{
            Code last=dictionary.tail(2).pf.tail(), temp=dictionary.tail();
            if (last.stage==0) {
                last.pf.addAll(temp.pf);
                dictionary.remove_tail();}
            else {
                last.pf1.addAll(temp.pf);
                if (last.stage==1) { dictionary.remove_tail();}
                else temp.pf.clear();}}),
        // loops
        new Code("loops",c->{
            while (true) {
                for (var w:c.pf) w.exec();
                if (c.stage==0 && stack.pop()!=0) break;   // until
                if (c.stage==1) continue;                  // again
                if (c.stage==2 && stack.pop()==0) break;   // while repeat
                for (var w:c.pf1) w.exec();}}),
        new Code("begin",c->{
            dictionary.tail().addCode(new Code("loops", false));
            dictionary.add(new Code("temp", false));}),
        new Code("while",c->{
            Code last=dictionary.tail(2).pf.tail(), temp=dictionary.tail();
            last.pf.addAll(temp.pf);
            temp.pf.clear();
            last.stage=2;}),
        new Code("repeat",c->{
            Code last=dictionary.tail(2).pf.tail(), temp=dictionary.tail();
            last.pf1.addAll(temp.pf);
            dictionary.remove_tail();}),
        new Code("again",c->{
            Code last=dictionary.tail(2).pf.tail(), temp=dictionary.tail();
            last.pf.addAll(temp.pf);
            last.stage=1;
            dictionary.remove_tail();}),
        new Code("until",c->{
            Code last=dictionary.tail(2).pf.tail(), temp=dictionary.tail();
            last.pf.addAll(temp.pf);
            dictionary.remove_tail();}),
        // for next
        new Code("cycles",c->{
            do { for (var w:c.pf) w.exec();
            } while (c.stage==0 && rstack.push(rstack.pop()-1)>=0);
            while (c.stage>0) {
                for(var w:c.pf2) w.exec();
                if (rstack.push(rstack.pop()-1)<0) break;
                for(var w:c.pf1) w.exec();}
            rstack.pop();}),
        new Code("for",c->{
            dictionary.tail()
                .addCode(new Code(">r", false))
                .addCode(new Code("cycles", false));
            dictionary.add(new Code("temp", false));}),
        new Code("aft",c->{
            Code last=dictionary.tail(2).pf.tail(), temp=dictionary.tail();
            last.pf.addAll(temp.pf);
            temp.pf.clear();
            last.stage=3;}),
        new Code("next",c->{
            Code last=dictionary.tail(2).pf.tail(), temp=dictionary.tail();
            if (last.stage==0) last.pf.addAll(temp.pf);
            else last.pf2.addAll(temp.pf);
            dictionary.remove_tail();}),
        // defining words
        new Code("exit",c->{throw new ArithmeticException(); }),     // exit interpreter
        new Code("exec",c->{int n=stack.pop();dictionary.get(n).exec();}),
        new Code(":",c->{                                            // colon
            String s=in.next();
            dictionary.add(new Code(s));
            compiling=true;}),
        new Code(";",c->compiling=false),                            // semicolon
        new Code("docon",c->stack.push(c.qf.head())),                // integer literal
        new Code("dovar",c->stack.push(c.token)),                    // string literal
        new Code("create",c->{
            String s=in.next(); dictionary.add(new Code(s));         // create variable
            Code last=dictionary.tail().addCode(new Code("dovar",0));
            last.pf.head().token=last.token;
            last.pf.head().qf.remove_head();}),
        new Code("variable",c->{
            String s=in.next(); dictionary.add(new Code(s));
            Code last=dictionary.tail().addCode(new Code("dovar",0));
            last.pf.head().token=last.token;}),
        new Code("constant",c->{  // n --
            String s=in.next(); dictionary.add(new Code(s));
            Code last=dictionary.tail().addCode(new Code("docon",stack.pop()));
            last.pf.head().token=last.token;}),
        new Code("@",c->{  // w -- n
            Code last=dictionary.get(stack.pop());
            stack.push(last.pf.head().qf.head());}),
        new Code("!",c->{  // n w --
            Code last=dictionary.get(stack.pop());
            last.pf.head().qf.set_head(stack.pop());}),
        new Code("+!",c->{  // n w --
            Code last=dictionary.get(stack.pop());
            int n=last.pf.head().qf.head(); n+=stack.pop();
            last.pf.head().qf.set_head(n);}),
        new Code("?",c->{  // w --
            Code last=dictionary.get(stack.pop());
            output.append(Integer.toString(last.pf.head().qf.head()));}),
        new Code("array@",c->{  // w a -- n
            int a=stack.pop();
            Code last=dictionary.get(stack.pop());
            stack.push(last.pf.head().qf.get(a));}),
        new Code("array!",c->{  // n w a --
            int a=stack.pop();
            Code last=dictionary.get(stack.pop());
            last.pf.head().qf.set(a,stack.pop());}),
        new Code(",",c->{ // n --
            Code last=dictionary.tail();
            last.pf.head().qf.add(stack.pop());}),
        new Code("allot",c->{  // n --
            int n=stack.pop();
            Code last=dictionary.tail();
            for(int i=0;i<n;i++) last.pf.head().qf.head();}),
        new Code("does",c->{ // n --
            Code last=dictionary.tail(), source=dictionary.get(wp);
            last.pf.addAll(source.pf.subList(ip+2,source.pf.size()));}),
        new Code("to",c->{                                               // n -- , compile only 
            Code last=dictionary.get(wp);ip++;                           // current colon word
            last.pf.get(ip++).pf.head().qf.set_head(stack.pop());}),     // next constant
        new Code("is",c->{                                               // w -- , execute only
            Code source=dictionary.get(stack.pop());         // source word
            String s=in.next();
            Code w=dictionary.find(s, wx->s.equals(wx.name));
            if (w==null) throw new  NumberFormatException();
            dictionary.get(w.token).pf=source.pf;}),
        // tools
        new Code("here",c->{stack.push(dictionary.tail().token);}),
        new Code("words",c->{
            for (int i=dictionary.size()-1,j=0; i>=0; --i) {
                Code w=dictionary.get(i);
                output.append(w.name+" "+w.token+" ");
                if (++j>9) {output.append("\n");j=0;}}}),
        new Code(".s",c->{
            for(int n:stack) output.append(Integer.toString(n,base)+" ");}),
        new Code("see",c->{
            String s=in.next();
            Code   w=dictionary.find(s, wx->s.equals(wx.name));
            if (w!=null) w.see(0); }),
        new Code("time",c->{
            LocalTime now=LocalTime.now();
            output.append(now.toString());}),
        new Code("ms",c->{ // n --
            try { Thread.sleep(stack.pop());}
            catch (Exception e) { output.append(e.toString());}}),
        new Code("forget",c->{
            String s=in.next();
            Code w=dictionary.find(s, wx->s.equals(wx.name));
            if (w==null) throw new  NumberFormatException();
            Code b=dictionary.find("boot", wx->"boot".equals(wx.name));
            int  n=Math.max(w.token, b.token+1);
            dictionary.subList(n, dictionary.size()).clear();}),
        new Code("boot",c->{
            Code b = dictionary.find(null, wx->"boot".equals(wx.name));
            dictionary.subList(b.token+1, dictionary.size()).clear();})));}

