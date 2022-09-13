# Overview

PartiQL's list of keywords can be broken down into **reserved** and **non-reserved** keywords. PartiQL's keywords are subject to
move between reserved and non-reserved keywords at any time, and, therefore, we recommend wrapping the use of **all**
keywords with double quotes.

It is **recommended** that you wrap *all* keywords with double quotes to avoid parser issues -- now or in the future. As
an example, the keyword `ACYCLIC` is currently considered non-reserved, but it may become reserved in the future. Therefore,
while PartiQL currently allows

```sql
SELECT acyclic FROM a MATCH [ACYCLIC (b) -> (c)];
```

it is best to wrap the use of `ACYCLIC` when using it as an identifier (column name, table name, variable, etc).

```sql
SELECT "acyclic" FROM a MATCH [ACYCLIC (b) -> (c)];
```

# Reserved vs Non-Reserved Keywords

Reserved keywords are keywords that may **not** be used as identifiers (column names, table names, etc) unless
double-quotes are used as well.

Non-reserved keywords are keywords that you may use as identifiers without the use of double-quotes -- however, we
**recommend** the use of double-quotes, as some non-reserved keywords may eventually become reserved in the future.

# Reserved Keywords

Below is the list of currently reserved keywords. You can also find the most up-to-date list in the `partiql-lang-kotlin`
sub-project, `partiql-grammar`.

