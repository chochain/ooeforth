///
/// @file 
/// @brief - generic List class - extended ArrayList
///
package eforth;

import java.util.ArrayList;

public class FV<T> extends ArrayList<T> {      ///< eForth ArrayList (i.e. vector)
    T head()               { return get(0);                 }
    T tail()               { return get(size() - 1);        }
    T tail(int offset)     { return get(size() - offset);   }
    
    FV<T> merge(FV<T> lst) { addAll(lst);      return this; }
    FV<T> drop()           { remove(size()-1); return this; }
}
    
