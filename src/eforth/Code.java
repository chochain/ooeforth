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
    Code(String n, Consumer<Code> f, boolean im) {                  ///< built-in words
        name=n; xt=f; immd=im; token=fence++;
    }             
    Code(String n)                    { name=n; token=fence++; }    ///< colon words
    Code(Consumer<Code> f)            { name=""; xt=f; }            ///< branching nodes
    Code(Consumer<Code> f, String s)  { name=""; xt=f; str=s; }     ///< string literal
    Code(Consumer<Code> f, int d)     { name=""; xt=f; qf.add(d); } ///< int literal
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
            catch (ArithmeticException e) {}
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
    void loop(Stack<Integer> ss) {
        switch (stage) {
        case 1:        /// again 
            while (true) {
                nest(pf);
            }
            /// never comes here?
            /// break;
        case 2:        /// repeat
            while (true) {
                nest(pf);
                if (ss.pop()==0) break;
                nest(p1);
            }
            break;
        default:       /// until
            while (true) {
                nest(pf);
                if (ss.pop() !=0 ) break;
            }
        }
    }
    void cycles(Stack<Integer> rs) {
        int i = 0;
        if (stage==0) {
            while(true){
                nest(pf);
                i = rs.pop();
                if (--i < 0) break;
                rs.push(i);
            }
        } 
        else {
            nest(pf);
            while (true) {
                nest(p2);
                i = rs.pop();
                if (--i < 0) break;
                rs.push(i);
                nest(p1);
            }
        }
    }
}
