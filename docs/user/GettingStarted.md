# Getting Started 

PartiQL provides an interactive shell, or Read Evaluate Print Loop (REPL),
that allows users to write and evaluate PartiQL queries. 

## Prerequisites 

PartiQL requires the Java Runtime (JVM) to be installed on your machine.
You can obtain the *latest* version of the Java Runtime from either

1. [OpenJDK](https://openjdk.java.net/install/), or [OpenJDK for Windows](https://developers.redhat.com/products/openjdk)  
1. [Oracle](https://www.oracle.com/technetwork/java/javase/downloads/index.html)

[Follow the instructions on how to set](https://docs.oracle.com/cd/E19182-01/820-7851/inst_cli_jdk_javahome_t/) 
`JAVA_HOME` to the path where your Java Runtime is installed. 

## Download the PartiQL REPL 
 
Each release of PartiQL comes with an archive that contains the PartiQL REPL as a
zip file.

1. [Download](https://github.com/partiql/partiql-lang-kotlin/releases).
You may have to click on `Assets` to see the zip and tgz archives.
the latest `partiql-cli`[^zipname] zip archive to your machine.
1. Expand (unzip) the archive on your machine. Expanding the archive yields the following folder structure:

[^zipname]: The file will append PartiQL's release version to the archive, i.e., `partiql-cli-0.1.0.zip`. 

```
├── partiql-cli
    ├── bin
    │   ├── partiql
    │   └── partiql.bat
    ├── lib
    │   └── ... 
    ├── README.md
    └── Tutorial
        ├── code
        │   └── ... 
        ├── tutorial.html
        └── tutorial.pdf
```

where `...` represents elided files/directories. 

The root folder `partiql-cli` contains a `README.md` file and 3 subfolders 

1. The folder `bin` contains startup scripts `partiql` for macOS and
Unix systems and `partiql.bat` for Windows systems. Execute these files
to start the REPL.
1. The folder `lib` contains all the necessary Java libraries needed to run PartiQL. 
1. The folder `Tutorial` contains the tutorial in `pdf` and `html`
form. The subfolder `code` contains 3 types of files:
    1. Data files with the extension `.env`. These files contains PartiQL
    data that we can query.
    1. PartiQL query files with the extension `.sql`. These files contain
    the PartiQL queries used in the tutorial.
    1. Sample query output files with the extension `.output`. These files
    contain sample output from running the tutorial queries on the
    appropriate data.




## Running the PartiQL REPL 

### Windows 

Run (double click on) `partiql.bat`. This should open a command-line
prompt and start the PartiQL REPL which displays:

```
Welcome to the PartiQL REPL!
PartiQL> 
```

### macOS (Mac) and Unix 

1. Open a terminal and navigate to the `partiql-cli`[^foldername] folder.
1. Start the REPL by typing `./bin/partiql` and pressing ENTER, which displays:

[^foldername]: The folder name will have the PartiQL version as a suffix, i.e., `partiql-cli-0.1.0`.

```
Welcome to the PartiQL REPL!
PartiQL>
```

## Testing the PartiQL REPL 

Let's write a simple query to verify that our PartiQL REPL is working. At the `PartiQL>` prompt type: 

```
PartiQL> SELECT * FROM [1,2,3]
```

and press `ENTER` *twice*. The output should look similar to: 

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
OK!
PartiQL> 
```

Congratulations! You successfully installed and run the PartiQL REPL.
The PartiQL REPL is now waiting for more input. 

To exit the PartiQL REPL, press: 

* `Control+D` in macOS or Unix 
* `Control+C` on Windows 

or close the terminal/command prompt window.


## Loading data from a file 

An easy way to load the necessary data into the REPL 
is use the `-e` switch when starting the REPL
and provide the name of a file that contains your data.


```
./bin/partiql  -e Tutorial/code/q1.env
```

You can then see what is loaded in the REPL's global environment using
the **special** REPL command `!global_env`, i.e.,

``` 
Welcome to the PartiQL REPL!
PartiQL> !global_env
   | 
===' 
{
  'hr': {
    'employees': <<
      {
        'id': 3,
        'name': 'Bob Smith',
        'title': NULL
      },
      {
        'id': 4,
        'name': 'Susan Smith',
        'title': 'Dev Mgr'
      },
      {
        'id': 6,
        'name': 'Jane Smith',
        'title': 'Software Eng 2'
      }
    >>
  }
}
--- 
OK!

```
