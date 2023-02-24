# LocalDB

This is a mock DB that represents a lightweight database that is defined by JSON files. It is
an extremely simple database.

## Catalog Schemas

Schemas are stored under the `localdb` directory in the `.partiql` directory of your `$HOME`: `${HOME}/.partiql/localdb/<schema>`

## Table Schemas

Table schemas are stored in `${HOME}/.partiql/localdb/<schema>/<table>.json` and have the following format:

```json
{
  "name": "plants",
  "attributes": [
    {
      "name": "id",
      "type": "STRING",
      "typeParams": []
    },
    {
      "name": "room_no",
      "type": "INT",
      "typeParams": []
    },
    {
      "name": "water_frequency_days",
      "type": "INT",
      "typeParams": []
    },
    {
      "name": "water_amount_liters",
      "type": "DOUBLE",
      "typeParams": ["32", "0"]
    }
  ]
}
```

## Catalog

In the following example, the `plants` table schema is placed in the `house` schema. To load the `house` schema, run:

```shell
partiql infer \
    --catalog ldb \
    --schema house \
    -m ldb=localdb \
    query.pql
```

## Inference Examples

See `query.pql` below:
```partiql
--query.pql

SELECT
  id AS identifier,
  room_no AS room_number,
  water_frequency_days,
  water_amount_liters
FROM plants
```

The output, using the command further above, is:
```text
----------------------------------------------
|  Schema Name: UNSPECIFIED_NAME             |
----------------------------------------------
|  identifier            |  string           |
|  room_number           |  int              |
|  water_frequency_days  |  int              |
|  water_amount_liters   |  decimal (32, 0)  |
----------------------------------------------
```

You can even create multiple tables and run more-complex queries:

```partiql
-- infer_join.pql

SELECT
  p1.id AS identifier,
  p1.room_no + 50 AS room_number,
  p1.water_frequency_days,
  p1.water_amount_liters,
  p2.name,
  p2.age,
  p2.weight,
  p2.favorite_toy
FROM
  plants AS p1
    CROSS JOIN
  pets AS p2
```

The above query outputs:

```text
----------------------------------------------
|  Schema Name: UNSPECIFIED_NAME             |
----------------------------------------------
|  identifier            |  string           |
|  room_number           |  int              |
|  water_frequency_days  |  int              |
|  water_amount_liters   |  decimal (32, 0)  |
|  name                  |  string           |
|  age                   |  int              |
|  weight                |  decimal          |
|  favorite_toy          |  string           |
----------------------------------------------
```

## Future Goals

At the moment, the PartiQLSchemaInferencer operates on the AST. In the future, the schema shall be inferred using the
logical plan.
