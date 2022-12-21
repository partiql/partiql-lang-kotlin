# Sprout

## Installation

```
gg :partiql-lib:partiql-sprout:install  
```

## Usage

```
> $ ./partiql-lib/partiql-sprout/build/install/sprout/bin/sprout --help

Usage: generate [-hV] [-I=<includes>] [-o=<out>] [-p=<packageRoot>]
                [-s=<syntax>] <file>
Generates Kotlin sources from type universe definitions
      <file>              Type definition file
  -h, --help              Show this help message and exit.
  -I=<includes>           Path of included type definitions
  -o, --out=<out>         Generated source output directory
  -p, --package=<packageRoot>
                          Package root
  -s, --syntax=<syntax>   Syntax version for the type universe definition
  -V, --version           Print version information and exit.
```

## Considerations from ISL
- [ ] Inline product definitions
- [ ] Imported non-node types
- [ ] Root-level referencing
- [ ] Properties of sum types
- [ ] Default values (ie just write the value, the type can be inferred)
