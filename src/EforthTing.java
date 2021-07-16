import java.util.*;
import java.awt.*;
import java.awt.event.*; 
import java.time.LocalTime;
import java.util.function.Consumer;
import java.util.Arrays;

public class Eforth202 {	// ooeforth
	static Scanner in;
	static Stack<Integer> stack=new Stack<>();
	static Stack<Integer> rstack=new Stack<>();
	static ArrayList<Code> dictionary=new ArrayList<>();
	static boolean compiling=false;
	static int base=10;
	static int wp,ip;
	static Frame frame = new Frame("ooeForth");
	static TextArea input = new TextArea("words",10,50);
	static TextArea output = new TextArea("ooeForth 2.02\n",10,80);
	static void setup_dictionary() {
		final String prim[]= {
				":","dup","over","4dup","swap","rot","-rot","2swap","pick","roll",
				"2dup","2over","drop","nip","2drop",">r","r>","r@","+","-",
				"*","/","mod","*/","*/mod","and","or","xor","negate","0=",
				"0<","0>","=","<",">","<>",">=","<=","base@","base!",
				"hex","decimal","cr",".",".r","u.r","key","emit","space","spaces",
				"]","[","'","exit","exec","create","variable","constant","@","!",
				"+!","?","array@","array!",",","allot","does","to","is","here",
				"words",".s","see","time","ms",
		};
		final String immd[]= {
				";","(","$\"","\\",".(",".\"",
				"aft","again","begin","else","for","if",
				"next","repeat","then","until","while",
		};
		for (String s: prim) { dictionary.add(new Code(s)); }
		for (String s: immd) { dictionary.add(new Code(s).immediate()); }		// CC:
	}
	public static void main(String args[]) {	// ooeforth 2.01
		System.out.println("ooeForth2.02\n");
		setup_dictionary();
		// GetKeyChar
		Font font= new Font("Monospaced", Font.PLAIN, 12);
		input.setFont(font);
		output.setFont(font);
		frame.add(input, BorderLayout.EAST);
		frame.add(output, BorderLayout.WEST);
		frame.setSize(1000, 700);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				System.exit(0);
			}});
		input.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent ke) {
				char keyChar = ke.getKeyChar();
				if (keyChar <= 13) {
					in = new Scanner(input.getText());
					outerInterpreter();
					for(int n:stack) System.out.print(Integer.toString(n,base)+" ");
					System.out.print(">ok\n");
					input.setText("");
					in.close();
				}
			}});
	}
	// outer interpreter
	public static void outerInterpreter() {			// ooeforth 2.01
		String idiom;
		while(in.hasNext()) {  							// parse input
			idiom=in.next();
			Code newWordObject=null;
			for (var w : dictionary) {  				// search dictionary
				if (w.name.equals(idiom))
				{newWordObject=w;break;};}
			if(newWordObject !=null) {  				// word found
				if((!compiling) || newWordObject.immediate) {
					try {newWordObject.xt(); } 			// execute
					catch (Exception e) {output.append(e.toString());}}
				else {  								// or compile
					Code latestWord=dictionary.get(dictionary.size()-1);
					latestWord.addCode(newWordObject);}}
			else { 
				try {int n=Integer.parseInt(idiom, base); // not word, try number
				if (compiling) {  					// compile integer literal
					Code latestWord=dictionary.get(dictionary.size()-1);
					latestWord.addCode(new Code("dolit",n));}
				else { stack.push(n);}}				// or push number on stack
				catch (NumberFormatException  ex) {		// catch number errors
					output.append(idiom + "? ");
					compiling=false; stack.clear();}
			}
		}
		for(int n:stack) output.append(Integer.toString(n,base)+" ");
		output.append(">ok\n");
	}
	// forth words constructor
	static class Code {									// one size fits all objects
		static int fence=0;
		public int token=0;
		public String name;
		public ArrayList<Code> pf=new ArrayList<>();
		public ArrayList<Code> pf1=new ArrayList<>();
		public ArrayList<Code> pf2=new ArrayList<>();
		public ArrayList<Integer> qf=new ArrayList<>() ;
		public int struct=0;
		public boolean immediate=false;
		public String literal;
		public Code(String n) {name=n;token=fence++;}
		public Code(String n, boolean f) {name=n; if (f) token=fence++;}  // CC:
		public Code(String n, int d) {name=n;qf.add(d);}
		public Code(String n, String l) {name=n;literal=l;}
		public Code immediate() { immediate=true; return this; }
		public void xt() {
			if (lookUp.containsKey(name)) {
				lookUp.get(name).accept(this);					// run primitives words, CC:
			} else { rstack.push(wp); rstack.push(ip);			// run colon words
			wp=token; ip=0;	// wp points to current colon object
			for(Code w:pf) {
				try { w.xt();ip++;}								// inner interpreter
				catch (ArithmeticException e) {}}
			ip=rstack.pop(); wp=rstack.pop();}
		}
		public void addCode(Code w) { this.pf.add(w);}		
	}
	static public HashMap<String, Consumer<Code>> lookUp=new HashMap<>() {{		// CC:
		// stacks
		put("dup",c->stack.push(stack.peek()));
		put("over",c->stack.push(stack.get(stack.size()-2)));
		put("2dup",c->stack.addAll(stack.subList(stack.size()-2,stack.size())));
		put("2over",c->stack.addAll(stack.subList(stack.size()-4,stack.size()-2)));
		put("4dup",c->stack.addAll(stack.subList(stack.size()-4,stack.size())));
		put("swap",c->stack.add(stack.size()-2,stack.pop()));
		put("rot",c->stack.push(stack.remove(stack.size()-3)));
		put("-rot",c->{stack.push(stack.remove(stack.size()-3));stack.push(stack.remove(stack.size()-3));});
		put("2swap",c->{stack.push(stack.remove(stack.size()-4));stack.push(stack.remove(stack.size()-4));});
		put("pick",c->{int i=stack.pop();int n=stack.get(stack.size()-i-1);stack.push(n);});
		put("roll",c->{int i=stack.pop();int n=stack.remove(stack.size()-i-1);stack.push(n);});
		put("drop",c->stack.pop());
		put("nip",c->stack.remove(stack.size()-2));
		put("2drop",c->{stack.pop();stack.pop();});
		put(">r",c->rstack.push(stack.pop()));
		put("r>",c->stack.push(rstack.pop()));
		put("r@",c->stack.push(rstack.peek()));
		put("push",c->rstack.push(stack.pop()));
		put("pop",c->stack.push(rstack.pop()));
		// math
		put("+",c->stack.push(stack.pop()+stack.pop()));
		put("-",c->{int n=stack.pop();stack.push(stack.pop()-n);});
		put("*",c->stack.push(stack.pop()*stack.pop()));
		put("/",c->{int n=stack.pop();stack.push(stack.pop()/n);});
		put("*/",c->{int n=stack.pop();stack.push(stack.pop()*stack.pop()/n);});
		put("*/mod",c->{int n=stack.pop();int m=stack.pop()*stack.pop();
		stack.push(m%n);stack.push(m/n);});
		put("mod",c->{int n=stack.pop();stack.push(stack.pop()%n);});
		put("and",c->stack.push(stack.pop()&stack.pop()));
		put("or",c->stack.push(stack.pop()|stack.pop()));
		put("xor",c->stack.push(stack.pop()^stack.pop()));
		put("negate",c->stack.push(-stack.pop()));
		// logic
		put("0=",c->stack.push((stack.pop()==0)?-1:0));
		put("0<",c->stack.push((stack.pop()<0)?-1:0));
		put("0>",c->stack.push((stack.pop()>0)?-1:0));
		put("=",c->{int n=stack.pop();stack.push((stack.pop()==n)?-1:0);});
		put(">",c->{int n=stack.pop();stack.push((stack.pop()>n)?-1:0);});
		put("<",c->{int n=stack.pop();stack.push((stack.pop()<n)?-1:0);});
		put("<>",c->{int n=stack.pop();stack.push((stack.pop()!=n)?-1:0);});
		put(">=",c->{int n=stack.pop();stack.push((stack.pop()>=n)?-1:0);});
		put("<=",c->{int n=stack.pop();stack.push((stack.pop()<=n)?-1:0);});
		// output
		put("base@",c->stack.push(base));
		put("base!",c->base=stack.pop());
		put("hex",c->base=16);
		put("decimal",c->base=10);
		put("cr",c->output.append("\n"));
		put(".",c->output.append(Integer.toString(stack.pop(),base)+" "));
		put(".r",c->{int n=stack.pop();String s=Integer.toString(stack.pop(),base);
		for(int i=0;i+s.length()<n;i++)output.append(" ");
		output.append(s+" ");});
		put("u.r",c->{int n=stack.pop();String s=Integer.toString(stack.pop()&0x7fffffff,base);
		for(int i=0;i+s.length()<n;i++)output.append(" ");
		output.append(s+" ");});
		put("key",c->stack.push((int) in.next().charAt(0)));
		put("emit",c->{char b=(char)(int)stack.pop();output.append(""+b);});
		put("space",c->{output.append(" ");});
		put("spaces",c->{int n=stack.pop();for(int i=0;i<n;i++)output.append(" ");});
		// literals
		put("[",c->compiling=false);
		put("]",c->compiling=true);
		put("'",c->{String s=in.next(); boolean found=false;
		for (var w:dictionary) {
			if (s.equals(w.name)) { stack.push(w.token); found=true;break;}}
		if (!found) stack.push(-1);});
		put("dolit",c->stack.push(c.qf.get(0)));			// integer literal
		put("dostr",c->stack.push(c.token));				// string literals
		put("$\"",c->{ // -- w a
			var d=in.delimiter();
			in.useDelimiter("\"");			
			String s=in.next();
			Code last=dictionary.get(dictionary.size()-1);
			last.addCode(new Code("dostr",s));				// literal=s
			in.useDelimiter(d);in.next();
			stack.push(last.token);stack.push(last.pf.size()-1);});
		put("dotstr",c->{output.append(c.literal);});
		put(".\"",c->{
			var d=in.delimiter();
			in.useDelimiter("\"");		
			String s=in.next();
			Code last=dictionary.get(dictionary.size()-1);
			last.addCode(new Code("dotstr",s));				// literal=s
			in.useDelimiter(d);in.next();});
		put("(",c->{
			var d=in.delimiter();
			in.useDelimiter("\\)");
			String s=in.next();
			in.useDelimiter(d);in.next();});
		put(".(",c->{
			var d=in.delimiter();
			in.useDelimiter("\\)");output.append(in.next());
			in.useDelimiter(d);in.next();});
		put("\\",c->{
			var d=in.delimiter();
			in.useDelimiter("\n");in.next();
			in.useDelimiter(d);in.next();});
		// structure: if else then
		put("branch",c->{
			if(!(stack.pop()==0)) {for(var w:c.pf) w.xt();}
			else {for(var w:c.pf1) w.xt();}});
		put("if",c->{
			Code last=dictionary.get(dictionary.size()-1);
			last.addCode(new Code("branch", false));	
			dictionary.add(new Code("temp", false));});
		put("else",c->{
			Code last=dictionary.get(dictionary.size()-2);
			Code temp=dictionary.get(dictionary.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.get(last.pf.size()-1).struct=1;});
		put("then",c->{
			Code last=dictionary.get(dictionary.size()-2);
			Code temp=dictionary.get(dictionary.size()-1);
			if (last.pf.get(last.pf.size()-1).struct==0) {
				last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
				dictionary.remove(dictionary.size()-1);
			} else {
				last.pf.get(last.pf.size()-1).pf1.addAll(temp.pf);
				if (last.pf.get(last.pf.size()-1).struct==1) {
					dictionary.remove(dictionary.size()-1);
				}
				else temp.pf.clear();
			}});
		// loops
		put("loops",c->{
			if (c.struct==1) {							// again
				while(true) {for(var w:c.pf) w.xt();}}
			if (c.struct==2) {							// while repeat
				while (true) {
					for(var w:c.pf) w.xt();
					if (stack.pop()==0) break;
					for(var w:c.pf1) w.xt();}
			} else {
				while(true) {							// until
					for(var w:c.pf) w.xt();
					if(stack.pop()!=0) break;}
			}});
		put("begin",c->{
			Code last=dictionary.get(dictionary.size()-1);
			last.addCode(new Code("loops", false));	
			dictionary.add(new Code("temp", false));});
		put("while",c->{
			Code last=dictionary.get(dictionary.size()-2);
			Code temp=dictionary.get(dictionary.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.get(last.pf.size()-1).struct=2;});
		put("repeat",c->{
			Code last=dictionary.get(dictionary.size()-2);
			Code temp=dictionary.get(dictionary.size()-1);
			last.pf.get(last.pf.size()-1).pf1.addAll(temp.pf);
			dictionary.remove(dictionary.size()-1);});
		put("again",c->{
			Code last=dictionary.get(dictionary.size()-2);
			Code temp=dictionary.get(dictionary.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			last.pf.get(last.pf.size()-1).struct=1;
			dictionary.remove(dictionary.size()-1);});
		put("until",c->{
			Code last=dictionary.get(dictionary.size()-2);
			Code temp=dictionary.get(dictionary.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			dictionary.remove(dictionary.size()-1);});
		// for next
		put("cycles",c->{int i=0;
		if (c.struct==0) {
			while(true){
				for(var w:c.pf) w.xt();
				i=rstack.pop();i--;
				if (i<0) break;
				rstack.push(i);
			}
		} else {
			if (c.struct>0) {
				for(var w:c.pf) w.xt();
				while(true){
					for(var w:c.pf2) w.xt();
					i=rstack.pop();i--;
					if (i<0) break;
					rstack.push(i);
					for(var w:c.pf1) w.xt();
				}
			}}
		});
		put("for",c->{
			Code last=dictionary.get(dictionary.size()-1);
			last.addCode(new Code(">r", false));
			last.addCode(new Code("cycles", false));
			dictionary.add(new Code("temp", false));});
		put("aft",c->{
			Code last=dictionary.get(dictionary.size()-2);
			Code temp=dictionary.get(dictionary.size()-1);
			last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			temp.pf.clear();
			last.pf.get(last.pf.size()-1).struct=3;});
		put("next",c->{
			Code last=dictionary.get(dictionary.size()-2);
			Code temp=dictionary.get(dictionary.size()-1);
			if (last.pf.get(last.pf.size()-1).struct==0) 
				last.pf.get(last.pf.size()-1).pf.addAll(temp.pf);
			else last.pf.get(last.pf.size()-1).pf2.addAll(temp.pf);
			dictionary.remove(dictionary.size()-1);});
		// defining words
		put("exit",c->{throw new ArithmeticException(); });			// marker to exit interpreter
		put("exec",c->{int n=stack.pop();dictionary.get(n).xt();});
		put(":",c->{         										// -- box
			String s=in.next();
			dictionary.add(new Code(s));
			Code last=dictionary.get(dictionary.size()-1);
			compiling=true;});
		put(";",c->{         								
			Code last=dictionary.get(dictionary.size()-1);
			compiling=false;});
		put("docon",c->{stack.push(c.qf.get(0));});			// integer literal
		put("dovar",c->{stack.push(c.token);});				// string literals
		put("create",c->{
			String s=in.next();
			dictionary.add(new Code(s));
			Code last=dictionary.get(dictionary.size()-1);
			last.addCode(new Code("dovar",0));
			last.pf.get(0).token=last.token;
			last.pf.get(0).qf.remove(0);});
		put("variable",c->{ 
			String s=in.next();
			dictionary.add(new Code(s));
			Code last=dictionary.get(dictionary.size()-1);
			last.addCode(new Code("dovar",0));
			last.pf.get(0).token=last.token;});
		put("constant",c->{  // n --
			String s=in.next();
			dictionary.add(new Code(s));
			Code last=dictionary.get(dictionary.size()-1);
			last.addCode(new Code("docon",stack.pop()));
			last.pf.get(0).token=last.token;});
		put("@",c->{  // w -- n
			Code last=dictionary.get(stack.pop());
			stack.push(last.pf.get(0).qf.get(0));});
		put("!",c->{  // n w -- 
			Code last=dictionary.get(stack.pop());
			last.pf.get(0).qf.set(0,stack.pop());});
		put("+!",c->{  // n w -- 
			Code last=dictionary.get(stack.pop());
			int n=last.pf.get(0).qf.get(0); n+=stack.pop();
			last.pf.get(0).qf.set(0,n);});
		put("?",c->{  // w -- 
			Code last=dictionary.get(stack.pop());
			output.append(Integer.toString(last.pf.get(0).qf.get(0)));});
		put("array@",c->{  // w a -- n
			int a=stack.pop();
			Code last=dictionary.get(stack.pop());
			stack.push(last.pf.get(0).qf.get(a));});
		put("array!",c->{  // n w a -- 
			int a=stack.pop();
			Code last=dictionary.get(stack.pop());
			last.pf.get(0).qf.set(a,stack.pop());});
		put(",",c->{ // n --
			Code last=dictionary.get(dictionary.size()-1);
			last.pf.get(0).qf.add(stack.pop());});
		put("allot",c->{  // n --
			int n=stack.pop(); 
			Code last=dictionary.get(dictionary.size()-1);
			for(int i=0;i<n;i++) last.pf.get(0).qf.add(0);});
		put("does",c->{ // n --
			Code last=dictionary.get(dictionary.size()-1);
			Code source=dictionary.get(wp);
			last.pf.addAll(source.pf.subList(ip+2,source.pf.size()));});
		put("to",c->{  											// n -- , compile only 
			Code last=dictionary.get(wp);	ip++;				// current colon word
			last.pf.get(ip++).pf.get(0).qf.set(0,stack.pop());});	// next constant
		put("is",c->{  											// w -- , execute only
			Code source=dictionary.get(stack.pop());			// source word
			String s=in.next(); boolean found=false;
			for (var w:dictionary) {
				if (s.equals(w.name)) { 						// target word
					Code target=dictionary.get(w.token); 
					target.pf=source.pf; 						// copy pf 
					found=true;break;}}
			if (!found) output.append(s+"? ");});
		// tools
		put("here",c->{stack.push(Code.fence);});				// CC:
		put("words",c->{int i=0;for (var word:dictionary) {
			output.append(word.name+" "+word.token+" ");i++;
			if (i>9) {output.append("\n");i=0;}}});
		put(".s",c->{for(int n:stack) output.append(Integer.toString(n,base)+" ");});
		put("see",c->{String s=in.next(); boolean found=false;
		for (var word:dictionary) {
			if (s.equals(word.name)) { 
				output.append(word.name+", "+word.token+", "+word.qf.toString());
				for( var w:word.pf) output.append(w.name+", "+w.token+", "+w.qf.toString()+"| ");       
				found=true; break;}
		}
		if (!found) output.append(s+"? ");
		});
		put("time",c->{
			LocalTime now=LocalTime.now();
			output.append(now.toString());});
		put("ms",c->{ // n --
			try { Thread.sleep(stack.pop());} 
			catch (Exception e) { output.append(e.toString());}});
	}};
}
