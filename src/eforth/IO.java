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
    public enum OP { CR, BL, EMIT, DOT, UDOT, DOTR, UDOTR, SPCS }
        
    Scanner         in   = null;
    StringTokenizer tok  = null;
    PrintWriter     out  = null;

    public IO(InputStream i, PrintStream o) {
        in  = new Scanner(i);
        out = new PrintWriter(o, true);
    }
    public boolean readline() {
        boolean nx = in.hasNextLine();                ///< any more line to read?
        String tib = nx ? in.nextLine() : null;       ///< feed input line
        tok = nx ? new StringTokenizer(tib) : null;   ///< build tokenizer
        return nx;
    }
    public String next_token() {
        return tok.hasMoreTokens() ? tok.nextToken().trim() : null;
    }
    public String scan(String delim) {
        String s = tok.nextToken(delim);              ///< read to delimiter
        tok.nextToken();                              ///< skip a blank
        return s;
    }

    public void pstr(String s)   { out.print(s); }
    public void err(Exception e) { e.printStackTrace(); }
    ///
    ///> IO methods
    ///
    public int    key() { return (int)tok.nextToken().charAt(0); }
    public String itoa(int n, int base) { return Integer.toString(n, base); }
    public void spaces(int n) {
        for (int i=0; i < Math.max(1,n); i++) out.print(" ");
    }
    public void dot(OP op, int n, int r, int base) {
        switch (op) {
        case CR:   out.println();                            break;
        case BL:   out.print(Character.toChars(0x20));       break;
        case EMIT: out.print(Character.toChars(n));          break;
        case DOT:  out.print(Integer.toString(n, base)+" "); break;
        case DOTR: {
            String s = itoa(n, base);
            spaces(r - s.length());
            out.print(s);
        } break;
        case UDOTR: {
            String s = itoa(n&0x7fffffff, base);
            spaces( r - s.length());
            out.print(s);
        } break;
        }
    }
    public void dot(OP op, int n, int r) { dot(op, n, r, 10); }
    public void dot(OP op, int n)        { dot(op, n, 0, 10); }
    public void cr()                     { dot(OP.CR, 0, 0, 10); }
    public void bl()                     { dot(OP.BL, 0, 0, 10); }
    ///
    ///> ok - stack dump and OK prompt
    ///
    public void ss_dump(Stack<Integer> ss, int base) {
        cr();
        for (int n : ss) out.print(Integer.toString(n, base)+" ");
    }
    public void words(Dict dict) {
        int i=0, sz = 0; 
        for (var w : dict) {
            pstr(w.name + "  ");
            sz += w.name.length() + 2;                         /// width control
            if (sz > 64) { cr(); sz = 0; }
        }
    }
    public void see(Code w) {
        if (w == null) return;
        
        pstr(w.name+", "+w.token+", "+w.qf.toString());
        for (var p : w.pf) pstr(p.name+", "+p.token+", "+p.qf.toString()+"| ");
    }
}
