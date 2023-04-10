# PartiQL AST

This package contains data structures for the PartiQL Kotlin abstract syntax tree.

## Generating Sources

> Have the code generator built, `./gradlew :lib:sprout:install`

```shell
# running from the package root
./lib/sprout/install/sprout/bin/sprout generate kotlin\
     -o ./ir\
     -p org.partiql.ast\
     -u Ast\
     -m DATA\
     --poems visitor --poems builder \
     ./partiql-ast/src/main/resources/partiql_ast.ion
```
