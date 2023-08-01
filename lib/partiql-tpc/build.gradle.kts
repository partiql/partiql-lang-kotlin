plugins {
    id(Plugins.application)
    id(Plugins.conventions)
}

dependencies {
    implementation(Deps.csv)
    implementation(Deps.picoCli)
    implementation(Deps.ionElement)
    implementation("io.trino.tpcds:tpcds:1.4")
    implementation("org.apache.parquet:parquet:1.13.1")
}

kotlin {
    explicitApi = null
}

ktlint {
    ignoreFailures.set(true)
}

application {
    applicationName = "partiql-tpc"
    mainClass.set("org.partiql.lib.tpc.Main")
}
