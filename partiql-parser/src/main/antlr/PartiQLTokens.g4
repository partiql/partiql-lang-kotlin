/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

lexer grammar PartiQLTokens;

options {
    caseInsensitive = true;
}

/**
 *
 * KEYWORDS
 *
 */

ABSOLUTE: 'ABSOLUTE';
ACTION: 'ACTION';
ADD: 'ADD';
ALL: 'ALL';
ALLOCATE: 'ALLOCATE';
ALTER: 'ALTER';
AND: 'AND';
ANY: 'ANY';
ARE: 'ARE';
AS: 'AS';
ASC: 'ASC';
ASSERTION: 'ASSERTION';
AT: 'AT';
AUTHORIZATION: 'AUTHORIZATION';
AVG: 'AVG';
BEGIN: 'BEGIN';
BETWEEN: 'BETWEEN';
BIT: 'BIT';
BIT_LENGTH: 'BIT_LENGTH';
BY: 'BY';
CASCADE: 'CASCADE';
CASCADED: 'CASCADED';
CASE: 'CASE';
CAST: 'CAST';
CATALOG: 'CATALOG';
CHAR: 'CHAR';
CHARACTER: 'CHARACTER';
CHARACTER_LENGTH: 'CHARACTER_LENGTH';
CHAR_LENGTH: 'CHAR_LENGTH';
CHECK: 'CHECK';
CLOSE: 'CLOSE';
COALESCE: 'COALESCE';
COLLATE: 'COLLATE';
COLLATION: 'COLLATION';
COLUMN: 'COLUMN';
COMMIT: 'COMMIT';
CONNECT: 'CONNECT';
CONNECTION: 'CONNECTION';
CONSTRAINT: 'CONSTRAINT';
CONSTRAINTS: 'CONSTRAINTS';
CONTINUE: 'CONTINUE';
CONVERT: 'CONVERT';
CORRESPONDING: 'CORRESPONDING';
COUNT: 'COUNT';
CREATE: 'CREATE';
CROSS: 'CROSS';
CURRENT: 'CURRENT';
CURRENT_DATE: 'CURRENT_DATE';
CURRENT_TIME: 'CURRENT_TIME';
CURRENT_TIMESTAMP: 'CURRENT_TIMESTAMP';
CURRENT_USER: 'CURRENT_USER';
CURSOR: 'CURSOR';
DATE: 'DATE';
DEALLOCATE: 'DEALLOCATE';
DEC: 'DEC';
DECIMAL: 'DECIMAL';
DECLARE: 'DECLARE';
DEFAULT: 'DEFAULT';
DEFERRABLE: 'DEFERRABLE';
DEFERRED: 'DEFERRED';
DELETE: 'DELETE';
DESC: 'DESC';
DESCRIBE: 'DESCRIBE';
DESCRIPTOR: 'DESCRIPTOR';
DIAGNOSTICS: 'DIAGNOSTICS';
DISCONNECT: 'DISCONNECT';
DISTINCT: 'DISTINCT';
DOMAIN: 'DOMAIN';
DOUBLE: 'DOUBLE';
DROP: 'DROP';
ELSE: 'ELSE';
END: 'END';
END_EXEC: 'END-EXEC';
ESCAPE: 'ESCAPE';
EVERY: 'EVERY';
EXCEPT: 'EXCEPT';
EXCEPTION: 'EXCEPTION';
EXCLUDE: 'EXCLUDE';
EXCLUDED: 'EXCLUDED';
EXEC: 'EXEC';
EXECUTE: 'EXECUTE';
EXISTS: 'EXISTS';
EXPLAIN: 'EXPLAIN';
EXTERNAL: 'EXTERNAL';
EXTRACT: 'EXTRACT';
DATE_ADD: 'DATE_ADD';
DATE_DIFF: 'DATE_DIFF';
FALSE: 'FALSE';
FETCH: 'FETCH';
FIRST: 'FIRST';
FLOAT: 'FLOAT';
FOR: 'FOR';
FOREIGN: 'FOREIGN';
FOUND: 'FOUND';
FROM: 'FROM';
FULL: 'FULL';
GET: 'GET';
GLOBAL: 'GLOBAL';
GO: 'GO';
GOTO: 'GOTO';
GRANT: 'GRANT';
GROUP: 'GROUP';
HAVING: 'HAVING';
IDENTITY: 'IDENTITY';
IMMEDIATE: 'IMMEDIATE';
IN: 'IN';
INDICATOR: 'INDICATOR';
INITIALLY: 'INITIALLY';
INNER: 'INNER';
INPUT: 'INPUT';
INSENSITIVE: 'INSENSITIVE';
INSERT: 'INSERT';
INT: 'INT';
INTEGER: 'INTEGER';
INTERSECT: 'INTERSECT';
INTERVAL: 'INTERVAL';
INTO: 'INTO';
IS: 'IS';
ISOLATION: 'ISOLATION';
JOIN: 'JOIN';
KEY: 'KEY';
LANGUAGE: 'LANGUAGE';
LAST: 'LAST';
LATERAL: 'LATERAL';
LEFT: 'LEFT';
LEVEL: 'LEVEL';
LIKE: 'LIKE';
LOCAL: 'LOCAL';
LOWER: 'LOWER';
MATCH: 'MATCH';
MAX: 'MAX';
MIN: 'MIN';
MODULE: 'MODULE';
NAMES: 'NAMES';
NATIONAL: 'NATIONAL';
NATURAL: 'NATURAL';
NCHAR: 'NCHAR';
NEXT: 'NEXT';
NO: 'NO';
NOT: 'NOT';
NULL: 'NULL';
NULLS: 'NULLS';
NULLIF: 'NULLIF';
NUMERIC: 'NUMERIC';
OCTET_LENGTH: 'OCTET_LENGTH';
OF: 'OF';
ON: 'ON';
ONLY: 'ONLY';
OPEN: 'OPEN';
OPTION: 'OPTION';
OR: 'OR';
ORDER: 'ORDER';
OUTER: 'OUTER';
OUTPUT: 'OUTPUT';
OVERLAPS: 'OVERLAPS';
OVERLAY: 'OVERLAY';
PAD: 'PAD';
PARTIAL: 'PARTIAL';
PLACING: 'PLACING';
POSITION: 'POSITION';
PRECISION: 'PRECISION';
PREPARE: 'PREPARE';
PRESERVE: 'PRESERVE';
PRIMARY: 'PRIMARY';
PRIOR: 'PRIOR';
PRIVILEGES: 'PRIVILEGES';
PROCEDURE: 'PROCEDURE';
PUBLIC: 'PUBLIC';
READ: 'READ';
REAL: 'REAL';
REFERENCES: 'REFERENCES';
RELATIVE: 'RELATIVE';
REPLACE: 'REPLACE';
RESTRICT: 'RESTRICT';
REVOKE: 'REVOKE';
RIGHT: 'RIGHT';
ROLLBACK: 'ROLLBACK';
ROWS: 'ROWS';
SCHEMA: 'SCHEMA';
SCROLL: 'SCROLL';
SECTION: 'SECTION';
SELECT: 'SELECT';
SESSION: 'SESSION';
SESSION_USER: 'SESSION_USER';
SET: 'SET';
SHORTEST: 'SHORTEST';
SIZE: 'SIZE';
SMALLINT: 'SMALLINT';
SOME: 'SOME';
SPACE: 'SPACE';
SQL: 'SQL';
SQLCODE: 'SQLCODE';
SQLERROR: 'SQLERROR';
SQLSTATE: 'SQLSTATE';
SUBSTRING: 'SUBSTRING';
SUM: 'SUM';
SYSTEM_USER: 'SYSTEM_USER';
TABLE: 'TABLE';
TEMPORARY: 'TEMPORARY';
THEN: 'THEN';
TIME: 'TIME';
TIMESTAMP: 'TIMESTAMP';
TO: 'TO';
TRANSACTION: 'TRANSACTION';
TRANSLATE: 'TRANSLATE';
TRANSLATION: 'TRANSLATION';
TRIM: 'TRIM';
TRUE: 'TRUE';
UNION: 'UNION';
UNIQUE: 'UNIQUE';
UNKNOWN: 'UNKNOWN';
UPDATE: 'UPDATE';
UPPER: 'UPPER';
UPSERT: 'UPSERT';
USAGE: 'USAGE';
USER: 'USER';
USING: 'USING';
VALUE: 'VALUE';
VALUES: 'VALUES';
VARCHAR: 'VARCHAR';
VARYING: 'VARYING';
VIEW: 'VIEW';
WHEN: 'WHEN';
WHENEVER: 'WHENEVER';
WHERE: 'WHERE';
WITH: 'WITH';
WORK: 'WORK';
WRITE: 'WRITE';
ZONE: 'ZONE';


