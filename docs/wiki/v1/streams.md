# PartiQL Data Streams

*This document defines the PSink and PSource in relation to Datum and Java primitives*

* * *

### Background

We have defined
an [encoding of PartiQL values using the Ion data format](https://quip-amazon.com/5Su8AQhKG5xA/PartiQL-Values-in-Ion),
but how does this fit in? Let’s look at two questions.

1. How does PartiQL represent values in memory?
2. How does PartiQL read values from a stream into memory?

An in-memory PartiQL value has a layer of indirection between the Java primitive and its view to the rest of the
program. This is called the “Datum” and is a fat interface which allows the partiql-lang-kotlin engine to not worry
about a value’s Java type, and instead switch on an int tag (ptype) to then pull out a value. Effectively the fat
interface removes checking classes and casting with tag checking then accessing without a cast. It’s effectively a
unifying interface over the old values, so how does the variant fit in?

A variant is an implementation of a Datum whose value is opaque to the rest of the system. When the system checks the
tag, it simply gets back “variant<T>” where T might tell us a set of capabilities (or traits) this type system / value
has. This value is not lowered into a PartiQL value such as an INT or ARRAY, but is kept in its “container” or “box”.
Think of the variant types of other engines or jsonb of PostgreSQL.

So how does PartiQL read values from a stream into Datums, and how does it handle variants? It depends because an
encoding may include a data type or it may not. Also, the reader itself may expect a type (or not). Consider that a
PartiQL value carries a type with it along with the value itself.

## Writing Data

### PSink

The PSink interface is used to write PartiQL data. It has APIs just like the IonWriter, and similarly, it has different
implementations for the actual encoding like how Ion has both a text and a binary encoding. A PSink is used without any
assumptions about the actual encoding.

### DatumWriter

The DatumWriter is a class which facilitates writing datums via a PSink implementation; it is handles materializing a
datum and calling the appropriate sink methods.

**Example**

```kotlin
val writer = DatumWriter.standard(sink)
writer.write(datum1)
writer.write(datum2)
writer.write(datum3)
writer.close()
```

### IonSink

This example shows how to encode a datum as Ion; type decorations are omitted where possible.

```kotlin
val sink = IonSink(System.out) // printing
val writer = DatumWriter(sink)

// bool
writer.write(Datum.bool(true))      // >> true

// ints
writer.write(Datum.smallint(1))     // >> smallint::1
writer.write(Datum.int(2))          // >> int::2
writer.write(Datum.bigint(3))       // >> 3

// exact and approx numeric
writer.write(Datum.decimal(BigDecimal("3.14"), 3, 2))   // >> ((decimal 3 2) 3.14)
writer.write(Datum.real(3.14f))                         // >> real::3.14e0
writer.write(Datum.doublePrecision(3.14))               // >> 3.14e0

// char strings
writer.write(Datum.char("abc", 3))      // >> ((char 3) "abc")
writer.write(Datum.varchar("abc", 3))   // >> ((varchar 3) "abc")
writer.write(Datum.string("abc"))       // >> "abc"

// lobs
writer.write(Datum.clob("hello".toByteArray()), 5)  // >> {{ "hello" }}
writer.write(Datum.blob("hello".toByteArray()), 5)  // >> {{ aGVsbG8= }}

// datetime
// TODO blocked on https://github.com/partiql/partiql-lang-kotlin/pull/1656

// ion

```

## Reading Data

### DatumReader

### PSource

PLACEHOLDER
