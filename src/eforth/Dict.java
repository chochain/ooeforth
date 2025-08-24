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
    /// dictionary indexing i_w = |   pf index   |  word index  |
    /// and proxies               |<-- 16-bit -->|<-- 16-bit -->|
    ///
    int idx() {                                                    ///< calculate String index
        return ((dict.tail().pf.size() - 1) << 16) | dict.tail().token;
    }
    int    getv(int i_w) { return dict.get(i_w & 0x7fff).get_var(i_w >> 16);    }
    void   setv(int i_w, int n) { dict.get(i_w & 0x7fff).set_var(i_w >> 16, n); }
    String str(int i_w)  { return dict.get(i_w & 0x7fff).pf.get(i_w >> 16).str; }
    ///
    ///> find - Forth dictionary search 
    ///    @param  str  input string to be search against dictionary words
    ///    @return      Code found; null - if not found
    ///
    Code find(String n, boolean compile)  {
        for (int i=dict.size()-(compile ? 2 : 1); i>=0; i--) { // search array from tail to head
            Code w = dict.get(i); 
            if (n.equals(w.name)) return w;
        }
        return null;
    }
    Code compile(Code w) { dict.tail().pf.add(w); return w; }
    Code bran()          { return dict.tail(2).pf.tail();   }
}
