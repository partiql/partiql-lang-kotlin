# PartiQL Rel

## About

This is an experimental library to represent PartiQL plans

It differs from partiql_logical in partiql.ion insofar as it is not an expansion of the SFW AST node as a value. The
goal here is to have a functional representation independent of the AST which is better suited for rewrites. As of now,
the combination of project (sql select) with bindings_to_tuples (partiql select value) creates an operational pipeline "
blocker" in which you cannot rearrange or pass-through operators. Bindings_to_tuples is described by a paper (with a
name related to physics) as "constructor" but its application in the sql select case is not necessary. At a minimum, we
need different projection operators for the cases of

- SELECT: binding tuples -> bindings tuples
- SELECT VALUE: binding tuples -> value

SELECT VALUE _is_ the general case, but the domain change is neither functional nor compact. It's not functional because
it cannot be composed with other operators (without a scan which creates the pipeline block). It's not compact because 
its unnecessary to round trip the values from binding tuples -> values -> binding tuples.

This work is experimental and not fully understood (at least by rchowell) at the moment. An accompanying formal 
specification is in the works.

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
