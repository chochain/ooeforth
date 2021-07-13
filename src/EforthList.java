import java.util.*;

final public class EforthList<T> extends ArrayList<T> {
    T head()		   { return get(0);                 }
    T tail()           { return get(size() - 1);        }
    T tail(int offset) { return get(size() - offset);   }
    
    EforthList<T> set_head(T w) { set(0, w);        return this; }
    EforthList<T> drop_head()   { remove(0);        return this; }
    EforthList<T> drop_tail()   { remove(size()-1); return this; }
}
	
