$ion_schema_1_0

type::{
    name: missing,
    type: $null,
    annotations: [required::$partiql_missing],
    valid_values: [null]
}

type::{
    name: bag,
    type: list,
    annotations: [required::$partiql_bag]
}

type::{
    name: date,
    type: {
        timestamp_precision: day
    },
    annotations: [required::$partiql_date]
}

type::{
    name: time,
    type: struct,
    fields: {
        hour:         {type: int, occurs: required, valid_values: range::[0,23]},
        min:          {type: int, occurs: required, valid_values: range::[0,59]},
        sec:          {type: int, occurs: required, valid_values: range::[0,59]},
        sec_fraction: {type: int, occurs: optional, valid_values: range::[0,999999]}
    },
    content: closed,
    annotations: [required::$partiql_time]
}