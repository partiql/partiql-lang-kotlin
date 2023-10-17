--#[exclude-01]
SELECT * EXCLUDE c.ssn FROM [
    {
        'name': 'Alan',
        'custId': 1,
        'address': {
            'city': 'Seattle',
            'zipcode': 98109,
            'street': '123 Seaplane Dr.'
        },
        'ssn': 123456789
    }] AS c;

--#[exclude-02]
SELECT * EXCLUDE c.ssn, c.address.street FROM [
    {
        'name': 'Alan',
        'custId': 1,
        'address': {
            'city': 'Seattle',
            'zipcode': 98109,
            'street': '123 Seaplane Dr.'
        },
        'ssn': 123456789
    }] AS c;

--#[exclude-03]
SELECT * EXCLUDE t.a.b.c[0], t.a.b.c[1].field
FROM [{
    'a': {
    'b': {
    'c': [
    {
    'field': 0    -- c[0]
    },
    {
    'field': 1    -- c[1]
    },
    {
    'field': 2    -- c[2]
    }
    ]
    }
    },
    'foo': 'bar'
    }] AS t;

--#[exclude-04]
SELECT *
           EXCLUDE
    t.a.b.c[0]
FROM [{
    'a': {
    'b': {
    'c': [0, 1, 2]
    }
    },
    'foo': 'bar'
    }] AS t;

--#[exclude-05]
SELECT *
           EXCLUDE
    t.a[*]
FROM [{
    'a': [0, 1, 2]
    }] AS t;

--#[exclude-06]
SELECT *
           EXCLUDE
    t.a.b.c[*].field_x
FROM [{
    'a': {
    'b': {
    'c': [
    {                    -- c[0]
    'field_x': 0,
    'field_y': 0
    },
    {                    -- c[1]
    'field_x': 1,
    'field_y': 1
    },
    {                    -- c[2]
    'field_x': 2,
    'field_y': 2
    }
    ]
    }
    },
    'foo': 'bar'
    }] AS t;

--#[exclude-07]
SELECT *
           EXCLUDE
    t.a.b.c[*].*
FROM [{
    'a': {
    'b': {
    'c': [
    {                    -- c[0]
    'field_x': 0,
    'field_y': 0
    },
    {                    -- c[1]
    'field_x': 1,
    'field_y': 1
    },
    {                    -- c[2]
    'field_x': 2,
    'field_y': 2
    }
    ]
    }
    },
    'foo': 'bar'
    }] AS t;

--#[exclude-08]
SELECT *
           EXCLUDE
    t.a
FROM [
    {
    'a': 2,
    'foo': 'bar2'
    },
    {
    'a': 1,
    'foo': 'bar1'
    },
    {
    'a': 3,
    'foo': 'bar3'
    }
    ] AS t
ORDER BY t.a;

--#[exclude-09]
SELECT *
           EXCLUDE bar.d
FROM
    <<
    {'a': 1, 'b': 11},
    {'a': 2, 'b': 22}
    >> AS foo,
    <<
    {'c': 3, 'd': 33},
    {'c': 4, 'd': 44}
    >> AS bar;

--#[exclude-10]
SELECT t.b EXCLUDE t.b[*].b_1
FROM <<
    {
    'a': {'a_1':1,'a_2':2},
    'b': [ {'b_1':3,'b_2':4}, {'b_1':5,'b_2':6} ],
    'c': 7,
    'd': 8
    } >> AS t;

--#[exclude-11]
SELECT * EXCLUDE t.b[*].b_1
FROM <<
    {
    'a': {'a_1':1,'a_2':2},
    'b': [ {'b_1':3,'b_2':4}, {'b_1':5,'b_2':6} ],
    'c': 7,
    'd': 8
    } >> AS t;

--#[exclude-12]
SELECT VALUE t.b EXCLUDE t.b[*].b_1
FROM <<
    {
    'a': {'a_1':1,'a_2':2},
    'b': [ {'b_1':3,'b_2':4}, {'b_1':5,'b_2':6} ],
    'c': 7,
    'd': 8
    } >> AS t;

--#[exclude-13]
SELECT * EXCLUDE t.a[*].b.c
FROM <<
    {
    'a': [
    { 'b': { 'c': 0, 'd': 'zero' } },
    { 'b': { 'c': 1, 'd': 'one' } },
    { 'b': { 'c': 2, 'd': 'two' } }
    ]
    }
    >> AS t;

--#[exclude-14]
SELECT * EXCLUDE t.a[1].b.c
FROM <<
    {
    'a': [
    { 'b': { 'c': 0, 'd': 'zero' } },
    { 'b': { 'c': 1, 'd': 'one' } },
    { 'b': { 'c': 2, 'd': 'two' } }
    ]
    }
    >> AS t;

--#[exclude-15]
SELECT * EXCLUDE t.a[*].b.*
FROM <<
    {
    'a': [
    { 'b': { 'c': 0, 'd': 'zero' } },
    { 'b': { 'c': 1, 'd': 'one' } },
    { 'b': { 'c': 2, 'd': 'two' } }
    ]
    }
    >> AS t;

--#[exclude-16]
SELECT * EXCLUDE t.a[1].b.*
FROM <<
    {
    'a': [
    { 'b': { 'c': 0, 'd': 'zero' } },
    { 'b': { 'c': 1, 'd': 'one' } },
    { 'b': { 'c': 2, 'd': 'two' } }
    ]
    }
    >> AS t;

