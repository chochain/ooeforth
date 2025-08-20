///
/// @file 
/// @brief - Code class - the core component of eForth
///
package eforth;

import java.util.*;
import java.util.function.Consumer;

public class Code {
    static int fence = 0;                  ///< token index

    String  name  = null;
    boolean immd  = false;
    int     token = 0;
    int     stage = 0;
    
    Consumer<Code> xt = null;              ///< execution token
    FV<Code>       pf = new FV<>();        ///< if..pf..
    FV<Code>       p1 = new FV<>();        ///< else..p1..then
    FV<Code>       p2 = new FV<>();        ///< aft..next
    FV<Integer>    qf = new FV<>();        ///< variable storage
    String         str;                    ///< string storage
    ///
    ///> constructors
    ///
    Code(String n, Consumer<Code> f, boolean im) {                           ///< built-in words
        name=n; xt=f; immd=im; token=fence++;
    }             
    Code(String n) { name=n; token=fence++; }                                ///< colon words
    Code(Consumer<Code> f, String n)            { name=n; xt=f; }            ///< branching nodes
    Code(Consumer<Code> f, String n, int d)     { name=n; xt=f; qf.add(d); } ///< int literal
    Code(Consumer<Code> f, String n, String s)  { name=n; xt=f; str=s; }     ///< string literal
    ///
    ///> attribute setting
    ///
    Code immediate()           { immd=true; return this; }
    ///
    ///> variable storage management methods
    ///
    void comma(int v)          { pf.head().qf.add(v);        }
    void set_var(int i, int v) { pf.head().qf.set(i, v);     }
    int  get_var(int i)        { return pf.head().qf.get(i); }
    ///
    ///> inner interpreter
    ///
    void nest() {
        if (xt != null) { xt.accept(this); return; }
        for (var w : pf) {
            try   { w.nest(); }
            catch (ArithmeticException e) { break; } ///* capture UNNEST
        }
    }
    void nest(FV<Code> pf) {
        for (var w : pf) w.nest();
    }
    void unnest() { throw new ArithmeticException(); }
    ///
    ///> branching, looping methods
    ///
    void branch(Stack<Integer> ss) {
        for (var w : ss.pop() != 0 ? pf : p1) w.nest();
    }
    void begin(Stack<Integer> ss) {
        int b = stage;
        while (true) {
            nest(pf);                              /// * begin..
            if (b==0 && ss.pop() != 0) break;      /// * ..until
            if (b==1)                  continue;   /// * ..again
            if (b==2 && ss.pop() == 0) break;      /// * ..while..repeat
            nest(p1);
        }
    }
    void dofor(Stack<Integer> rs) {
        try {
            int i, b = stage;
            do {
                nest(pf);                          ///> for..
                if (b > 0) break;                  ///> ..aft..
                i = rs.pop();                      ///> decrement i (expensive)
                rs.push(--i);
            } while (i >= 0);
            while (b > 0) {
                nest(p2);
                i = rs.pop();
                rs.push(--i);
                if (i < 0) break;
                nest(p1);
            }
        }
        catch (Exception e) { /* leave */ }
        finally { rs.pop(); }                      ///> pop off index
    }
    void loop(Stack<Integer> rs) {                 ///> do..loop
        try {
            int i;
            while (true) {
                i = rs.pop();
                if (++i > rs.peek()) break;
                nest(pf);
                rs.push(i);
            }
        }
        catch (Exception e) { /* leave */ }        /// handle LEAVE
        finally { rs.pop(); rs.pop(); }
    }
}
