package com.amazon.ionsql.eval.builtins

internal val TIMESTAMP_FORMAT_SYMBOLS: Set<Char> =
    setOf(
        'y', //Year of era, e.g. "1978"; "78"
        'M', //Month of year (1-12)
        'L', //Month of year e.g. "Jan"; "January"
        'd', //day of month (1-31)
        'a', //am-pm of day
        'h', //Clock hour of am-pm (1-12)
        'H', //hour of day (0-23)
        'm', //Minute of hour (0-59)
        's', //Second of minute (0-59)

        //Note:  S and n both use ChronoField.NANO_OF_SECOND so we cannot remove support for one without
        //removing support for the other AFAIK.
        'S', //fraction of second, in milliseconds (0-999)
        'n', //Nano of second (0-999,999,999)

        //Note: Same with X, x O and Z for ChronoField.OFFSET_SECONDS
        'X', //Zone offset or Z for zero: e.g. "-08", "-0830", "-08:30", "-083000", "-08:30:00" Note: the seconds portion will always be "00" because Ion-Timestamp offset is specified in minutes
        'x', //Zone offset "+0000", "-08", "-0830", "-08:30", "-083000", "-08:30:00" Note: the seconds portion will always be "00" because Ion-Timestamp offset is specified in minutes
        'O', //Localized zone offset, e.g. "GMT+8", "GMT+08:00", "UTC-08:00";
        'Z'  //4 digit zone offset, e.g "+0000", "-0800", "-08:00"
    )
