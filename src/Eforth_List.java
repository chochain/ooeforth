import java.util.*;

public class Eforth_List<Eforth_Code> extends ArrayList<Eforth_Code> {
    Eforth_Code head()			 { return get(0);               }
    Eforth_Code tail()           { return get(size() - 1);      }
    Eforth_Code tail(int offset) { return get(size() - offset); }
    void set_head(Eforth_Code w) { set(0, w);                   }
    void drop_head()      		 { remove(0);                   }
    void drop_tail()      		 { remove(size()-1);            }
}
	