| ALL KEYWORDS      | RESERVED (Y/N) |
|-------------------|----------------|
| ABSOLUTE          | Y              |
| ACTION            | Y              |
| ACYCLIC           | N              |
| ADD               | Y              |
| ALL               | Y              |
| ALLOCATE          | Y              |
| ALTER             | Y              |
| AND               | Y              |
| ANY               | Y              |
| ARE               | Y              |
| AS                | Y              |
| ASC               | Y              |
| ASSERTION         | Y              |
| AT                | Y              |
| AUTHORIZATION     | Y              |
| AVG               | Y              |
| BAG               | Y              |
| BEGIN             | Y              |
| BETWEEN           | Y              |
| BIGINT            | Y              |
| BIT               | Y              |
| BIT_LENGTH        | Y              |
| BLOB              | Y              |
| BOOL              | Y              |
| BOOLEAN           | Y              |
| BOTH              | N              |
| BY                | Y              |
| CAN_CAST          | Y              |
| CAN_LOSSLESS_CAST | Y              |
| CASCADE           | Y              |
| CASCADED          | Y              |
| CASE              | Y              |
| CAST              | Y              |
| CATALOG           | Y              |
| CHAR              | Y              |
| CHARACTER         | Y              |
| CHARACTER_LENGTH  | Y              |
| CHAR_LENGTH       | Y              |
| CHECK             | Y              |
| CLOB              | Y              |
| CLOSE             | Y              |
| COALESCE          | Y              |
| COLLATE           | Y              |
| COLLATION         | Y              |
| COLUMN            | Y              |
| CONFLICT          | Y              |
| COMMIT            | Y              |
| CONNECT           | Y              |
| CONNECTION        | Y              |
| CONSTRAINT        | Y              |
| CONSTRAINTS       | Y              |
| CONTINUE          | Y              |
| CONVERT           | Y              |
| CORRESPONDING     | Y              |
| COUNT             | Y              |
| CREATE            | Y              |
| CROSS             | Y              |
| CURRENT           | Y              |
| CURRENT_DATE      | Y              |
| CURRENT_TIME      | Y              |
| CURRENT_TIMESTAMP | Y              |
| CURRENT_USER      | Y              |
| CURSOR            | Y              |
| DATE              | Y              |
| DEALLOCATE        | Y              |
| DEC               | Y              |
| DECIMAL           | Y              |
| DECLARE           | Y              |
| DEFAULT           | Y              |
| DEFERRABLE        | Y              |
| DEFERRED          | Y              |
| DELETE            | Y              |
| DESC              | Y              |
| DESCRIBE          | Y              |
| DESCRIPTOR        | Y              |
| DIAGNOSTICS       | Y              |
| DISCONNECT        | Y              |
| DISTINCT          | Y              |
| DO                | Y              |
| DOMAIN            | N              |
| DOUBLE            | Y              |
| DROP              | Y              |
| ELSE              | Y              |
| END               | Y              |
| END-EXEC          | Y              |
| ESCAPE            | Y              |
| EXCEPT            | Y              |
| EXCEPTION         | Y              |
| EXEC              | Y              |
| EXECUTE           | Y              |
| EXISTS            | Y              |
| EXTERNAL          | Y              |
| EXTRACT           | Y              |
| DATE_ADD          | Y              |
| DATE_DIFF         | Y              |
| FALSE             | Y              |
| FETCH             | Y              |
| FIRST             | Y              |
| FLOAT             | Y              |
| FOR               | Y              |
| FOREIGN           | Y              |
| FOUND             | Y              |
| FROM              | Y              |
| FULL              | Y              |
| GET               | Y              |
| GLOBAL            | Y              |
| GO                | Y              |
| GOTO              | Y              |
| GRANT             | Y              |
| GROUP             | Y              |
| HAVING            | Y              |
| IDENTITY          | Y              |
| IMMEDIATE         | Y              |
| IN                | Y              |
| INDEX             | Y              |
| INDICATOR         | Y              |
| INITIALLY         | Y              |
| INNER             | Y              |
| INPUT             | Y              |
| INSENSITIVE       | Y              |
| INSERT            | Y              |
| INT               | Y              |
| INT2              | Y              |
| INT4              | Y              |
| INT8              | Y              |
| INTEGER           | Y              |
| INTEGER2          | Y              |
| INTEGER4          | Y              |
| INTEGER8          | Y              |
| INTERSECT         | Y              |
| INTERVAL          | Y              |
| INTO              | Y              |
| IS                | Y              |
| ISOLATION         | Y              |
| JOIN              | Y              |
| KEY               | Y              |
| LANGUAGE          | Y              |
| LAST              | Y              |
| LATERAL           | Y              |
| LEADING           | N              |
| LEFT              | Y              |
| LET               | Y              |
| LEVEL             | Y              |
| LIKE              | Y              |
| LIMIT             | Y              |
| LIST              | Y              |
| LOCAL             | Y              |
| LOWER             | Y              |
| MATCH             | Y              |
| MAX               | Y              |
| MIN               | Y              |
| MISSING           | Y              |
| MODIFIED          | Y              |
| MODULE            | Y              |
| NAMES             | Y              |
| NATIONAL          | Y              |
| NATURAL           | Y              |
| NCHAR             | Y              |
| NEW               | Y              |
| NEXT              | Y              |
| NO                | Y              |
| NOT               | Y              |
| NOTHING           | Y              |
| NULL              | Y              |
| NULLS             | Y              |
| NULLIF            | Y              |
| NUMERIC           | Y              |
| OCTET_LENGTH      | Y              |
| OF                | Y              |
| OFFSET            | Y              |
| OLD               | Y              |
| ON                | Y              |
| ONLY              | Y              |
| OPEN              | Y              |
| OPTION            | Y              |
| OR                | Y              |
| ORDER             | Y              |
| OUTER             | Y              |
| OUTPUT            | Y              |
| OVERLAPS          | Y              |
| PAD               | Y              |
| PARTIAL           | Y              |
| POSITION          | Y              |
| PRECISION         | Y              |
| PREPARE           | Y              |
| PRESERVE          | Y              |
| PRIMARY           | Y              |
| PRIOR             | Y              |
| PRIVILEGES        | Y              |
| PROCEDURE         | Y              |
| PUBLIC            | N              |
| READ              | Y              |
| REAL              | Y              |
| REFERENCES        | Y              |
| RELATIVE          | Y              |
| RESTRICT          | Y              |
| REVOKE            | Y              |
| RIGHT             | Y              |
| ROLLBACK          | Y              |
| ROWS              | Y              |
| SCHEMA            | Y              |
| SCROLL            | Y              |
| SECTION           | Y              |
| SELECT            | Y              |
| SESSION           | Y              |
| SESSION_USER      | Y              |
| SET               | Y              |
| SHORTEST          | Y              |
| SIMPLE            | Y              |
| SIZE              | Y              |
| SMALLINT          | Y              |
| SOME              | Y              |
| SPACE             | Y              |
| SQL               | Y              |
| SQLCODE           | Y              |
| SQLERROR          | Y              |
| SQLSTATE          | Y              |
| STRING            | Y              |
| STRUCT            | Y              |
| SUBSTRING         | Y              |
| SUM               | Y              |
| SYMBOL            | Y              |
| SYSTEM_USER       | Y              |
| TABLE             | Y              |
| TEMPORARY         | Y              |
| THEN              | Y              |
| TIME              | Y              |
| TIMESTAMP         | Y              |
| TO                | Y              |
| TRANSACTION       | Y              |
| TRANSLATE         | Y              |
| TRANSLATION       | Y              |
| TRIM              | Y              |
| TRUE              | Y              |
| TUPLE             | Y              |
| UNION             | Y              |
| UNIQUE            | Y              |
| UNKNOWN           | Y              |
| UNPIVOT           | Y              |
| UPDATE            | Y              |
| UPPER             | Y              |
| USAGE             | Y              |
| USER              | Y              |
| USING             | Y              |
| PIVOT             | Y              |
| REMOVE            | Y              |
| RETURNING         | Y              |
| SEXP              | Y              |
| SIMPLE            | N              |
| TRAIL             | N              |
| TRAILING          | N              |
| USER              | N              |
| VALUE             | Y              |
| VALUES            | Y              |
| VARCHAR           | Y              |
| VARYING           | Y              |
| VIEW              | Y              |
| WHEN              | Y              |
| WHENEVER          | Y              |
| WHERE             | Y              |
| WITH              | Y              |
| WORK              | Y              |
| WRITE             | Y              |
| ZONE              | Y              |

# Other Examples

While the non-reserved keywords above do not **require** the use of double-quotes, it may be of use to use them for specific
scenarios. Take, for instance, the `TRIM` function. `TRIM`, as its first optional argument, takes keywords `LEADING`, `BOTH`,
or `TRAILING`.

```sql
SELECT leading FROM t WHERE TRIM(leading FROM t.someString) = 'otherString';
```

The above query removes the *leading* whitespace of `t.someString`, compares it with the literal `'otherString'`, and
selects the column `leading` from table `t`.

If, however, you wanted to trim whatever `leading` refers to as a column reference from both sides of `t.someString`, you 
would want to re-write your query as:

```sql
SELECT leading FROM t WHERE TRIM("leading" FROM t.someString) = 'otherString';
```

Or, if you wanted to be extremely explicit (**recommended**), you could write the query as:

```sql
SELECT "leading" FROM t WHERE TRIM(BOTH "leading" FROM t.someString) = 'otherString';
```
