///
/// @file 
/// @brief - Code class - the core component of eForth
///
package eforth;

import java.util.*;
import java.util.function.Consumer;

public class Code {
    static int     fence = 0;

    public String  name;
    public int     token = 0;
    public boolean immd  = false;
    public int     stage = 0;
    
    public Consumer<Code> xt = null;              ///< execution token
    public FV<Code>       pf = new FV<>();        ///< if..pf..
    public FV<Code>       p1 = new FV<>();        ///< else..p1..then
    public FV<Code>       p2 = new FV<>();        ///< aft..
    public FV<Integer>    qf = new FV<>();        ///< variable storage
    public String         str;                    ///< string storage
    ///
    /// constructors
    ///
    public Code(String n)            { name=n; fence++; }
    public Code(String n, boolean b) { name=n; }
    public Code(String n, int d)     { name=n; qf.add(d); }
    public Code(String n, String s)  { name=n; str=s; }
    ///
    /// accessing method
    ///
    public Code immediate()          { immd=true;      return this; }
    public Code add_w(Code w)        { pf.add(w);      return this; }
//    public Code add1(Code w)         { p1.add(w);      return this; }
//    public Code add1(FV<Code> lst)   { p1.addAll(lst); return this; }
//    public Code add2(FV<Code> lst)   { p2.addAll(lst); return this; }

    public void add_var(int v)        { pf.head().qf.add(v);        }
    public void set_var(int i, int v) { pf.head().qf.set(i, v);     }
    public int  get_var(int i)        { return pf.head().qf.get(i); }
    ///
    ///> inner interpreter
    ///
    public void nest() {
        if (xt != null) { xt.accept(this); return; }
        for (var w : pf) {
            try   { w.nest(); }
            catch (ArithmeticException e) {}
        }
    }
    public void nest(FV<Code> pf) {
        for (var w : pf) w.nest();
    }
    public void unnest() { throw new ArithmeticException(); }
    ///
    ///> branching, looping methods
    ///
    public void branch(Stack<Integer> ss) {
        for (var w : ss.pop() != 0 ? pf : p1) w.nest();
    }
    public void loop(Stack<Integer> ss) {
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
        int i=0;
        if (stage==0) {
            while(true){
                nest(pf);
                i=rs.pop();
                if (--i<0) break;
                rs.push(i);
            }
        } 
        else {
            nest(pf);
            while (true) {
                nest(p2);
                i = rs.pop();
                if (--i<0) break;
                rs.push(i);
                nest(p1);
            }
        }
    }
}