--#[exclude-17]
SELECT * EXCLUDE t.a[*].b.d[*].e
FROM <<
    {
    'a': [
    { 'b': { 'c': 0, 'd': [{'e': 'zero', 'f': true}] } },
    { 'b': { 'c': 1, 'd': [{'e': 'one', 'f': true}] } },
    { 'b': { 'c': 2, 'd': [{'e': 'two', 'f': true}] } }
    ]
    }
    >> AS t;

--#[exclude-18]
SELECT * EXCLUDE t.a[1].b.d[*].e
FROM <<
    {
    'a': [
    { 'b': { 'c': 0, 'd': [{'e': 'zero', 'f': true}] } },
    { 'b': { 'c': 1, 'd': [{'e': 'one', 'f': true}] } },
    { 'b': { 'c': 2, 'd': [{'e': 'two', 'f': true}] } }
    ]
    }
    >> AS t;

--#[exclude-19]
SELECT * EXCLUDE t.a[1].b.d[0].e
FROM <<
    {
    'a': [
    { 'b': { 'c': 0, 'd': [{'e': 'zero', 'f': true}] } },
    { 'b': { 'c': 1, 'd': [{'e': 'one', 'f': true}] } },
    { 'b': { 'c': 2, 'd': [{'e': 'two', 'f': true}] } }
    ]
    }
    >> AS t;

--#[exclude-20]
SELECT * EXCLUDE t."a".b['c']
FROM <<
    {
    'a': {
    'B': {
    'c': 0,
    'd': 'foo'
    }
    }
    }
    >> AS t;

--#[exclude-21]
SELECT * EXCLUDE t."a".b['c']
FROM <<
    {
    'a': {
    'B': {
    'c': 0,
    'C': true,
    'd': 'foo'
    }
    }
    }
    >> AS t;

--#[exclude-22]
SELECT * EXCLUDE t."a".b.c
FROM <<
    {
    'a': {
    'B': {          -- both 'c' and 'C' to be removed
    'c': 0,
    'C': true,
    'd': 'foo'
    }
    }
    }
    >> AS t;

--#[exclude-23]
SELECT * EXCLUDE t."a".b.c
FROM <<
    {
    'a': {
    'B': {
    'c': 0,
    'c': true,
    'd': 'foo'
    }
    }
    }
    >> AS t;

--#[exclude-24]
SELECT * EXCLUDE t.a, t.a.b FROM << { 'a': { 'b': 1 }, 'c': 2 } >> AS t;

--#[exclude-25]
SELECT * EXCLUDE t.attr_does_not_exist FROM << { 'a': 1 } >> AS t;

--#[exclude-26]
SELECT t EXCLUDE t.a.b
FROM <<
    {
    'a': {
    'b': 1,    -- `b` to be excluded
    'c': 'foo'
    }
    },
    {
    'a': NULL
    }
    >> AS t;

--#[exclude-27]
SELECT t EXCLUDE t.a.b
FROM <<
    {
    'a': {
    'b': 1,    -- `b` to be excluded
    'c': 'foo'
    }
    },
    {
    'a': {
    'b': 1,    -- `b` to be excluded
    'c': NULL
    }
    }
    >> AS t;

--#[exclude-28]
SELECT t EXCLUDE t.a.c
FROM <<
    {
    'a': {
    'b': 1,
    'c': 'foo'  -- `c` to be excluded
    }
    },
    {
    'a': {
    'b': 1,
    'c': NULL   -- `c` to be excluded
    }
    }
    >> AS t;

--#[exclude-29]
SELECT * EXCLUDE t.a[*]
FROM <<
    {
    'a': {
    'b': {
    'c': 0,
    'd': 'foo'
    }
    }
    }
    >> AS t;

--#[exclude-30]
SELECT * EXCLUDE t.a[1]
FROM <<
    {
    'a': {
    'b': {
    'c': 0,
    'd': 'foo'
    }
    }
    }
    >> AS t;

--#[exclude-31]
SELECT * EXCLUDE t.a.b
FROM <<
    {
    'a': [
    { 'b': 0 },
    { 'b': 1 },
    { 'b': 2 }
    ]
    }
    >> AS t;

--#[exclude-32]
SELECT * EXCLUDE t.a.*
FROM <<
    {
    'a': [
    { 'b': 0 },
    { 'b': 1 },
    { 'b': 2 }
    ]
    }
    >> AS t;

--#[exclude-33]
SELECT * EXCLUDE t.b   -- `t.b` does not exist
FROM <<
    {
    'a': <<
    { 'b': 0 },
    { 'b': 1 },
    { 'b': 2 }
    >>
    }
    >> AS t;

--#[exclude-34]
-- EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
SELECT * EXCLUDE nonsense.b   -- `nonsense` does not exist in binding tuples
FROM <<
    { 'a': <<
            { 'b': 0 },
            { 'b': 1 },
            { 'b': 2 }
        >>
    }
>> AS t;

--#[exclude-35]
SELECT * EXCLUDE t.a[0].c    -- `c`'s type to be unioned with `MISSING`
FROM <<
    {
    'a': [
    {
    'b': 0,
    'c': 0
    },
    {
    'b': 1,
    'c': NULL
    },
    {
    'b': 2,
    'c': 0.1
    }
    ]
    }
    >> AS t;

--#[exclude-36]
SELECT * EXCLUDE t.c FROM b.b.b AS t;