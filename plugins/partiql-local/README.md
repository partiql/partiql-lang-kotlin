# PartiQL Local DB Plugin

This is a PartiQL Plugin for a local database which represents its catalog with object descriptors in Ion files.
Its primary use is for testing.

## Configuration

In order to configure the catalog root directory, please create a Connector Configuration file as
`~/.partiql/plugins/<catalog_name>.ion`, where `<catalog_name>` will be the catalog name. See the below example:

```ion
// File: ~/.partiql/plugins/fs.ion
// Description: Stands for File System

{
  connector_name: "local",  // Associate the local connector to this catalog.
  root: "/Users"            // Specify this catalog's root directory where object descriptor files are stored.
}
```

## Catalog 

Your Catalog is specified as the `root` directory from your configuration file above. If not specified, it defaults to
`${HOME}/.partiql/local`.

Each Catalog holds Directories. Here's an example filesystem using the Configuration File from further above:
```text
fs (Connector: local) (Root: /Users)
├── john
│   ├── plants.ion
│   └── pets.ion
└── jack
    ├── living
    |  ├── furniture.ion
    |  └── pets.ion
    └── kitchen
       └── appliances.ion
```

In the above PartiQL Environment, we have loaded all of the files from our filesystem starting at `/Users`. We can see
that there are two top-level directories `john` and `jack`. `john` does not have child directories, but `jack` does.
`john` directly holds Value Descriptors, while `jack`'s child directories hold Value Descriptors.

## Object Descriptors

Each leaf in the catalog tree is an Ion file describing that object's value schema. Importantly, the file paths are
the qualified names of each object; ie the object `c` with path `a/b/c` corresponds to the qualified identifier `a.b.c`.

Object value schemas are stored as Ion files using PartiQL Value Schema notation. Here is an example of that format:

```ion
{
  type: "bag",
  items: {
    type: "struct",
    constraints: [
      closed,
      unique,
      odered
    ],
    fields: [
      {
        name: "id",
        type: "string"
      },
      {
        name: "room_no",
        type: "int"
      },
      {
        name: "water_frequency_days",
        type: "decimal"
      },
      {
        name: "metas",
        type: {
          type: "struct",
          fields: [
            {
              name: "a",
              type: "int"
            },
            {
              name: "b",
              type: "string"
            }
          ]
        }
      }
    ]
  }  
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
