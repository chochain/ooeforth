# ooeforth
## eForth in Java
Dr. Ting has been an influential figure in Forth community. He was the key person who brought Bill Munich's eForth into a wide range of platforms. [see here](https://www.forth.org/OffeteStore/OffeteStore.html)

When I demonstrated that high-level languages such as C++, Java, even Javascript can also construct Forth dictionary without meta-compilation as were done in classic Forths, he got very excited. He stated that Forth can finally be built without bootstrapping from lower-level Forth scripts and termed the paradigm "Forth without Forth".

He had an earlier implementation, called jeForth, with input from Shawn Chen and Brad Nelson. Upon taking the template I sent, Dr. Ting single-handily completed several reversions of ooeForth which I kept in **~/orig directory**. BTW, is it a Forth that supports Object-Orientated Programming? No! At least not at the moment. I think Dr. Ting appreciated the fact it is implemented in an object-oriented way and prefixed it OO.

Dr. Ting has a peculiar interest in minimizing program line count and keeping his code packed without spacing. Though personally cared much more for readability and modularization, respect his preference, I gathered all the sources into one file i.e. **src/EforthTing.java**. He proudly gave a presentation on Silicon Valley Forth Interest Group's July 2021 meeting of this development. [see here](https://github.com/chochain/ooeforth/blob/master/docs/)

The project has been in frozen state since his passing late May 2022 and I've wondered into other spaces. Not until recently, 3 years later, I took notice that it demands some maintenance. So, here we are.

## Build & Run
### Dr. Ting's style

    javac -d tests src/EforthTing.java
    java -cp tests EforthTing

### Modularization - Work in Progress

    javac -sourcepath src -d tests src/Eforth.java

## Benchmark
Performance-wise, running the Forth interpreter on top of the Java Virtual Machine (another interpreter) does not really make sense. JIT doesn't help much of the threading of Forth, either. It runs at about 1/10 of pure C++ implementation. see [eForth in C++](https://github.com/chochain/eforth).

On the other hand, if you consider providing an interactive shell or a DSL for your Java app, this might be a path forward. Not yet, but it is certainly possible to extend ooeForth with real object and reflection/invocation to the vast ecosystem of Java libraries. That'll be fun.

Well, there's more. What if Forth words can be compiled directly into Java Bytecode? I did an experimental Bytecode interpreter here [nanoJVM](https://github.com/chochain/nanoJVM). Dr. Ting also tried an FPGA approach here [eJsv32](https://github.com/chochain/eJsv32).

## TODO
* embedded
* reflection
* object stack + invocation

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
    + move GUI to main
