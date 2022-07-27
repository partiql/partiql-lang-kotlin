# CLI/REPL Changes between Major Versions

## v0.2.* (latest v0.2.7)

* New features
    * Version number and commit hash in REPL
    * `PARTIQL_PRETTY` output-format added for non-interactive CLI users
* Misc/bug fixes
    * [fix] close CLI stream correctly
    * [fix] preserve negative zero when writing values to the CLI/REPL

## v0.3.* (latest v0.3.4)

* No changes

## v0.4.* (latest v0.4.0)

* No changes

## v0.5.* (latest v0.5.0)

* New features
    * Replaces CSV file reader with Apache’s CSVParser 
    * Adds ability to read custom CSV configurations
* Misc/bug fixes
    * [fix] CLI command bug when input data is empty
    * [fix] CLI bug when outputting IONTEXT to file

## v0.6.* (latest v0.6.0)

* New features
    * Adds permissive evaluation mode option to CLI/REPL

## v0.7.* (latest v0.7.0)

* New features
    * Replacement of REPL with [JLine](https://jline.github.io/) shell
    * Syntax highlighting for CLI
    * Three additional CLI flags related to evaluation options:
        * `-r --projection-iter-behavior:` Controls the behavior of ExprValue.iterator in the projection result: (default: FILTER_MISSING) [FILTER_MISSING, UNFILTERED]
        * `-t --typed-op-behavior`: indicates how CAST should behave: (default: HONOR_PARAMETERS) [LEGACY, HONOR_PARAMETERS]
        * `-v --undefined-variable-behavior`: Defines the behavior when a non-existent variable is referenced: (default: ERROR) [ERROR, MISSING]
    * `--input-format` flag to the CLI
    * An optional flag, `--wrap-ion`, to give users the old functionality of reading multiple Ion values to support the previous behavior
* Misc/bug fixes
    * `query_ddb` function added to allow for querying AWS DDB from the CLI
    * [fix] `write_file` CLI function; the old function required the input to be a `string`, but it must be a generic type
    * [adjust] handling of Ion input (requires a single value)
    * [adjust] handling of Ion output (outputting the actual evaluation result value)

