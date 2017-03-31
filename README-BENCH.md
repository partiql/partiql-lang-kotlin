# Ion SQL Sandbox Benchmarks
This package provides benchmark code using the [Java Microbenchmarking Harness (JMH)][jmh].
The code lives in the `bench` source tree and is a mixture of Java and Kotlin.

## JMH Driver
The `Benchmarks` main program has many options that drive how the benchmarks are run that
can be passed into commandline arguments.

### Direct Options
Example of defining patterns of benchmarks to run and restricting certain
injected parameters and writing the benchmark results to a CSV file

```
CsvsBench.*
-p fileName=PARKING-TINY.csv.gz
-p queryMode=RAW,COUNT
-rf CSV
-rff BENCH.csv
```

### JVM Parameters
Fast mode.  This runs smaller number of iterations and duration per iterations
for each benchmark and warmup.  This is useful to get a sense of the benchmark numbers
quickly.

```
-Dbenchmarks.fast=true
```

Profiling mode, this is useful for getting Java Flight Recorder telemetry to do sampled profiling.

```
-Dbenchmarks.profile=true
-Dbenchmarks.jfr.enable=true
-Dbenchmarks.jfr.path=/Users/almann/Desktop
```


## Build and Execution
Running the benchmarks can be done using the following command:

```
$ brazil-build benchmark
```

This will download dependencies and data from Brazil S3 and execute the default benchmarks.

### Ant Support
Properties to pump benchmark parameters through the Ant build.

```
$ brazil-build \
    -Dbench.cli.args='KinesisSerializeGetRecordsBench\.deserializeResponseFull 
                      KinesisSerializeGetRecordsBench\.serializeResponse 
                      -p recordSize=4096 
                      -p recordLimit=10,50 
                      -rf CSV 
                      -rff BENCH.csv' \
    benchmark >BENCH.out 2>&1
```

### Updating JMH
Currently we use S3 binary to store the self-contained JMH dependency tree outside of Brazil.
Updating these dependencies can be done via the following commands, assuming the
dependencies for the libraries are stored in `bench-lib` and the data files are stored
in the `bench-data` directories.

```
$ zip -r bench-lib.zip bench-lib/

  adding: bench-lib/ (stored 0%)
  adding: bench-lib/commons-math3-3.2-sources.jar (deflated 7%)
  adding: bench-lib/commons-math3-3.2.jar (deflated 11%)
  adding: bench-lib/jmh-core-1.18-sources.jar (deflated 8%)
  adding: bench-lib/jmh-core-1.18.jar (deflated 9%)
  adding: bench-lib/jmh-generator-annprocess-1.18-sources.jar (deflated 7%)
  adding: bench-lib/jmh-generator-annprocess-1.18.jar (deflated 9%)
  adding: bench-lib/jopt-simple-4.6-sources.jar (deflated 7%)
  adding: bench-lib/jopt-simple-4.6.jar (deflated 9%)

$ zip -r bench-data.zip bench-data
  adding: bench-data/ (stored 0%)
  adding: bench-data/PARKING-SMALL.csv.gz (deflated 0%)
  adding: bench-data/PARKING-TINY.csv.gz (deflated 0%)
  adding: bench-data/PARKING.csv.gz (deflated 0%)

$ brazil s3binary upload --file bench-lib.zip -k IonSQLSandbox-bench-lib.zip

$ brazil s3binary upload --file bench-data.zip -k IonSQLSandbox-bench-data.zip
```

When updating be sure to set the `bench.data.rev` and `bench.lib.rev` properties
in `build.xml`.

## IDE integration
The benchmark framework uses JARs that are downloaded to `build/private/bench-lib` as
part of the `brazil-build benchmark` target.  Those should be added to your IDE's classpath
as well as enabling *annotation processing* for the compiler as JMH uses this for code generation.

## TODO
* Replace this custom build solution with the one stated in the [wiki][jmh].  The version
  in Brazil is older and should be updated, but it is the correct long term approach.

[jmh]: https://w.amazon.com/index.php/JMH
