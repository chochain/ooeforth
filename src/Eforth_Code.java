class Eforth_Code {
    static int      fence     = 0;
		
    public String 	name;
    public int 		idx       = 0;
    public boolean 	immd      = false;
    public int      stage     = 0;
    public String 	literal;

    public Eforth_List<Eforth_Code> pf  = new Eforth_List<>();
    public Eforth_List<Eforth_Code> pf1 = new Eforth_List<>();
    public Eforth_List<Eforth_Code> pf2 = new Eforth_List<>();
    public Eforth_List<Integer>     qf  = new Eforth_List<>();

    public Eforth_Code(String n) {
        name = n;
        immd = false;
        idx  = fence++;
    }
    public Eforth_Code(String n, int d)    { name=n;  qf.add(d); }
    public Eforth_Code(String n, String l) { name=n;  literal=l; }

    public Eforth_Code immediate()         				 	{ immd=true;  		return this; }
    public Eforth_Code add(Eforth_Code w)  					{ pf.add(w);  		return this; }
    public Eforth_Code add1(Eforth_Code w) 					{ pf1.add(w); 		return this; }
    public Eforth_Code add(Eforth_List<Eforth_Code> lst)  	{ pf.addAll(lst);  	return this; }
    public Eforth_Code add1(Eforth_List<Eforth_Code> lst) 	{ pf1.addAll(lst); 	return this; }
    public Eforth_Code add2(Eforth_List<Eforth_Code> lst) 	{ pf2.addAll(lst); 	return this; }
}
