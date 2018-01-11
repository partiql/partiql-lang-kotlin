
# IonSQL++ Sandbox
This package is a basic implementation of Ion SQL++, a generalized extended SQL language
that uses the Ion type system natively, but is designed to be used over sparse, hierarchical
data in a relational way.

## Getting Started
Building the package requires a standard Brazil workspace and should be buildable against `live`.

```
$ brazil-build release
```

Building a self-contained JAR with a very basic read-eval-print loop (REPL), can be done
using the `single-jar` target:

```
$ brazil-build single-jar
```

Once this is done the REPL can be used to experiment with Ion SQL.

## Documentation 

Documentation is available as * `.md` files inside this git repo. 

### User Documentation 

Documentation related to IonSQL++ features, built in functions and the
Read Evaluate Print Loop (REPL), or command line, is under [docs/user](docs/user). 


### Developer Documentation 

Documentation related to IonSQL++ developement, including design documentation, release process and contribution guide, 
 is under [doc/dev](doc/dev). 

## Helpful Links

 - [IonSQL++ Specification (working draft)](https://drive.corp.amazon.com/view/Ion%20SQL++/Ion%20SQL++%20Formal%20reference%20evolving%20draft.pdf)
 - [A Walkthrough of IonSQL++ (Video)](https://broadcast.amazon.com/videos/52396)
 - [IonSQL++ in the Browser](https://sapp.amazon.com/IonSqlpp/)
     * **WARNING** the Browser version of IonSQL++ is **experimental** and not always up-to-date with the latest release. 
       If you want to test the beahviour of IonSQL++ **use the REPL** provided with this package. 
 - [Hyperlinked SQL-92 BNF](https://ronsavage.github.io/SQL/sql-92.bnf.html) - this is much easier to navigate than the official ISO standard!
 - [sqlfiddle.com](http://sqlfiddle.com/) - Use this to experiment with SQL as implemented by Postgres, MySQL, Oracle, MS SQL Server and SQLite. 


