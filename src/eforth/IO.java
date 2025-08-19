///
/// @file
/// @brief - IO module
///
package eforth;

import java.io.*;
import java.util.*;
import java.util.function.*;
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
///
///> console input/output
///
public class IO {
    private static final boolean DEBUG = false;
    enum OP { CR, BL, EMIT, DOT, UDOT, DOTR, UDOTR, SPCS }

    String      name;
    Scanner     in   = null;                           ///< line input
    Scanner     tok  = null;                           ///< tokenizer
    PrintWriter out  = null;                           ///< streaming output
    String      pad;                                   ///< tmp storage

    public IO(String n, InputStream i, PrintStream o) {
        name = n;
        in   = new Scanner(i);
        out  = new PrintWriter(o, true);
    }
    public void mstat() {
        Runtime rt   = Runtime.getRuntime();
        long    max  = rt.maxMemory() / 1024 / 1024;        ///< heap size
        long    tot  = rt.totalMemory() / 1024 / 1024;      ///< JVM allcated
        long    used = tot - rt.freeMemory() / 1024 / 1024; ///< used memory
        long    free = max - used;
        double  pct  = 100.0 * free / max;
        out.printf("\n%s, RAM %3.1f%% free (%d / %d MB)\n", name, pct, free, max);
    }
    public boolean readline() {
        tok = in.hasNextLine()                         ///< create tokenizer
            ? new Scanner(in.nextLine()) : null;   
        return tok != null;
    }
    public void pstr(String s)   { out.print(s); out.flush(); }
    public void pchr(int n)      { out.print(Character.toChars(n)); }
    public void debug(String s)  { if (DEBUG) pstr(s); }
    public void err(Exception e) { e.printStackTrace(); }
    
    String next_token()       {                        ///< fetch next token from in stream
        return tok.hasNext() ? tok.next() : null;
    }   
    String scan(String delim) {
        var d = tok.delimiter();                       ///< keep delimiter (space)
        tok.useDelimiter(delim); pad = tok.next();     /// * read to delimiter (into pad)
        tok.useDelimiter(d);     tok.next();           /// * restore and skip off delim
        return (pad = pad.substring(1));
    }
    ///
    ///> IO methods
    ///
    int    key() { return (int)next_token().charAt(0); }
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
        for (int n : ss) pstr(itoa(n, base)+" ");
    }
    void words(Dict dict) {
        int i=0, sz = 0; 
        for (var w : dict) {
            pstr("  " + w.name);
            sz += w.name.length() + 2;                         /// width control
            if (sz > 64) { cr(); sz = 0; }
        }
        cr();
    }
    void see(Code c, int dp) {
        if (c==null) return;
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
    void load(VM vm, String fn) {
        debug("loading "+fn+"...\n");
        Scanner in0 = in;
        int i = 0;
        try (Scanner sc = new Scanner(new File(fn))) {
            in = sc;
            while (readline()) {
                i++;
                if (!vm.outer()) break;
            }
        }
        catch (IOException e) { err(e); }
        debug(fn + " loaded "+i+" lines\n");
        in = in0;
    }
}
