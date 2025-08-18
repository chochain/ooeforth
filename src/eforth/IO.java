///
/// @file
/// @brief - IO module
///
package eforth;

import java.util.*;
import java.io.*;
///
///> console input/output
///
public class IO {
    enum OP { CR, BL, EMIT, DOT, UDOT, DOTR, UDOTR, SPCS }
        
    Scanner         in   = null;
    StringTokenizer tok  = null;
    PrintWriter     out  = null;

    public IO(InputStream i, PrintStream o) {
        in  = new Scanner(i);
        out = new PrintWriter(o, true);
    }
    public boolean readline() {
        boolean t = in.hasNextLine();                ///< any more line to read?
        String tib = t ? in.nextLine() : null;       ///< feed input line
        System.out.println("tib="+tib);
        tok = t ? new StringTokenizer(tib) : null;   ///< build tokenizer
        return tok!=null;
    }
    public void pstr(String s)   { out.print(s); out.flush(); }
    public void err(Exception e) { e.printStackTrace(); }
    
    String next_token() {
        return tok.hasMoreTokens() ? tok.nextToken().trim() : null;
    }
    String scan(String delim) {
        String s = tok.nextToken(delim);              ///< read to delimiter
        tok.nextToken();                              ///< skip a blank
        return s;
    }
    ///
    ///> IO methods
    ///
    int    key() { return (int)tok.nextToken().charAt(0); }
    String itoa(int n, int base) { return Integer.toString(n, base); }
    void spaces(int n) {
        for (int i=0; i < Math.max(1,n); i++) out.print(" ");
    }
    void dot(OP op, int n, int r, int base) {
        switch (op) {
        case CR:   pstr("\n");                          break;
        case BL:   out.print(Character.toChars(0x20));  break;
        case EMIT: out.print(Character.toChars(n));     break;
        case DOT:  pstr(itoa(n ,base) + " ");           break;
        case UDOT: pstr(itoa(n&0x7fffffff, base));      break;
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
    void see(Code w) {
        if (w == null) return;
        
        pstr(w.name+", "+w.token+", "+w.qf.toString());
        for (var p : w.pf) pstr(p.name+", "+p.token+", "+p.qf.toString()+"| ");
    }
}
