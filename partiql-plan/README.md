# partiql-plan

This package holds logical plans which are composed of actions and operator trees.

* Operator (tree node interface)
* Rel & RelBase
* Rex & RexBase
* OperatorVisitor
* OperatorRewriter

## Operands

What are operands?

* Operands are an important concept
* An operand is an input to some operator.
* An operator may have more than one operand e.g. join (left and right).
* Rel operands are typically called "inputs"
* Operands unify inputs since PartiQL bridges rel/rex domains.
* Not all operators are operands e.g. the limit of RelLimit is a rex, but not an operand - it is an "arg"

| Operator        | Operands         | Arguments              |
|-----------------|------------------|------------------------|
| RelAggregate    | input            | measures, groups       |
| RelCorrelate    | left, right      | joinType               |
| RelDistinct     | input            |                        |
| RelExcept       | left, right      | all                    |
| RelExclude      | input            | exclusions             |
| RelFilter       | input            | predicate              |
| RelIntersect    | left, right      | all                    | 
| RelIterate      | rex              |                        |
| RelJoin         | left, right      | joinType, condition    |
| RelLimit        | input            | limit                  |
| RelOffset       | input            | offset                 |
| RelProject      | input            | projections            |
| RelScan         | rex              |                        |
| RelSort         | input            | collations             |
| RelUnion        | left, right      | all                    |
| RelUnpivot      | rex              |                        |
| RexArray        | values (vararg)  |                        |
| RexBag          | values (vararg)  |                        |
| RexCall         | args             |                        |
| RexCase         | match (optional) | branches, default      |
| RexCast         | operand          | type                   |
| RexCoalesce     | args (vararg)    |                        |
| RexDispatch     | args (vararg)    |                        |
| RexLit          |                  | value                  |
| RexNullIf       | v1, v2           |                        |
| RexPathIndex    | operand          | index                  |
| RexPathKey      | operand          | key                    |
| RexPathSymbol   | operand          | symbol                 |
| RexPivot        | input            | key, value             |
| RexSelect       | input            | constructor            |
| RexSpread       | args (vararg)    |                        |
| RexStruct       |                  | fields                 |
| RexSubquery     | input            |                        |
| RexSubqueryComp | input            | comparison, quantifier |
| RexSubqueryIn   | input            | args                   |
| RexSubqueryTest | input            | test                   |
| RexTable        |                  | table                  |
| RexVar          |                  | scope, offset          | 

## Design

For the rule and strategy patterns to work, we need to model classes whose operands have a stable ordering;
so we have defined an abstract base class for all operators which holds operands and controls the access to them.
We use base implementations for state management and enforcing operands ordering; however we use interfaces for the
top-level of each domain. The abstract bases can be extended, and the operator/rel/rex interfaces can be implemented
directly.

Why interfaces for top-level domain entities and not abstract base classes?

* We don’t want to force materialization of operands (consider wrapping a serde class)
* We don’t want to force holding state (aka having to call super constructors)
* The operator/rel/rex should be flexible for extension and the interface is the most open-ended / non-prescriptive.

Why abstract base classes for individual operators and not interfaces?

* Enforce operands ordering is the primary reason; also can memoize operands.
* Hold state such as mutable tags and computed types.
* We want the standard operators to be prescriptive but not limiting.
