# PartiQL Plan

## About

Early implementation of PartiQL Plan Representation.

## Generating Sources

Undecided if we will even generate the internal representation, but the serialized format will be generated.

**How To**
> Naming is bad right now, so I'm just going with Calcite's Rex and Rel for the expression and relation operator domains
> respectively. They are deliberately separated.
```shell
# running from partiql-plan/
$HOME/Desktop/sprout/bin/sprout generate kotlin\
 -o ./ir\
 -p org.partiql.plan.ir\
 -u Plan\
 -m DATA\
 --poems visitor --poems builder \
 ./src/main/resources/partiql_plan.ion
```
