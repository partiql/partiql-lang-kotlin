plugins {
    id(Plugins.application)
    id(Plugins.conventions)
}

dependencies {
    implementation(Deps.avro)
    implementation(Deps.csv)
    implementation(Deps.hadoopCommon)
    // ??? without this I was getting linking errors at runtime
    implementation("org.apache.hadoop:hadoop-mapreduce-client-core:3.3.6")
    implementation(Deps.ionElement)
    implementation(Deps.parquet)
    implementation(Deps.parquetAvro)
    implementation(Deps.parquetHadoop)
    implementation(Deps.picoCli)
    implementation(Deps.trinoTPC)
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
