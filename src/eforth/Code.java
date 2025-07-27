package eforth;

public class Code {
    static int     fence = 0;
        
    public String  name;
    public int     idx   = 0;
    public boolean immd  = false;
    public int     stage = 0;
    public String  str;

    public List<Code>    pf = new List<>();
    public List<Code>    p1 = new List<>();
    public List<Code>    p2 = new List<>();
    public List<Integer> qf = new List<>();
    ///
    /// constructors
    ///
    public Code(String n)            { name=n; idx=fence++; }
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
}
