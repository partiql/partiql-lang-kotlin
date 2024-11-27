# partiql-plan

## Classes

* Operator
* Rel
* Rex

## Visitors

* Visitor
* Rewriter

## Design

For the rule and strategy patterns to work, we need to model classes whose children have a stable ordering;
so we have defined an abstract base class for all operators which holds children and controls the access to them.
We use base implementations for state management and enforcing children ordering; however we use interfaces for the
top-level of each domain. The abstract bases can be extended, and the operator/rel/rex interfaces can be implemented
directly.

Why interfaces for top-level domain entities and not abstract base classes?

* We don’t want to force materialization of children (consider wrapping a serde class)
* We don’t want to force holding state (aka having to call super constructors)
* The operator/rel/rex should be flexible for extension and the interface is the most open-ended / non-prescriptive.

Why abstract base classes for individual operators and not interfaces?

* Enforce children ordering is the primary reason; also can memoize children.
* Hold state such as mutable tags and computed types.
* We want the standard operators to be prescriptive but not limiting.
