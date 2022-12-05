# Experimental features
Even though the core semantics of PartiQL is mostly stable, we have been rapidly adding new features and improving our public APIs. 

To prevent potential issues, we leverage the [Kotlin Opt-in requirements](https://kotlinlang.org/docs/opt-in-requirements.html) to inform user about the APIs/features that are in a pre-stable stage. Explicit consent is required to use those features.

If no explicit consent is given, the compiler will warn users of such APIs about these conditions and requires them to opt in before using the API.

## Code Example: 

Suppose you would like to use the `PartiQLCompilerPipeline` which is currently in the experimental stage: 
```kotlin
@OptIn(PartiQLExperimental::class)
fun getPipeline() {
    val pipeline = PartiQLCompilerPipeline.standard()
    // ...
}
```
To see a complete example, check [examples/ExperimentalFeatureExample.kt].

Note that:

If your kotlin version is less than 1.7.0, you may need to add the `-opt-in` compiler option in your build file.

If your kotlin version is less than 1.6.0, you may need to add the `-Xopt-in` compiler option in your build file. 

To see an example, check [examples/build.gradle.kts].