/**
 * window related
 */
 // TODO: Move the keywords to the corresponding section once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
 // i.e. Move OVER/PARTITION to KEYWORDS.
LAG: 'LAG';
LEAD: 'LEAD';
OVER: 'OVER';
PARTITION: 'PARTITION';


/**
 * OTHER
 */
CAN_CAST: 'CAN_CAST';
CAN_LOSSLESS_CAST: 'CAN_LOSSLESS_CAST';
MISSING: 'MISSING';
PIVOT: 'PIVOT';
UNPIVOT: 'UNPIVOT';
LIMIT: 'LIMIT';
OFFSET: 'OFFSET';
REMOVE: 'REMOVE';
INDEX: 'INDEX';
LET: 'LET';
CONFLICT: 'CONFLICT';
DO: 'DO';
RETURNING: 'RETURNING';
MODIFIED: 'MODIFIED';
NEW: 'NEW';
OLD: 'OLD';
NOTHING: 'NOTHING';

/**
 *
 * DATA TYPES
 *
 */

TUPLE: 'TUPLE';
INTEGER2: 'INTEGER2';
INT2: 'INT2';
INTEGER4: 'INTEGER4';
INT4: 'INT4';
INTEGER8: 'INTEGER8';
INT8: 'INT8';
BIGINT: 'BIGINT';
BOOL: 'BOOL';
BOOLEAN: 'BOOLEAN';
STRING: 'STRING';
SYMBOL: 'SYMBOL';
CLOB: 'CLOB';
BLOB: 'BLOB';
STRUCT: 'STRUCT';
LIST: 'LIST';
SEXP: 'SEXP';
BAG: 'BAG';

