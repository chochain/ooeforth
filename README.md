# ooeforth
## eForth in Java
Dr. Ting has been an influential figure in Forth community. He was the key person who brought Bill Munich's eForth into a wide range of platforms [see here](https://www.forth.org/OffeteStore/OffeteStore.html)

When I demonstrated that high-level languages such as C++, Java, even Javascript can also construct Forth dictionary without meta-compilation as were done in classic Forths, he got very excited. He stated that Forth can finally be built without bootstrapping from lower-level Forth scripts and termed the paradigm "Forth without Forth".

Taking the template I sent, Dr. Ting single-handily completed several reversions of ooeForth which I kept in ~/orig directory.

Dr. Ting has a peculiar interest in minimizing program line count and to keep his code packed without spacing. Though personally cared much more for readability and modularization, respect his preference, I gathered all the input into one file i.e. src/EforthTing.java. He proudly gave a presentation on Silicon Valley Forth Interest Group's July 2021 meeting of this development [see here](https://github.com/chochain/ooeforth/blob/master/docs/ooeforth204.ppt).

The project has been in frozen state since his passing late May 2022 and I've wondered into other spaces. Not until recently, late July 2025, I took notice that it might needs some maintenance. So, here we are.

## Build & Run
### Dr. Ting's style
javac -d tests src/EforthTing.java
java -cp tests EforthTing

### Modularization - Work in Progress
javac -sourcepath src -d tests src/Eforth.java
