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
    public List<Code>     pf = new List<>();      ///< if..pf..
    public List<Code>     p1 = new List<>();      ///< else..p1..then
    public List<Code>     p2 = new List<>();      ///< aft..
    public List<Integer>  qf = new List<>();      ///< variable storage
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
    public Code add(Code w)          { pf.add(w);      return this; }
    public Code add1(Code w)         { p1.add(w);      return this; }
    public Code add(List<Code> lst)  { pf.addAll(lst); return this; }
    public Code add1(List<Code> lst) { p1.addAll(lst); return this; }
    public Code add2(List<Code> lst) { p2.addAll(lst); return this; }
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
    public void nest(List<Code> pf) {
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