/**
 *
 * OPERATORS AND LITERALS
 *
 */

CARET: '^';
COMMA: ',';
PLUS: '+';
MINUS: '-';
SLASH_FORWARD: '/';
PERCENT: '%';
AT_SIGN: '@';
TILDE: '~';
ASTERISK: '*';
VERTBAR: '|';
AMPERSAND: '&';
BANG: '!';
LT_EQ: '<=';
GT_EQ: '>=';
EQ: '=';
NEQ: '<>' | '!=';
CONCAT: '||';
ANGLE_LEFT: '<';
ANGLE_RIGHT: '>';
ANGLE_DOUBLE_LEFT: '<<';
ANGLE_DOUBLE_RIGHT: '>>';
BRACKET_LEFT: '[';
BRACKET_RIGHT: ']';
BRACE_LEFT: '{';
BRACE_RIGHT: '}';
PAREN_LEFT: '(';
PAREN_RIGHT: ')';
BACKTICK: '`' -> more, pushMode(ION);
COLON: ':';
COLON_SEMI: ';';
QUESTION_MARK: '?';
PERIOD: '.';

/**
 *
 * LITERALS & IDENTIFIERS
 *
 */

LITERAL_STRING
    : '\'' ( ('\'\'') | ~('\'') )* '\'';

LITERAL_INTEGER
    : DIGIT DIGIT*;

LITERAL_DECIMAL:
    DIGIT+ '.' DIGIT* ([e] [+-]? DIGIT+)?
    | '.' DIGIT DIGIT* ([e] [+-]? DIGIT+)?
    | DIGIT DIGIT* ([e] [+-]? DIGIT+)?
    ;

IDENTIFIER
    : [A-Z$_][A-Z0-9$_]*;

IDENTIFIER_QUOTED
    : '"' ( ('""') | ~('"') )* '"';

/**
 *
 * TO IGNORE
 *
 */

WS
    : WHITESPACE+ -> channel(HIDDEN);

COMMENT_SINGLE
    : '--' ~[\r\n]* '\r'? '\n'? -> channel(HIDDEN);

COMMENT_BLOCK
    : '/*' .*? '*/' -> channel(HIDDEN);

UNRECOGNIZED
    : . ;

/**
 *
 * FRAGMENTS
 *
 */

fragment DIGIT
    : [0-9];

fragment LETTER
    : [A-Z];

fragment LETTER_NOT
    : ~[A-Z];

fragment WHITESPACE
    : [ \r\n\t];

/**
 *
 * ION MODE
 * Note: This is largely copied from Ion's public ANTLR grammar, but this is used in a very specific manner in PartiQL's
 *  grammar. We use a Lexer Grammar because it allows multiple modes (languages), and whenever we find a straggling
 *  backtick, we use pushMode() to enter Ion's grammar/mode. From there, since we don't necessarily care about the semantics of
 *  Ion, we need to capture *everything* until we see a standalone backtick. So, the only tokens we need to watch out for
 *  are tokens that *may* include a backtick in its contents (such as comments, strings, and quoted symbols). We use the
 *  `-> more` annotation to accumulate all of the received Ion tokens into a single PartiQL token (ION_CLOSURE). That
 *  indicates when we've received a standalone backtick and can pop out from Ion's grammar.
 *
 */

