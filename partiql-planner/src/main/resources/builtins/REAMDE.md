= Builtins

See https://github.com/orgs/partiql/discussions/64

== Syntax

```
{
  name: <symbol>,   // the routine name
  type: op|fn|agg,  // the routine type
  description: '''
     I am a multi-line string for generated documentation..
  ''',
  impls: [
    (fn name::type -> type), // start with op|fn|agg followed by name annotated arg types, then -> <return type>
     ....
  ]
}
```
