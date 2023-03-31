# MockDB (LocalDB)

This is a mock DB that represents a lightweight database that is defined by JSON files. It is used for testing purposes.

## Configuration

In order to configure your root directory (Catalog), please create a Connector Configuration file as
`~/.partiql/plugins/<catalog_name>.ion`, where `<catalog_name>` will be the catalog name. See the below example:

```ion
// File: ~/.partiql/plugins/fs.ion
// Description: Stands for File System

{
  "connector_name": "localdb",  // This connector
  "localdb_root": "/Users"  // The (configurable) root of my filesystem to query against
}
```

## Catalog 

Your Catalog is specified as the `localdb_root` directory from your configuration file above. If not specified, it defaults to
`${HOME}/.partiql/localdb`.

Each Catalog holds Directories. Here's an example filesystem using the Configuration File from further above:
```text
fs (Connector: localdb) (Root: /Users)
├── john
│   ├── plants.json
│   └── pets.json
└── jack
    ├── living
    |  ├── furniture.json
    |  └── pets.json
    └── kitchen
       └── appliances.json
```

In the above PartiQL Environment, we have loaded all of the files from our filesystem starting at `/Users`. We can see
that there are two top-level directories `john` and `jack`. `john` does not have child directories, but `jack` does.
`john` directly holds Value Descriptors, while `jack`'s child directories hold Value Descriptors.

## Table Descriptors

Table schemas are stored as JSON files and have the following format:

```json
{
  "name": "plants",
  "type": "TABLE",
  "attributes": [
    {
      "name": "id",
      "type": "STRING",
      "attributes": []
    },
    {
      "name": "room_no",
      "type": "INT",
      "attributes": []
    },
    {
      "name": "water_frequency_days",
      "type": "DECIMAL",
      "attributes": [32, 0]
    },
    {
      "name": "metas",
      "type": "STRUCT",
      "attributes": [
        {
          "name": "a",
          "type": "INT",
          "attributes": []
        },
        {
          "name": "b",
          "type": "STRING",
          "attributes": []
        }
      ]
    }
  ]
}
```


## Inference Examples

### Using Current Catalog/Namespace

If we are referencing `plants`, and we've set our current namespace to `john` within the current catalog `fs`,
we can reference `plants`, similar to how we can reference files relative to our current working directory in Unix.

See `query.pql` below:
```partiql
--query.pql

SELECT
  id AS identifier,
  room_no AS room_number,
  water_frequency_days
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
----------------------------------------------
```

Similarly, if our current namespace is `jack` with the current catalog `fs` we can reference the table `appliances` using
a relative path:

```partiql
SELECT *
FROM kitchen.appliances
```

If we have set a current namespace and if the requested table doesn't match any position relative to the current namespace,
we will attempt to resolve it from the root of the catalog. If we can't find it there, we will attempt all other catalogs.

If you'd like to use an absolute reference to remove ambiguity and improve efficiency, please use the fully-qualified name:
```partiql
SELECT *
FROM fs.jack.kitchen.appliances
```


### More Complex Queries

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
