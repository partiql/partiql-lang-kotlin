# Getting Started 

\SqlName provides an interactive shell, or Read Eval Print Loop (REPL),
that allows users to write and evaluate \SqlName queries. 

## Prerequisites 

\SqlName requires the Java Runtime (JVM) to be installed on your machine.
You can obtain the *latest* version of the Java Runtime from either  

1. [OpenJDK](https://openjdk.java.net/install/), and [OpenJDK for Windows](https://developers.redhat.com/products/openjdk)  
1. [Oracle](https://www.oracle.com/technetwork/java/javase/downloads/index.html)

## Download the \SqlName REPL 
 
Each [release](https://github.com/partiql/partiql-lang-kotlin/releases)
of \SqlName comes with an archive that contains the \SqlName REPL as a
zip file.

1. [Download](https://github.com/partiql/partiql-lang-kotlin/releases/download/v0.1.0-alpha/partiql-cli-0.1.0.zip)
the latest `partiql-cli` zip archive to your machine.
1. Expand (unzip) the archive on your machine. Expanding the archive yields the following folder structure 

```
├── partiql-cli
│   ├── partiql
│   ├── partiql.bat
│   ├── partiql-cli.jar
│   └── README.md
│   └── tutorial.html
│   └── tutorial.pdf
```

## Running the \SqlName REPL 

### Windows 

Run (double click) on `particl.bat`. This should open a command line
prompt and start the \SqlName REPL. The \SqlName REPL prompt should look like this 

```
PartiQL> 
```

### OSX (Mac) and Unix 

1. Open a terminal and navigate to the `partiql-cli` folder we created when we extracted `partiql-cli.zip`. 
1. Run the executable `partiql` file, by typing `./partiql` and hit
enter. This should start the \SqlName REPL and should look like this

```
PartiQL> 
```

## Testing the \SqlName REPL 

Let's write a simple query to verify that our \SqlName REPL is working. At the `PartiQL>` prompt type 

```
PartiQL> SELECT * FROM [1,2,3]
```

and press `ENTER` *twice*. The output should look similar to 

```
PartiQL> SELECT * FROM [1,2,3]
   | 
===' 
{
  _1:1
}
{
  _1:2
}
{
  _1:3
}
--- 
Result type was BAG and contained 3 items
OK! (100 ms)
PartiQL> 

```

Congratulations! You succesfuly installed and run the \SqlName REPL.
The \SqlName REPL is now waiting for more input. To exit the \SqlName
REPL press `Control+D` or close the terminal/command
prompt window.  

