package com.amazon.howero.fs

enum class FsFormat {
    TEXT,
    JSON,
    ION,
    B10N
}

enum class FsCompression {
    NONE,
    ZSTD,
    GZIP
}

class FsEncoding(
    val format: FsFormat,
    val compression: FsCompression,
) {

    companion object {

        fun parse(arg: String): FsEncoding {
            if (arg == "") {
                throw IllegalArgumentException("source encoding argument is null or empty")
            }
            val parts = arg.toUpperCase().split('/')
            return when (parts.size) {
                1 -> FsEncoding(FsFormat.valueOf(parts[0]), FsCompression.NONE)
                2 -> FsEncoding(FsFormat.valueOf(parts[0]), FsCompression.valueOf(parts[1]))
                else -> throw IllegalArgumentException("Invalid source encoding $arg")
            }
        }
    }
}
