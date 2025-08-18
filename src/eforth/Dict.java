///
/// @file 
/// @brief - generic List class - extended ArrayList
///
package eforth;

import java.util.*;
import java.io.*;
import java.time.*;
import java.util.function.*;

final public class Dict extends FV<Code> {
    static Dict dict = new Dict();
    
    static Dict get_instance() { return dict; }         ///< singleton
    ///
    ///> create dictionary with given word list
    ///
    void forget(int t) {
        dict.subList(t, dict.size()).clear();           ///> forget words
    }
    ///
    ///> find - Forth dictionary search 
    ///    @param  str  input string to be search against dictionary words
    ///    @return      Code found; null - if not found
    ///
    Code find(String n, boolean compile)  {
        for (int i=dict.size()-(compile ? 2 : 1); i>=0; i--) { // search array from tail to head
            Code w = dict.get(i); if (w.name == n) return w;
        }
        return null;
    }
    Code compile(Code w) { dict.tail().pf.add(w); return w; }
    Code bran()          { return dict.tail(2).pf.tail();   }
}
