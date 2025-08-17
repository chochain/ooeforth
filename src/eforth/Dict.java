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
    static LinkedHashMap<String, Consumer<Code>> vtable;
    static Dict dict;
    
    static Dict get_instance() { return dict; }
    ///
    ///> create dictionary with given word list
    ///
    public void init(LinkedHashMap<String, Consumer<Code>> vt) {
        vtable = vt;
        vt.forEach((k, v) -> dict.add(new Code(k)));    ///> create primitive words
    }
    ///
    ///> find - Forth dictionary search 
    ///    @param  str  input string to be search against dictionary words
    ///    @return      Code found; null - if not found
    ///
    public Code find(String n)  { return dict.scan(n,wx->n.equals(wx.name)); }
    public Code compile(Code w) { dict.tail().pf.add(w); return w;           }
    public Code bran()          { return dict.tail(2).pf.tail();             }
    public Code tgt()           { return dict.get(dict.size()-2).pf.tail();  }
}
