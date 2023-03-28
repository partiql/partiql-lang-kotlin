# PartiQL Plan

## About

This package contains an early implementation of the PartiQL Plan data structures. Specification work is ongoing, and
this package should be considered experimental.

## Usage

The data structures in `org.partiql.plan` can be used via the latest [partiql-lang-kotlin](https://central.sonatype.com/artifact/org.partiql/partiql-lang-kotlin/0.9.3) JAR and using the AstToRel translator (`org.partiql.lang.planner.transforms.AstToRel`).

For example,

```kotlin
val parser = PartiQLParserBuilder.standard().build()
val ast = parser.parseAstStatement(input)
val plan = AstToPlan.transform(ast)

println(PlanPrinter.toString(plan))
```

## Generating Sources

> Have the code generator built, `./gradlew :lib:sprout:install`

```shell
# running from the package root
./lib/sprout/install/sprout/bin/sprout generate kotlin\
     -o ./ir\
     -p org.partiql.plan\
     -u Plan\
     -m DATA\
     --poems visitor --poems builder \
     ./partiql-plan/src/main/resources/partiql_plan.ion
```
