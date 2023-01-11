# Sprout

## Installation

> Note, all commands in this document are run from the partiql-lang-kotlin root

```shell
./gradlew :partiql-lib:partiql-sprout:install  
```

## Usage

```shell
$ ./partiql-lib/partiql-sprout/build/install/sprout/bin/sprout generate kotlin --help

Usage: sprout generate kotlin [-hV] [-o=<out>] [-p=<packageRoot>] [-u=<id>]
                              <file>
Generates Kotlin sources from type universe definitions
      <file>            Type definition file
  -h, --help            Show this help message and exit.
  -o, --out=<out>       Generated source output directory
  -p, --package=<packageRoot>
                        Package root
  -u, --universe=<id>   Universe identifier
```

**Example**
```shell
$ ./partiql-lib/partiql-sprout/build/install/sprout/bin/sprout generate kotlin \
      -o ./generated \
      -p org.partiql.isl \ 
      -u IonSchema \
      ./partiql-lib/partiql-isl/src/main/resources/ion_schema_v2_0.ion  
      
# sources are generated in partiql-lang-kotlin/generated/
```

## Considerations from ISL
- [ ] Inline product definitions
- [x] Imported non-node types
- [x] Root-level referencing
- [ ] Properties of sum types
- [ ] Default values (ie just write the value, the type can be inferred)
