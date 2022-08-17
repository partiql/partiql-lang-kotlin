# PartiQL Plugins

1. Implement Plugin and Plugin.Factory
2. Define `META-INF/services/org.partiql.spi.Plugin$Factory`
3. Compile to JAR
4. Copy JAR to ~/.partiql/plugins/
5. `JAVA_OPTS="-cp ~/.partiql/plugins/echo-plugin.jar" ./cli/shell.sh`

