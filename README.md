# ooeforth
## eForth in Java
Dr. Ting has been an influential figure in Forth community. He was the key person who brought Bill Munich's eForth into a wide range of platforms. [see here](https://www.forth.org/OffeteStore/OffeteStore.html)

When I demonstrated that high-level languages such as C++, Java, even Javascript can also construct Forth dictionary without meta-compilation as were done in classic Forths, he got very excited. He stated that Forth can finally be built without bootstrapping from lower-level Forth scripts and termed the paradigm "Forth without Forth".

He had an earlier implementation, called jeForth, with input from Shawn Chen and Brad Nelson. Upon taking the template I sent, Dr. Ting single-handily completed several reversions of ooeForth which I kept in **~/orig directory**. So, is it a Forth that supports Object-Orientated Programming? No! At least not at the moment. I think Dr. Ting appreciated the fact it is implemented in an object-oriented way and prefixed it OO.

Dr. Ting has a peculiar interest in minimizing program line count and keeping his code packed without spacing. Though personally cared much more for readability and modularization, respect his preference, I gathered all the sources into one file i.e. **src/EforthTing.java**. He proudly gave a presentation on Silicon Valley Forth Interest Group's July 2021 meeting of this development. [see here](https://github.com/chochain/ooeforth/blob/master/docs/)

The project has been in frozen state since his passing late May 2022 and I've wondered into other spaces. Not until recently, 3 years later, I took notice that it demands some maintenance. So, here we are.

## What is it

1. ooeForth is an eForth developed in 100% Java.
2. The purpose is for education and to help Java developers quickly grasp the core concepts of Forth.
    + It has a small built-in word set, around 110. Much less than most of Forths 
    + No VOCABULARY, or Multitasking. These are the real power of Forth but maybe later.
    + Minimal meta-programming stuffs except CREATE..DOES>, and POSTPONE.
    + Not Object-Oriented. But can be extended with as all Forths do.
3. It utilize Java containers for its core components.
    + Dictionary        - ArrayList
    + Data Stack        - Stack
    + Return Stack      - Stack
    + Parameter Fields  - ArrayList
4. Its CELL is 32-bit integer but trivial changing to long, float, or double.

## Code Directories
ooeForth is refactored from Dr. Ting's original one-file app into the following modules.

    src/Eforth.java        - main program, text-based
       /EforthTing.java    - Dr. Ting's original one-file app (2 AWT panels)
       /eforth/Code.java   - core object, serves as dictionary entries and parameters
              /Dict.java   - a singleton dictionary manager
              /FV.java     - Forth Vector (extends ArrayList)
              /IO.java     - IO manager, handles input/output streams
              /VM.java     - Forth Virtual Machine, and built-in word definitions

## eForth Internals
The core of current implementation of eForth is the dictionary composed of an array of Code objects that represent each of Forth words.

1. <b>FV</b> - extended from ArrayList with a handful of useful methods for Forth

2. <b>Code</b> - the heart of eForth, depends on the constructor called, the following fields are populated accordingly
    <pre>
    + name  - String, holds primitive word's name, i.e. NFA in classic FORTH,
    + token - Integer, keeps index to dictionary (for reverse lookup)
    + xt    - Consumer<*Code*>, lambda as primitive word function i.e. XT in classic FORTH
    + pf, p1, p2 - parameter arrays of Code objects for compound words, i.e. PFA in classic FORTH
    + qf    - Integer Array, holds the literal value
    + str   - String, holds string literal
    </pre>

3. <b>Dictionary</b> - an array of *Code* objects
    <pre>
    + build-it words - constructed by dict_init() in VM module, with CODE/IMMD macros at start up assigning the names and these lambda i.e. Consumer<*Code*> in Code.xt
        dict[0].xt ------> lambda[0]       <== These function pointers can be converted
        dict[1].xt ------> lambda[1]           into indices to a jump table
        ...                                    which is exactly what WASM does
        dict[N-1].xt ----> lambda[N-1]     <== N is number of built-in words
        
    + colon (user defined) words - collection of word pointers during compile time
        dict[N].pf   = [ Code, Code, ... ] <== These are called the 'threads' in Forth's term
        dict[N+1].pf = [ Code, Code, ... ]     So, instead of subroutine threading
        ...                                    this is 'object' threading.
        dict[-1].pf  = [ Code, Code, ... ]     It can be further compacted into
                                               token (i.e. dict index) threading if desired
    </pre>


4. <b>Inner Interpreter</b> - *Code.nest()*
    ```Java
    void nest() {                                     
        if (xt != null) { xt.accept(this); return; }  /// built-in word
        for (var w : pf) {                            /// colon word
            try   { w.nest(); }                       /// recursive
            catch (ArithmeticException e) { break; }  /// capture UNNEST
        }
    }
    ```
    
    i.e. either we call a built-in word's lambda function or walk the Code.pf array recursively like a depth-first tree search.
    

