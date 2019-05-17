# Introduction

**Note that this document is [subject to change](https://github.com/partiql/partiql-lang-kotlin/issues/43) as required by the use 
of `ktlint`.**

This document serves as style guide and includes code conventions and idioms for [Kotlin](https://kotlinlang.org/) 
in the \SqlName project.
 
We use [Kotlin official coding conventions](http://kotlinlang.org/docs/reference/coding-conventions.html) document as 
a base. If it's not specified here use that as a reference. 

If you use Intellij you can import the code-style settings [here](./intellij_code_style.xml)  

# Packages
Maintain directory structure and package names consistent, e.g. foo.bar should be in foo/bar folder. Keeping both 
consistent makes easier to find any resource, e.g. class or function, that is part of the package and naturally groups 
them all.

# Imports

Use `*` imports to avoid polluting the import list and alphabetical order to simplify git merges.

# Control Flow

Use `when` instead of `if else if` when possible, e.g.:

```kotlin
// Bad
fun foo(i: Int) {
    if(i == 0){
        // (...)
    }
    else if (i in 1..10){
        // (...)
    }
    else {
        // (...)
    }
}

// Good
fun foo(i: Int) {
    when(i)
    {
        0 -> // (...)
        in 0..10 -> // (...)
        else -> // (...)
    }
}

```

Apart from being cleaner `when` is safer when operating over sealed 
[classes](http://kotlinlang.org/docs/reference/sealed-classes.html), e.g.

```kotlin
sealed class Shape {}
data class Round(val radius: Double) : Shape() {}
data class Square(val width: Double, val height: Double) : Shape() {}

// compilation error as it's missing Square and has no `else` clause
fun calculateArea(shape: Shape): Double = when(shape) {
    is Square -> shape.width * shape.height
}

// works and will give a compilation error when another Shape is introduced
fun calculateArea(shape: Shape): Double = when(shape) {
    is Square -> shape.width * shape.height
    is Round -> shape.radius*shape.radius*Math.PI
}

// Won't give any compilation error when another Shape is introduced forcing you to implement a runtime failure
fun calculateArea(shape: Shape): Double {
    if(shape is Square) {
        return shape.width * shape.height
    }
    else if (shape is Round) {
        return shape.radius*shape.radius*Math.PI
    }

    throw new RuntimeException("unknown shape $shape")
}
```

# Kotlin Extension Functions

We should limit the scope of helper extensions to avoid polluting clients and avoid clashes.

```kotlin
// bad: forAll will be seen by client
fun <T> List<T>.forAll(predicate: (T) -> Boolean): Boolean = // (...)

// good: Only seen by the interpreter library itself   
internal fun <T> List<T>.forAll(predicate: (T) -> Boolean): Boolean = // (...)
```

While this is not as much an issue for Java clients where extensions are exposed as static methods, e.g.:
`public static PackageNameKt.forAll(...)`, we should avoid exposing internal APIs externally as much as possible
to avoid creating opportunity for bad coupling with clients.

Consider [inline](https://kotlinlang.org/docs/reference/inline-functions.html) for extensions. Doing so can lead to 
better performance as avoids method call overhead and avoids the Java interoperability issue of exposing `static` APIs.

Separate module scoped extensions in their own class inside `org.partiql.lang.util`, see `CollectionExtensions` 
inside that package for an example.

# Lambdas

Name big lambda expressions by transforming them in `val` or functions. example:

```kotlin
// bad: hard to tell what what it's being filtered
val numbers = IntRange(1, 100).filter {
    if (it <= 0) false
    else if (it <= 3) true
    else if (it % 2 ==0 || it % 3 == 0) false
    else {
        var i = 5
        var r = true
        while (i * i <= it && r) {
            if(it % i == 0 || it % (i + 2) == 0) {
                r = false
            }
            i += 6
        }

        r
    }
}

// good
val isPrime = { n: Int -> /* (...) */ }
val numbers = IntRange(1, 100).filter(isPrime)

// better: a name helps to understand the filter intent  
fun isPrime(n: Int): Boolean { /* (...) */ }
val numbers = IntRange(1, 100).filter(::isPrime)

// best, extension functions for this particular example fits well   
internal fun Int.isPrime(): Boolean { /* (...) */ }
val numbers = IntRange(1, 100).filter { it.isPrime() }
```

# Visibility Modifiers
Be mindful of what should be exposed to clients and what should be internal to the module. The library flexibility 
should be by design, being more restrictive avoids accidental coupling with internal parts of the library making future 
refactorings simpler and safer

**TODO** example

In particular, instance dispatched extension methods should always be private, e.g.:

```kotlin
class SomeClass {
    //...
    
    fun A.fooOperation() {  // <-- this is a no-no because it is public.
        //...
    }
    
    
    protected fun A.barOperation() {  // <-- this is a no-no because it is protected.
        //...
    }
    
    private fun A.batOperation() { // <-- this is ok because it is private.
        //...
    }
    //...
}
```

# Java Interop

If intending to expose a companion object field publicly, use the 
[@JvmFeild](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-field/) annotation. Doing so will expose them 
as `final static` members of the class e.g.:

```kotlin
class Foo {
    companion object {
        val BAD = "bad"
        @JvmField val GOOD = "good"
    }
}

// usage in Java
Foo.Companion.BAD
Foo.GOOD
```

Use `@JvmOverloads` in functions with default parameters to generate the java overloaded version of the method. 
By default java will only have access to the full method signature, e.g.:
  
```kotlin
@JvmOverloads fun good(arg1: Int, arg2: Int = 1) = arg1 + arg2
fun bad(arg1: Int, arg2: Int = 1) = arg1 + arg2   
```
  
In Java the following function signatures will be accessible:

```java
public Integer good(final Integer arg1, final Integer arg2) { return arg1 + arg2; }
public Integer good(final Integer arg1) { return good(arg1, 1); }
public Integer bad(final Integer arg1, final Integer arg2) { return arg1 + arg2; }
```

Avoid using default arguments in interface methods, prefer overloading it instead. It's not possible to use 
`@JvmOverloads` in interface methods to generate the overloads to Java from an interface so you have to do it by hand 
to maintain symmetric interface for Java and Kotlin.  
