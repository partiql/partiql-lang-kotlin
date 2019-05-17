# PartiQL Test Driver

Runs a bunch of tests against an implementation of PartiQL.

To run the test suite, from the project's root directory execute: 

```
$ ./gradlew :tools:sqltest:integrationTest
```

### Awesome Console

To turn the error messages from the driver into clickable links within the IntelliJ output window that point to
the locations of failed tests and therefore save yourself lots of time while writing and debugging tests, install
the [Awesome Console](https://plugins.jetbrains.com/plugin/7677-awesome-console) plugin.
 