5. <b>Outer Interpreter</b> - *parse()*
    ```Java
    Code w = dict.find(idiom, compile);             ///< search dictionary
    if (w != null) {                                ///> found word?
        if (!compile || w.immd) {                   ///> in interpreter mode?
            try                 { w.nest();  }      ///> * execute immediately
            catch (Exception e) { io.err(e); }      ///> * just-in-case something failed
        }
        else dict.compile(w);                       ///> add to dictionary if in compile mode
        return;
    }
    ///> word not found, try as a number
    try {
        int n=Integer.parseInt(idiom, base);        ///> * try process as a number
        if (compile)                                ///>> in compile mode 
            dict.compile(new Code(_dolit));         ///> add to latest defined word
        else ss.push(n);                            ///> or, add number to top of stack
    }                                            
    catch (NumberFormatException ex) {              ///> if it's not a number
        io.pstr(idiom + " ?");                      ///> * show not found sign
        compile = false; 
    }
    ```
    
    i.e. get an idiom, try find it in the dictionary, or parse it as a number.
    execute or compile depends on whether VM is in compiling or interpreter mode.


## Build & Run
I'm not a GUI person and have very limited experience with IDEs. Most of my works are done one Linux box. So, the following instructions are command-line driven. However, I think it's pretty straight forward to move these files into an IDE project if desired.

### Eforth 

    clone this repo to your local drive
    make sure you have JDK installed
    from ooeForth root directory
    
    > javac -cp src -d tests src/Eforth.java         /// to build
    > javac -cp tests Eforth                         /// to run
    
    > type> words⏎               \ to see available Forth words
    > type> 1 2 +⏎               \ see Forth in action
    > type> bye⏎  or Ctrl-C      \ to exit eForth

### Dr. Ting's original one-file eForth

    javac -d tests src/EforthTing.java
    java -cp tests EforthTing

## Examples

Once you have the Eforth.java up and running, beyond roaming around, there are a list of eForth examples provided. They are a great collections from the gurus of Forth, Bill Ragsdale, Dr. Ting, and others. 

    bring up ooeForth and enter
    
    > type> include tests/demo.fs⏎          \ to invoke demos

This will load and run all 18 examples one by one. They will give you some insight into the beauty of the trait.

### To Learn More About Forth?
If your programming language exposure has been with C, Java, or even Python so far, FORTH is quite **different**. Before you dive right into the deep-end, here's a good online materials.
* Interactive tutorial for FORTH primer. It teaches you how FORTH fundamentally works such as the numbers, the stack, and the dictionary.
  > <a href="https://skilldrick.github.io/easyforth/#introduction" target="_blank">*Easy Forth Tutorial by Nick Morgan*</a> with a <a href="https://wiki.forth-ev.de/lib/exe/fetch.php/en:projects:a-start-with-forth:05_easy_forth_v16_a5_withexp_comments.pdf?fbclid=IwAR0sHmgiDtnMRuQtJdVkhl9bmiitpgcjs4ZlIDVtlxrssMOmLBv0vesvmKQ" target="_blank">*Writeup*</a> by Juergen Pintaske.

To understand the philosophy of FORTH, excellent online e-books are here free for you.
* Timeless classic for the history, paths, and thoughts behind FORTH language.
  > <a href="https://www.forth.com/starting-forth/" target="_blank">*Starting Forth by Leo Brodie*</a><br/>
  > <a href="http://thinking-forth.sourceforge.net" target="_blank">*Thinking Forth by Leo Brodie*</a>

## Benchmark
Performance-wise, running the Forth interpreter on top of the Java Virtual Machine (another interpreter) does not really make sense. JIT doesn't help much of the threading of Forth, either. It runs at about 1/10 of pure C++ implementation [eForth](https://github.com/chochain/eforth) or, sadly, at only 1/5 of its nemesis Javascript [weForth](https://github.com/chochain/weForth).

On the other hand, if you consider providing an interactive shell or a DSL for your Java app, this might be a path forward. Not yet, but it is certainly possible to extend ooeForth with real object and reflection/invocation to the vast ecosystem of Java libraries. That'll be fun.

Well, there's more. What if Forth words can be compiled directly into Java Bytecode? I did an experimental Bytecode interpreter here [nanoJVM](https://github.com/chochain/nanoJVM). Dr. Ting also tried an FPGA approach here [eJsv32](https://github.com/chochain/eJsv32).

## TODO
* embedded
* object stack + invocation
* reflection
* Java lib integration

## Revisions
### 202107 Dr. Ting's original. Now in ~/orig
### 202110 Tune up
    + tighten var names
    + add Immd class
    + #exec=>#nest
### 202507 Tune up
    + Add Var class
    + fix allot, does>, s", array!, array@
    + tune see
    + use var(0) for base
    + add "macro" methods (i.g. spaces, tgt, bool, alu) to reduce verbosity
    + move GUI to main
### 202508 Refactor
    + modulization
    + Code class to handle branching
    + add primitive as Consumer<*Code*>
    + use short methods as macros
    + add do..loop, rnd, ok, upgrade see
    + add include, included
    
