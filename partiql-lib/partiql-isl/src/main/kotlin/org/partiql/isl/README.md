# IonSchema Model

## About

## Usage

> Run from partiql-lang-kotlin root

```shell
# Rebuild sprout if necessary
./gradlew :partiql-lib:partiql-sprout:install

# Generate ISL model
./partiql-lib/partiql-sprout/build/install/sprout/bin/sprout generate kotlin \
    -p org.partiql.isl \
    -u ion_schema \
    -o generated \
    ./partiql-lib/partiql-isl/src/main/resources/ion_schema_v2_0.ion
```
