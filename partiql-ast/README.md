# PartiQL AST

This package contains data structures for the PartiQL Kotlin abstract syntax tree.

## Generating Sources

> Have the code generator built, `./gradlew :lib:sprout:install`

```shell
# running from the package root
./lib/sprout/build/install/sprout/bin/sprout generate kotlin\
     -o ./ast\
     -p org.partiql.ast\
     -u Ast\
     -m DATA\
     --poems visitor --poems identifier --poems builder\
     ./partiql-ast/src/main/resources/partiql_ast.ion
```