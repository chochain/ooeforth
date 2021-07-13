public class EforthCode {
    static int      fence     = 0;
		
    public String 	name;
    public int 		idx       = 0;
    public boolean 	immd      = false;
    public int      stage     = 0;
    public String 	str;

    public EforthList<EforthCode>   pf  = new EforthList<>();
    public EforthList<EforthCode>   pf1 = new EforthList<>();
    public EforthList<EforthCode>   pf2 = new EforthList<>();
    public EforthList<Integer>  qf  = new EforthList<>();

    public EforthCode(String n) 		   { name=n; idx=fence++; }
    public EforthCode(String n, boolean b) { name=n; }
    public EforthCode(String n, int d)     { name=n; qf.add(d); }
    public EforthCode(String n, String s)  { name=n; str=s; }

    public EforthCode immediate()         		{ immd=true;  		return this; }
    public EforthCode add(EforthCode w)  			{ pf.add(w);  		return this; }
    public EforthCode add1(EforthCode w) 			{ pf1.add(w); 		return this; }
    public EforthCode add(EforthList<EforthCode> lst)  	{ pf.addAll(lst);  	return this; }
    public EforthCode add1(EforthList<EforthCode> lst) 	{ pf1.addAll(lst); 	return this; }
    public EforthCode add2(EforthList<EforthCode> lst) 	{ pf2.addAll(lst); 	return this; }
}
