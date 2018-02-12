# Contents 

1. [Overview](#ionsql-sandbox)
    1. [Getting Started](#getting-started)
1. [Documentation](#documentation)
    1. [User Documentation](#user-documentation)
    1. [Developer Documentation](#developer-documentation)
1. [Communication](#communication)
    1. [Tickets](#tickets)
    1. [Issues](#issues)
1. [Helpful Links](#helpful-links)


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

# Documentation 

Documentation is available as * `.md` files inside this git repo. 

## User Documentation 

Documentation related to IonSQL++ features, built in functions and the
Read Evaluate Print Loop (REPL), or command line, is under [docs/user](docs/user). 


## Developer Documentation 

Documentation related to IonSQL++ developement, including design documentation, release process and contribution guide, 
 is under [doc/dev](doc/dev). 


# Communication 

## IonSQL++ news and announcements

Subscribe to [ionsql-interest@](https://email-list.corp.amazon.com/email-list/expand-list/ionsql-interest)
for important updates and news about upcoming releases.

## Onboarding 

If your team is evaluating or interested in integrating with IonSQL we would love to hear from you. Please send an e-mail to 
[jonswd](mailto:jonwsd@amazon.com) with the following info

* Who you are, what is your team
* What is your use case
* Why did you choose IonSQL++
* Who are your customers

This information help us guide our backlog toward the needs of our clients

## Tickets

For reaching our on-call for a SEV-2 issue please use [Software / Ion / IonSQL](https://tt.amazon.com/quicklink/Q000802791).

## Issues

For non-SEV-2 issues, feature requests, or, general help, please use 

* [Search our SIM Queue](https://tiny.amazon.com/25bxnndy/IonSQLIssues)
  * [File a Bug Issue](https://issues.amazon.com/issues/create?template=964bf8dc-67c5-4ab6-9dc8-cf8db3258226)
    * [Parser Issue](https://issues.amazon.com/issues/create?template=6663e63b-ccce-4b77-ac78-8655acee3ad1)
    * [Evaluator Issue](https://issues.amazon.com/issues/create?template=e9dc99bc-776b-4022-b63c-f85bf95813cf)
    * [Web Demo Issue (experimental)](https://issues.amazon.com/issues/create?template=02c4067e-4389-47e1-8d3c-b965ed59c053)
 * [File a Feature Request](https://issues.amazon.com/issues/create?template=8c984b68-9765-41c9-939c-e69d67e4691f)
 * [Ask a question on IonSQL](https://sage.amazon.com/questions/ask?tags=IonSQL General Question)

# Helpful Links

 - [IonSQL++ Specification (working draft)](https://drive.corp.amazon.com/view/Ion%20SQL++/Ion%20SQL++%20Formal%20reference%20evolving%20draft.pdf)
 - [A Walkthrough of IonSQL++ (Video)](https://broadcast.amazon.com/videos/52396)
 - [IonSQL++ in the Browser](https://sapp.amazon.com/IonSqlpp/)
     * **WARNING** the Browser version of IonSQL++ is **experimental** and not always up-to-date with the latest release. 
       If you want to test the behavior of IonSQL++ **use the REPL** provided with this package. 
 - [Hyperlinked SQL-92 BNF](https://ronsavage.github.io/SQL/sql-92.bnf.html) - this is much easier to navigate than the official ISO standard!
 - [sqlfiddle.com](http://sqlfiddle.com/) - Use this to experiment with SQL as implemented by Postgres, MySQL, Oracle, MS SQL Server and SQLite. 


