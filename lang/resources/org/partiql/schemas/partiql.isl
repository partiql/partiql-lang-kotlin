$ion_schema_1_0

type::{
    name: missing,
    type: $null,
    annotations: [required::$missing],
    valid_values: [null]
}

type::{
    name: bag,
    type: list,
    annotations: [required::$bag]
}

type::{
    name: date,
    type: {
        timestamp_precision: day
    },
    annotations: [required::$date]
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
    annotations: [required::$time]
}