mode ION;

ION_INLINE_COMMENT
    : '//' .*? (ION_NEWLINE | EOF) -> more;

ION_BLOCK_COMMENT
    : '/*' .*? '*/' -> more;

ION_BLOB
    : LOB_START (BASE_64_QUARTET | WS)* BASE_64_PAD? WS* LOB_END -> more;

SHORT_QUOTED_STRING
    : SHORT_QUOTE STRING_SHORT_TEXT SHORT_QUOTE -> more
    ;

LONG_QUOTED_STRING
    : LONG_QUOTE STRING_LONG_TEXT LONG_QUOTE -> more
    ;

QUOTED_SYMBOL
    : SYMBOL_QUOTE SYMBOL_TEXT SYMBOL_QUOTE -> more;

ION_CLOSURE: '`' -> popMode;

ION_ANY: . -> more;

fragment ION_NEWLINE
    : '\u000D\u000A'
    | '\u000D'
    | '\u000A'
    ;

fragment SHORT_QUOTE
    : '"';

fragment LONG_QUOTE
    : '\'\'\'';

fragment STRING_SHORT_TEXT
    : (TEXT_ESCAPE | STRING_SHORT_TEXT_ALLOWED)*;

fragment STRING_LONG_TEXT
    : (TEXT_ESCAPE | STRING_LONG_TEXT_ALLOWED)*?;

// non-control Unicode and not double quote or backslash
fragment STRING_SHORT_TEXT_ALLOWED
    : '\u0020'..'\u0021' // no C1 control characters and no U+0022 double quote
    | '\u0023'..'\u005B' // no U+005C backslash
    | '\u005D'..'\uFFFF' // FIXME should be up to U+10FFFF
    | WS_NOT_NL
    ;

// non-control Unicode (newlines are OK)
fragment STRING_LONG_TEXT_ALLOWED
    : '\u0020'..'\u005B' // no C1 control characters and no U+005C blackslash
    | '\u005D'..'\uFFFF' // FIXME should be up to U+10FFFF
    | WS
    ;

fragment TEXT_ESCAPE
    : COMMON_ESCAPE | HEX_ESCAPE | UNICODE_ESCAPE;

fragment LOB_START
    : '{{';

fragment LOB_END
    : '}}';

fragment BASE_64_PAD
    : BASE_64_PAD1
    | BASE_64_PAD2
    ;

fragment BASE_64_QUARTET
    : BASE_64_CHAR WS* BASE_64_CHAR WS* BASE_64_CHAR WS* BASE_64_CHAR;

fragment BASE_64_PAD1
    : BASE_64_CHAR WS* BASE_64_CHAR WS* BASE_64_CHAR WS* '=';

fragment BASE_64_PAD2
    : BASE_64_CHAR WS* BASE_64_CHAR WS* '=' WS* '=';

fragment BASE_64_CHAR
    : [0-9A-Z+/];

fragment SYMBOL_TEXT
    : (TEXT_ESCAPE | SYMBOL_TEXT_ALLOWED)*;

fragment SYMBOL_TEXT_ALLOWED
    : '\u0020'..'\u0026' // no C1 control characters and no U+0027 single quote
    | '\u0028'..'\u005B' // no U+005C backslash
    | '\u005D'..'\uFFFF' // should be up to U+10FFFF
    | WS_NOT_NL
    ;

fragment COMMON_ESCAPE
    : '\\' COMMON_ESCAPE_CODE;

fragment COMMON_ESCAPE_CODE
    : 'a'
    | 'b'
    | 't'
    | 'n'
    | 'f'
    | 'r'
    | 'v'
    | '?'
    | '0'
    | '\''
    | '"'
    | '/'
    | '\\'
    | ION_NEWLINE
    ;

fragment HEX_ESCAPE
    : '\\x' HEX_DIGIT HEX_DIGIT;

fragment UNICODE_ESCAPE
    : '\\u'     HEX_DIGIT_QUARTET
    | '\\U000'  HEX_DIGIT_QUARTET HEX_DIGIT
    | '\\U0010' HEX_DIGIT_QUARTET
    ;

fragment HEX_DIGIT_QUARTET
    : HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;

fragment HEX_DIGIT
    : [0-9A-F];

fragment WS_NOT_NL
    : '\u0009' // tab
    | '\u000B' // vertical tab
    | '\u000C' // form feed
    | '\u0020' // space
    ;

fragment SYMBOL_QUOTE : '\'';
