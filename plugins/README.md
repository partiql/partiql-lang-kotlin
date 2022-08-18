# PartiQL Plugins

1. Implement Plugin and Plugin.Factory
2. Define `META-INF/services/org.partiql.spi.Plugin$Factory`
3. Compile to JAR
4. Copy JAR to ~/.partiql/plugins/
5. `JAVA_OPTS="-cp ~/.partiql/plugins/echo-plugin.jar" ./cli/shell.sh`


## Examples

## Sample Queries

## Data
```
data
├── 10n
│   ├── service_log_legacy.10n
│   ├── service_log_legacy_all_values_as_symbols.10n.gz
│   └── service_log_legacy_short.ion
├── ion
│   ├── logentries.ion
│   ├── service_log.ion
│   └── service_log_legacy.ion
├── json
│   └── songs.json
└── text
    └── test.txt
```