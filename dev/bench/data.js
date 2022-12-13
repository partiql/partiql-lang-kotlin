window.BENCHMARK_DATA = {
  "lastUpdate": 1670971577565,
  "repoUrl": "https://github.com/partiql/partiql-lang-kotlin",
  "entries": {
    "JMH Benchmark": [
      {
        "commit": {
          "author": {
            "email": "40360967+johnedquinn@users.noreply.github.com",
            "name": "John Ed Quinn",
            "username": "johnedquinn"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "d951d1dadbb51c6c4af3c2a9ce1b89ceef1ea566",
          "message": "Adds performance benchmarking to GitHub actions (#921)",
          "timestamp": "2022-12-09T10:01:46-08:00",
          "tree_id": "65a9eac9f3dd0904ce46e474dd382e279a9867c5",
          "url": "https://github.com/partiql/partiql-lang-kotlin/commit/d951d1dadbb51c6c4af3c2a9ce1b89ceef1ea566"
        },
        "date": 1670611913713,
        "tool": "jmh",
        "benches": [
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLCompiler15",
            "value": 128.41518940764033,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLCompiler30",
            "value": 246.67271718384504,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLEvaluator15",
            "value": 470422.24778333324,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLEvaluator30",
            "value": 900086.233475,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLEvaluator30WithData10",
            "value": 9104016.73315,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLParser15",
            "value": 219.65715455577896,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLParser30",
            "value": 432.31148015181526,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameCaseWhenThen",
            "value": 41.48478243831742,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameComplexQuery",
            "value": 55.31526303063994,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameComplexQuery01",
            "value": 264.3161755523905,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameComplexQuery02",
            "value": 454.7948751600182,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameExceptUnionIntersectSixty",
            "value": 212.25758913759188,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameExec20Expressions",
            "value": 65.63204577659678,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameFromLet",
            "value": 42.603898061312165,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameGraphPattern",
            "value": 40.94644502614413,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameGraphPreFilters",
            "value": 65.97474589962158,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameGroupLimit",
            "value": 44.76951292484179,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameLongFromSourceOrderBy",
            "value": 67.21172766199227,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameManyJoins",
            "value": 59.22533552725597,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameNestedAggregates",
            "value": 100.71052781189556,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameNestedParen",
            "value": 18.43879125540402,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNamePivot",
            "value": 67.1871238914466,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQuery15OrsAndLikes",
            "value": 218.30895510963018,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQuery30Plus",
            "value": 113.58667058545313,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQueryFunc",
            "value": 46.607174696260216,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQueryFuncInProjection",
            "value": 93.03275868464246,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQueryList",
            "value": 81.16881189195995,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQueryNestedSelect",
            "value": 652.3669120586088,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQuerySimple",
            "value": 16.769347281685683,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSeveralJoins",
            "value": 22.557861778794763,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSeveralProjections",
            "value": 74.70689592165243,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSeveralSelect",
            "value": 191.9332807235607,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSimpleInsert",
            "value": 31.452461592894924,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSomeJoins",
            "value": 23.018190808136232,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSomeProjections",
            "value": 31.663583673081934,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSomeSelect",
            "value": 51.45697278609502,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameTimeZone",
            "value": 25.72198776720207,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameVeryLongQuery",
            "value": 319.470616845473,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameVeryLongQuery01",
            "value": 1008.1866219413444,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameCaseWhenThen",
            "value": 31.623994466291855,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameComplexQuery",
            "value": 277.8287832093838,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameComplexQuery01",
            "value": 133.08035555117442,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameExceptUnionIntersectSixty",
            "value": 249.27828258315603,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameExec20Expressions",
            "value": 74.40793008342153,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameFromLet",
            "value": 43.99783313675827,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameGraphPattern",
            "value": 46.328695919734955,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameGraphPreFilters",
            "value": 80.69422617297542,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameGroupLimit",
            "value": 42.95256562318443,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameLongFromSourceOrderBy",
            "value": 158.73850588627215,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameManyJoins",
            "value": 49.578919981227884,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameNestedAggregates",
            "value": 107.52385483948672,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameNestedParen",
            "value": 92.45607927122651,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNamePivot",
            "value": 77.73631644075547,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQuery15OrsAndLikes",
            "value": 216.98099921879748,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQuery30Plus",
            "value": 72.09943122424941,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQueryFunc",
            "value": 140.93640010833195,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQueryFuncInProjection",
            "value": 115.46700031281371,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQueryList",
            "value": 103.2232340053647,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQueryNestedSelect",
            "value": 151.43680853044685,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQuerySimple",
            "value": 14.642172358674964,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSeveralJoins",
            "value": 90.1319166460166,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSeveralProjections",
            "value": 64.5058984124318,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSeveralSelect",
            "value": 128.8824628727895,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSimpleInsert",
            "value": 25.5422372439746,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSomeJoins",
            "value": 24.455241398577062,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSomeProjections",
            "value": 21.215793161684186,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSomeSelect",
            "value": 41.46970617854238,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameTimeZone",
            "value": 9.904450114308565,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameVeryLongQuery",
            "value": 512.1820898867547,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameVeryLongQuery01",
            "value": 1323.778095673038,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.PartiQLBenchmark.testPartiQLCompiler",
            "value": 10.127466631360818,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.PartiQLBenchmark.testPartiQLEvaluator",
            "value": 2.8982132451074993,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.PartiQLBenchmark.testPartiQLParser",
            "value": 13.600484542931795,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "RCHowell@users.noreply.github.com",
            "name": "R. C. Howell",
            "username": "RCHowell"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "07b3845e4037c51b6c1c73b06315cfcfe31fe70d",
          "message": "Adds 0.9 migration guide (#930)",
          "timestamp": "2022-12-13T13:56:21-08:00",
          "tree_id": "fe0a4cb33fad032a9c804994a85e168b5d908232",
          "url": "https://github.com/partiql/partiql-lang-kotlin/commit/07b3845e4037c51b6c1c73b06315cfcfe31fe70d"
        },
        "date": 1670971576692,
        "tool": "jmh",
        "benches": [
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLCompiler15",
            "value": 127.4950775161687,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLCompiler30",
            "value": 248.71770097851396,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLEvaluator15",
            "value": 468239.4900666668,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLEvaluator30",
            "value": 914197.7606249998,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLEvaluator30WithData10",
            "value": 8944168.36105,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLParser15",
            "value": 219.76953991884284,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.MultipleLikeBenchmark.testPartiQLParser30",
            "value": 431.05606440793053,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameCaseWhenThen",
            "value": 41.51651605261266,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameComplexQuery",
            "value": 56.040884719497306,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameComplexQuery01",
            "value": 267.34881080059336,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameComplexQuery02",
            "value": 450.69914083835585,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameExceptUnionIntersectSixty",
            "value": 211.99009456312666,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameExec20Expressions",
            "value": 61.75686556279716,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameFromLet",
            "value": 42.68430178964671,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameGraphPattern",
            "value": 41.20925937935259,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameGraphPreFilters",
            "value": 63.97065652030652,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameGroupLimit",
            "value": 44.03970452835531,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameLongFromSourceOrderBy",
            "value": 57.721590032183336,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameManyJoins",
            "value": 59.57073352687488,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameNestedAggregates",
            "value": 101.31512506888134,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameNestedParen",
            "value": 18.526119406333954,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNamePivot",
            "value": 66.29605173092703,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQuery15OrsAndLikes",
            "value": 213.97471632302123,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQuery30Plus",
            "value": 112.72243262840732,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQueryFunc",
            "value": 46.1051449016223,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQueryFuncInProjection",
            "value": 88.53288708367384,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQueryList",
            "value": 76.76146476607934,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQueryNestedSelect",
            "value": 619.0866576083141,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameQuerySimple",
            "value": 16.1264810361495,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSeveralJoins",
            "value": 22.542479337717054,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSeveralProjections",
            "value": 73.6808998813253,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSeveralSelect",
            "value": 190.59261397808115,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSimpleInsert",
            "value": 30.74527015393189,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSomeJoins",
            "value": 22.66766054095715,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSomeProjections",
            "value": 32.068925464507785,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameSomeSelect",
            "value": 50.52980084102937,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameTimeZone",
            "value": 25.397094489090094,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameVeryLongQuery",
            "value": 297.83480020240455,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseFailNameVeryLongQuery01",
            "value": 995.7865607003198,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameCaseWhenThen",
            "value": 30.667655273559756,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameComplexQuery",
            "value": 264.7604450446071,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameComplexQuery01",
            "value": 125.40915685683403,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameExceptUnionIntersectSixty",
            "value": 258.3893683452456,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameExec20Expressions",
            "value": 74.48567381253397,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameFromLet",
            "value": 43.70155668312162,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameGraphPattern",
            "value": 45.632037926974704,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameGraphPreFilters",
            "value": 81.82407779227057,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameGroupLimit",
            "value": 41.98249712127088,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameLongFromSourceOrderBy",
            "value": 149.7743032353722,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameManyJoins",
            "value": 50.1858984654856,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameNestedAggregates",
            "value": 109.85141849034024,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameNestedParen",
            "value": 93.35875316083047,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNamePivot",
            "value": 82.08339753345798,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQuery15OrsAndLikes",
            "value": 208.52087409878686,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQuery30Plus",
            "value": 73.19351578452434,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQueryFunc",
            "value": 146.61463322580974,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQueryFuncInProjection",
            "value": 117.85559782433457,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQueryList",
            "value": 98.27084001851861,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQueryNestedSelect",
            "value": 148.7529543364451,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameQuerySimple",
            "value": 14.189807766739538,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSeveralJoins",
            "value": 87.87636192012704,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSeveralProjections",
            "value": 66.24416372724657,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSeveralSelect",
            "value": 127.75701972465114,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSimpleInsert",
            "value": 24.488196087465937,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSomeJoins",
            "value": 24.257876965193294,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSomeProjections",
            "value": 21.088553513643888,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameSomeSelect",
            "value": 40.147234813105115,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameTimeZone",
            "value": 9.979642248998903,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameVeryLongQuery",
            "value": 463.77607194684134,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.ParserBenchmark.parseNameVeryLongQuery01",
            "value": 1337.4867855191321,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.PartiQLBenchmark.testPartiQLCompiler",
            "value": 10.158435894456172,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.PartiQLBenchmark.testPartiQLEvaluator",
            "value": 2.9515044207428742,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          },
          {
            "name": "org.partiql.jmh.benchmarks.PartiQLBenchmark.testPartiQLParser",
            "value": 13.414582712489906,
            "unit": "us/op",
            "extra": "iterations: 10\nforks: 2\nthreads: 1"
          }
        ]
      }
    ]
  }
}