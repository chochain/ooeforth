///
/// @file
/// @brief - IO module
///
package eforth;

import java.io.*;
import java.util.*;
import java.util.function.*;
///
///> console input/output
///
public class IO {
    enum OP { CR, BL, EMIT, DOT, UDOT, DOTR, UDOTR, SPCS }
        
    Scanner         in   = null;
    StringTokenizer tok  = null;
    PrintWriter     out  = null;
    String          pad;

    public IO(InputStream i, PrintStream o) {
        in  = new Scanner(i);
        out = new PrintWriter(o, true);
    }
    public boolean readline() {
        boolean t   = in.hasNextLine();                ///< any more line to read?
        String  tib = t ? in.nextLine() : null;        ///< feed input line
        pstr("more="+t+" tib="+tib);
        return t;
    }
    public void pstr(String s)   { out.print(s); out.flush(); }
    public void pchr(int n)      { out.print(Character.toChars(n)); }
    public void err(Exception e) { e.printStackTrace(); }
    
    String next_token()       { return in.next(); }    ///< fetch next token from in stream
    String scan(String delim) {
        var d = in.delimiter();                        ///< keep delimiter (space)
        in.useDelimiter(delim); pad = in.next();       /// * read to delimiter (into pad)
        pstr("pad="+pad);
        in.useDelimiter(d);     String x=in.next();    /// * restore and skip off spaces
        pstr(" x="+x+"\n");
        return pad;
    }
    ///
    ///> IO methods
    ///
    int    key() { return (int)tok.nextToken().charAt(0); }
    String pad() { return pad; }
    String itoa(int n, int base) { return Integer.toString(n, base); }
    void spaces(int n) {
        for (int i=0; i < Math.max(1,n); i++) pstr(" ");
    }
    void dot(OP op, int n, int r, int base) {
        switch (op) {
        case CR:   pstr("\n");                     break;
        case BL:   pchr(0x20);                     break;
        case EMIT: pchr(n);                        break;
        case DOT:  pstr(itoa(n ,base) + " ");      break;
        case UDOT: pstr(itoa(n&0x7fffffff, base)); break;
        case DOTR: {
            String s = itoa(n, base);
            spaces(r - s.length());
            pstr(s);
        } break;
        case UDOTR: {
            String s = itoa(n & 0x7fffffff, base);
            spaces( r - s.length());
            pstr(s);
        } break;
        }
    }
    void dot(OP op, int n, int r) { dot(op, n, r, 10); }
    void dot(OP op, int n)        { dot(op, n, 0, 10); }
    void cr()                     { dot(OP.CR, 0, 0, 10); }
    void bl()                     { dot(OP.BL, 0, 0, 10); }
    ///
    ///> ok - stack dump and OK prompt
    ///
    void ss_dump(Stack<Integer> ss, int base) {
        cr();
        for (int n : ss) pstr(itoa(n, base)+" ");
    }
    void words(Dict dict) {
        int i=0, sz = 0; 
        for (var w : dict) {
            pstr("  " + w.name);
            sz += w.name.length() + 2;                         /// width control
            if (sz > 64) { cr(); sz = 0; }
        }
    }
    void see(Code c, int dp) {
        Consumer<String> tab = s->{
            int i = dp;
            cr();
            while (i-->0) { pstr("  "); } pstr(s);
        };
        tab.accept((dp == 0 ? ": " : "")+c.name+" ");
        c.pf.forEach(w -> see(w, dp+1));
        if (c.p1.size() > 0) {
            tab.accept("( 1-- )");  c.p1.forEach(w -> see(w, dp+1));
        }
        if (c.p2.size() > 0) {
            tab.accept("( 2-- )");  c.p2.forEach(w -> see(w, dp+1));
        }
        if (c.qf.size() > 0)  {
            pstr(" \\ ="); c.qf.forEach(i -> pstr(i.toString()+" "));
        }
        if (c.str != null)  pstr(" \\ =\""+c.str+"\" ");
        if (dp == 0) pstr("\n;");
    }
}
