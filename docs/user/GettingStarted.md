# Getting Started 

PartiQL provides an interactive shell, or Read Eval Print Loop (REPL),
that allows users to write and evaluate PartiQL queries. 

## Prerequisites 

PartiQL requires the Java Runtime (JVM) to be installed on your machine.
You can obtain the *latest* version of the Java Runtime from either  

1. [OpenJDK](https://openjdk.java.net/install/), and [OpenJDK for Windows](https://developers.redhat.com/products/openjdk)  
1. [Oracle](https://www.oracle.com/technetwork/java/javase/downloads/index.html)

[Follow the instructions on how to set](https://docs.oracle.com/cd/E19182-01/820-7851/inst_cli_jdk_javahome_t/) 
`JAVA_HOME` to the path where your Java Runtime is installed. 

## Download the PartiQL REPL 
 
Each [release](https://github.com/partiql/partiql-lang-kotlin/releases)
of PartiQL comes with an archive that contains the PartiQL REPL as a
zip file.

1. [Download](https://github.com/partiql/partiql-lang-kotlin/releases/download/v0.1.0-alpha/partiql-cli-0.1.0.zip)
the latest `partiql-cli` zip archive to your machine.
1. Expand (unzip) the archive on your machine. Expanding the archive yields the following folder structure 

```
├── partiql-cli
│   ├── bin
│   │   ├── partiql
│   │   └── partiql.bat
│   ├── lib
│   │   └── ... 
│   ├── README.md
│   └── Tutorial
│       ├── code
│       │   └── ... 
│       ├── tutorial.html
│       └── tutorial.pdf
```

We have used ellipsis `...` to elide files/directories. 

The root folder `partiql-cli` contains a `README.md` file and 3 subfolders 

1. The folder `bin` contains startup scripts `partiql` for OSX (Mac) and
Unix systems and `partiql.bat` for Windows systems. Execute these files
to start the REPL
1. The folder `lib` contains all the necessary java libraries needed to run PartiQL. 
1. The folder `Tutorial` contains the tutorial in `pdf` and `html` form. The subfolder `code` contains 3 types of files 
    1. Data files with the extension `.env`. These files contains PartiQL
    data that we can query
    1. PartiQL query files with the extension `.sql`. These files contain
    the PartiQL queries used in the tutorial.
    1. Sample query output files with the extension `.out`. These files
    contain sample output from running the tutorial queries on the
    appropriate data.




## Running the PartiQL REPL 

### Windows 

Run (double click) on `particl.bat`. This should open a command line
prompt and start the PartiQL REPL. The PartiQL REPL prompt should look like this 

```
Welcome to the PartiQL REPL!
PartiQL> 
```

### OSX (Mac) and Unix 

1. Open a terminal and navigate to the `partiql-cli` folder we created when we extracted `partiql-cli.zip`. 
1. Run the executable `partiql` file, by typing `./partiql` and hit
enter. This should start the PartiQL REPL and should look like this

```
Welcome to the PartiQL REPL!
PartiQL>
```

## Testing the PartiQL REPL 

Let's write a simple query to verify that our PartiQL REPL is working. At the `PartiQL>` prompt type 

```
PartiQL> SELECT * FROM [1,2,3]
```

and press `ENTER` *twice*. The output should look similar to 

```
PartiQL> SELECT * FROM [1,2,3]
   | 
===' 
<<
  {
    '_1': 1
  },
  {
    '_1': 2
  },
  {
    '_1': 3
  }
>>
--- 
OK! (86 ms)
PartiQL> 
```

>
> **INFO** 
>
> An easy way to load the necessary data into the REPL 
> is use the `-e` switch when starting the REPL
> and providing the name of a file that contains your data.
>
> 
> ```
> ./bin/partiql  -e Tutorial/code/q1.env
> ```
>
> You can then see what is loaded in the REPL's global environment using
> the **special** REPL command `!global_env`, e.g., 
> 
> ``` 
> Welcome to the PartiQL REPL!
> PartiQL> !global_env
>    | 
> ===' 
> {
>   'hr': {
>     'employees': <<
>       {
>         'id': 3,
>         'name': 'Bob Smith',
>         'title': NULL
>       },
>       {
>         'id': 4,
>         'name': 'Susan Smith',
>         'title': 'Dev Mgr'
>       },
>       {
>         'id': 6,
>         'name': 'Jane Smith',
>         'title': 'Software Eng 2'
>       }
>     >>
>   }
> }
> --- 
> OK! (6 ms)
> 
> ```
> 

Congratulations! You succesfuly installed and run the PartiQL REPL.
The PartiQL REPL is now waiting for more input. 

To exit the PartiQL REPL press 

* `Control+D` in OSX or Unix 
* `Control+C` on Windows 

or close the terminal/command prompt window.

