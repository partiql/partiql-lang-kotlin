# PartiQL Compiler Guide

This document is a guide to using the PartiQL compiler.

## Overview

The compiler is responsible for converting logical plans (an _operator_ tree)
to physical plans (an _expr_ tree) by applying _strategies_. A _strategy_ is a class
that has a _pattern_ and an _apply_ method. The pattern determines when to
invoke the _apply_ method which converts the matched operators (logical) to expressions (physical).

## Pattern Matching

A pattern is a tree composed of nodes with one of the types,

```
TYPE:   T – match if class T, check children.
ANY:    ? - match any node, check children.
ALL:    * - match all nodes in the subtree.
```

For backwards compatibility, a pattern will ignore unmatched children.
If this is not the desired behavior, a pattern can be marked as "strict"
which will match children exactly and error on extraneous children.

### Example

Let's combine a limit and offset into a single relational expression; the logical tree looks like this.

```
RelLimit         Pattern.match(..)
  \
   RelOffset       Pattern.match(..)
```

We use the builders to create a pattern.

```
Pattern.match(RelLimit::class)
       .child(Pattern.match(RelOffset::class))
       .build()
```

In practice, the compiler will be walking the tree so we must deal with the inputs which have been recursively compiled.
Because these nodes have been compiled, they are part of the "physical" or "Expr" domain. To illustrate, I've enclosed
the compiled children nodes with `< >` so `ExprValue -> <Value>` and `ExprRelation -> <Rel>`.

Recall the definition of a `RelLimit` and a `RelOffset`

```
* RelLimit(input: Rel, limit: Rex)
* RelOffset(input: Rel, offset: Rex)
```

I have labelled these children in the illustration so that you can see where the end up in the match.

```
...
  \
  RelLimit
  /       \
x:<Value>  RelOffset
        /      \
   y:<Value>   z:<Rel>
``` 

The compiler will look for this pattern in the operator tree, and produce a match like so,

```
Match {
   matched: [
      RelLimit,
      RelOffset,
   ],
   children: [
        [x:<Value>],
        [y:<Value>, z:<Rel> ]
   ],
}
```

The matched items are the flattened (in-order traversal) matched nodes from the pattern, while the children
is a nested list of corresponding compiled children.

This match structure is sent to the Strategy which gives the implementor all the information they need to know
to continue folding the tree.
