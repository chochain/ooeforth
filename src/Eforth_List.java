import java.util.*;

final public class Eforth_List<T> extends ArrayList<T> {
    T head()		   { return get(0);               }
    T tail()           { return get(size() - 1);      }
    T tail(int offset) { return get(size() - offset); }
    
    Eforth_List<T> set_head(T w) { set(0, w);        return this; }
    Eforth_List<T> drop_head()   { remove(0);        return this; }
    Eforth_List<T> drop_tail()   { remove(size()-1); return this; }
}
	
