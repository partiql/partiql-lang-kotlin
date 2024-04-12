### Conformance comparison report-Cross Engine
| | Base (legacy) | eval | +/- |
| --- | ---: | ---: | ---: |
| % Passing | 92.47% | 82.37% | -10.11% |
| :white_check_mark: Passing | 5380 | 4792 | -588 |
| :x: Failing | 438 | 1026 | 588 |
| :large_orange_diamond: Ignored | 0 | 0 | 0 |
| Total Tests | 5818 | 5818 | 0 |
Number passing in both: 4614

Number failing in both: 260

Number passing in legacy engine but fail in eval engine: 766

Number failing in legacy engine but pass in eval engine: 178
:interrobang: CONFORMANCE REPORT REGRESSION DETECTED :interrobang:. The following test(s) are passing in legacy but fail in eval:
<details><summary>Click here to see</summary>


- equiv wildcard steps struct, compileOption: PERMISSIVE
- equiv wildcard steps struct, compileOption: LEGACY
- equiv path expression with wildcard steps, compileOption: PERMISSIVE
- equiv path expression with wildcard steps, compileOption: LEGACY
- equiv path collection expression with wildcard steps, compileOption: PERMISSIVE
- equiv path collection expression with wildcard steps, compileOption: LEGACY
- equiv attribute value pair unpivot missing, compileOption: PERMISSIVE
- equiv left join, compileOption: PERMISSIVE
- equiv left join, compileOption: LEGACY
- Example 6 — Value Coercion, compileOption: PERMISSIVE
- Example 6 — Value Coercion, compileOption: LEGACY
- path on string, compileOption: PERMISSIVE
- tuple navigation missing attribute dot notation, compileOption: PERMISSIVE
- tuple navigation missing attribute array notation, compileOption: PERMISSIVE
- array navigation with wrongly typed array index, compileOption: PERMISSIVE
- single source FROM with scalar, compileOption: LEGACY
- single source FROM with tuple, compileOption: LEGACY
- single source FROM with absent value null, compileOption: LEGACY
- single source FROM with absent value missing, compileOption: LEGACY
- tuple constructor and mistyped attribute name, compileOption: PERMISSIVE
- attribute value evaluates to MISSING, compileOption: LEGACY
- array element evaluates to MISSING, compileOption: LEGACY
- bag element evaluates to MISSING, compileOption: LEGACY
- bag element evaluates to MISSING in bag constructor, compileOption: LEGACY
- pivot into a tuple with invalid attribute name, compileOption: LEGACY
- missing value in arithmetic expression, compileOption: PERMISSIVE
- data type mismatch in comparison expression, compileOption: PERMISSIVE
- data type mismatch in logical expression, compileOption: PERMISSIVE
- equality of scalar missing, compileOption: PERMISSIVE
- equality of same element bags, compileOption: PERMISSIVE
- equality of same element bags, compileOption: LEGACY
- WHERE clause eliminating absent values, compileOption: PERMISSIVE
- WHERE clause eliminating absent values, compileOption: LEGACY
- group by with absent values, compileOption: LEGACY
- group by with differenciated absent values, compileOption: LEGACY
- Right with variables, compileOption: PERMISSIVE
- Right with variables, compileOption: LEGACY
- Right with spots, compileOption: PERMISSIVE
- Right with spots, compileOption: LEGACY
- Right shorthand, compileOption: PERMISSIVE
- Right shorthand, compileOption: LEGACY
- Left with variables, compileOption: PERMISSIVE
- Left with variables, compileOption: LEGACY
- Left with spots, compileOption: PERMISSIVE
- Left with spots, compileOption: LEGACY
- Left shorthand, compileOption: PERMISSIVE
- Left shorthand, compileOption: LEGACY
- Left+right with variables, compileOption: PERMISSIVE
- Left+right with variables, compileOption: LEGACY
- Left+right with spots, compileOption: PERMISSIVE
- Left+right with spots, compileOption: LEGACY
- Left+right shorthand, compileOption: PERMISSIVE
- Left+right shorthand, compileOption: LEGACY
- Left+right with variables and label, compileOption: PERMISSIVE
- Left+right with variables and label, compileOption: LEGACY
- Undirected with variables, compileOption: PERMISSIVE
- Undirected with variables, compileOption: LEGACY
- Undirected with spots, compileOption: PERMISSIVE
- Undirected with spots, compileOption: LEGACY
- Undirected shorthand, compileOption: PERMISSIVE
- Undirected shorthand, compileOption: LEGACY
- Undirected with variables and label, compileOption: PERMISSIVE
- Undirected with variables and label, compileOption: LEGACY
- Right+undirected with variables, compileOption: PERMISSIVE
- Right+undirected with variables, compileOption: LEGACY
- Right+undirected with spots, compileOption: PERMISSIVE
- Right+undirected with spots, compileOption: LEGACY
- Right+undirected shorthand, compileOption: PERMISSIVE
- Right+undirected shorthand, compileOption: LEGACY
- Right+undirected with variables and labels, compileOption: PERMISSIVE
- Right+undirected with variables and labels, compileOption: LEGACY
- Left+undirected with variables, compileOption: PERMISSIVE
- Left+undirected with variables, compileOption: LEGACY
- Left+undirected with spots, compileOption: PERMISSIVE
- Left+undirected with spots, compileOption: LEGACY
- Left+undirected shorthand, compileOption: PERMISSIVE
- Left+undirected shorthand, compileOption: LEGACY
- Left+undirected with variables and label, compileOption: PERMISSIVE
- Left+undirected with variables and label, compileOption: LEGACY
- Left+right+undirected with variables, compileOption: PERMISSIVE
- Left+right+undirected with variables, compileOption: LEGACY
- Left+right+undirected with spots, compileOption: PERMISSIVE
- Left+right+undirected with spots, compileOption: LEGACY
- Left+right+undirected shorthand, compileOption: PERMISSIVE
- Left+right+undirected shorthand, compileOption: LEGACY
- (N0E0 MATCH (x)), compileOption: PERMISSIVE
- (N0E0 MATCH (x)), compileOption: LEGACY
- (N0E0 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N0E0 MATCH -[y]-> ), compileOption: LEGACY
- (N0E0 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N0E0 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N1E0 MATCH (x)), compileOption: PERMISSIVE
- (N1E0 MATCH (x)), compileOption: LEGACY
- (N1E0 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N1E0 MATCH -[y]-> ), compileOption: LEGACY
- (N1E0 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N1E0 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N1E0 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N1E0 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N1U1 MATCH (x)), compileOption: PERMISSIVE
- (N1U1 MATCH (x)), compileOption: LEGACY
- (N1U1 MATCH ~[y]~ ), compileOption: PERMISSIVE
- (N1U1 MATCH ~[y]~ ), compileOption: LEGACY
- (N1U1 MATCH (x)~[y]~(z) ), compileOption: PERMISSIVE
- (N1U1 MATCH (x)~[y]~(z) ), compileOption: LEGACY
- (N1U1 MATCH (x)~[y]~(x) ), compileOption: PERMISSIVE
- (N1U1 MATCH (x)~[y]~(x) ), compileOption: LEGACY
- (N1U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: PERMISSIVE
- (N1U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: LEGACY
- (N1D2 MATCH (x)), compileOption: PERMISSIVE
- (N1D2 MATCH (x)), compileOption: LEGACY
- (N1D2 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N1D2 MATCH -[y]-> ), compileOption: LEGACY
- (N1D2 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N1D2 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N1D2 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N1D2 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N1D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N1D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2E0 MATCH (x)), compileOption: PERMISSIVE
- (N2E0 MATCH (x)), compileOption: LEGACY
- (N2E0 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N2E0 MATCH -[y]-> ), compileOption: LEGACY
- (N2E0 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N2E0 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N2E0 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N2E0 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N2D1 MATCH (x)), compileOption: PERMISSIVE
- (N2D1 MATCH (x)), compileOption: LEGACY
- (N2D1 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N2D1 MATCH -[y]-> ), compileOption: LEGACY
- (N2D1 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N2D1 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N2D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D1 MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D1 MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2U1 MATCH (x)), compileOption: PERMISSIVE
- (N2U1 MATCH (x)), compileOption: LEGACY
- (N2U1 MATCH ~[y]~ ), compileOption: PERMISSIVE
- (N2U1 MATCH ~[y]~ ), compileOption: LEGACY
- (N2U1 MATCH (x)~[y]~(z) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x)~[y]~(z) ), compileOption: LEGACY
- (N2U1 MATCH (x)~[y]~(x) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x)~[y]~(x) ), compileOption: LEGACY
- (N2U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: LEGACY
- (N2U1 MATCH (x1)~[y1]~(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x1)~[y1]~(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2U1 MATCH (x1)-[y1]-(x2)~[y2]~(x3) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x1)-[y1]-(x2)~[y2]~(x3) ), compileOption: LEGACY
- (N2U1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D2 MATCH (x)), compileOption: PERMISSIVE
- (N2D2 MATCH (x)), compileOption: LEGACY
- (N2D2 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N2D2 MATCH -[y]-> ), compileOption: LEGACY
- (N2D2 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N2D2 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N2D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D2 MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D2 MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D2 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D2c MATCH (x)), compileOption: PERMISSIVE
- (N2D2c MATCH (x)), compileOption: LEGACY
- (N2D2c MATCH -[y]-> ), compileOption: PERMISSIVE
- (N2D2c MATCH -[y]-> ), compileOption: LEGACY
- (N2D2c MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N2D2c MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x1) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x1) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2U2 MATCH (x)), compileOption: PERMISSIVE
- (N2U2 MATCH (x)), compileOption: LEGACY
- (N2U2 MATCH ~[y]~ ), compileOption: PERMISSIVE
- (N2U2 MATCH ~[y]~ ), compileOption: LEGACY
- (N2U2 MATCH (x)~[y]~(z) ), compileOption: PERMISSIVE
- (N2U2 MATCH (x)~[y]~(z) ), compileOption: LEGACY
- (N2U2 MATCH (x)~[y]~(x) ), compileOption: PERMISSIVE
- (N2U2 MATCH (x)~[y]~(x) ), compileOption: LEGACY
- (N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: PERMISSIVE
- (N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: LEGACY
- (N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x1) ), compileOption: PERMISSIVE
- (N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x1) ), compileOption: LEGACY
- cast to MISSING valid cases{value:"NULL"}, compileOption: PERMISSIVE
- cast to MISSING valid cases{value:"NULL"}, compileOption: LEGACY
- cast to MISSING valid cases{value:"MISSING"}, compileOption: PERMISSIVE
- cast to MISSING valid cases{value:"MISSING"}, compileOption: LEGACY
- cast to NULL valid cases{value:"MISSING"}, compileOption: PERMISSIVE
- cast to NULL valid cases{value:"MISSING"}, compileOption: LEGACY
- cast to int invalid target type{value:"`2017T`",target:"TIMESTAMP"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:" `{{\"\"}}` ",target:"CLOB"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:" `{{\"1\"}}` ",target:"CLOB"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"`{{}}`",target:"BLOB"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"[1, 2]",target:"LIST"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"[1]",target:"LIST"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"[]",target:"LIST"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"`(1 2)`",target:"SEXP"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"`(1)`",target:"SEXP"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"`()`",target:"SEXP"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"{'a': 1}",target:"STRUCT"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"{'a': '12'}",target:"STRUCT"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"{}",target:"STRUCT"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"<<1, 2>>",target:"BAG"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"<<1>>",target:"BAG"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"<<>>",target:"BAG"}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null ESCAPE null "}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null ESCAPE null "}, compileOption: LEGACY
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null ESCAPE '['  "}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null ESCAPE '['  "}, compileOption: LEGACY
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE 'S1' ESCAPE null "}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE 'S1' ESCAPE null "}, compileOption: LEGACY
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null "}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null "}, compileOption: LEGACY
- MISSING LIKE 'some pattern', compileOption: PERMISSIVE
- 'some value' LIKE MISSING, compileOption: PERMISSIVE
- MISSING LIKE MISSING, compileOption: PERMISSIVE
- NULL LIKE MISSING, compileOption: PERMISSIVE
- MISSING LIKE NULL, compileOption: PERMISSIVE
- MISSING LIKE 'some pattern' ESCAPE '/', compileOption: PERMISSIVE
- 'some value' LIKE MISSING ESCAPE '/', compileOption: PERMISSIVE
- 'some value' LIKE 'some pattern' ESCAPE MISSING, compileOption: PERMISSIVE
- NULL LIKE 'some pattern' ESCAPE MISSING, compileOption: PERMISSIVE
- 'some value' LIKE NULL ESCAPE MISSING, compileOption: PERMISSIVE
- outerUnionCoerceScalar, compileOption: PERMISSIVE
- outerUnionCoerceScalar, compileOption: LEGACY
- outerUnionCoerceStruct, compileOption: PERMISSIVE
- outerUnionCoerceStruct, compileOption: LEGACY
- outerUnionCoerceNullMissing, compileOption: PERMISSIVE
- outerUnionCoerceNullMissing, compileOption: LEGACY
- inPredicate, compileOption: PERMISSIVE
- inPredicate, compileOption: LEGACY
- inPredicateSingleItem, compileOption: PERMISSIVE
- inPredicateSingleItem, compileOption: LEGACY
- inPredicateSingleExpr, compileOption: PERMISSIVE
- inPredicateSingleExpr, compileOption: LEGACY
- inPredicateSingleItemListVar, compileOption: PERMISSIVE
- inPredicateSingleItemListVar, compileOption: LEGACY
- inPredicateSingleListVar, compileOption: PERMISSIVE
- inPredicateSingleListVar, compileOption: LEGACY
- inPredicateSubQuerySelectValue, compileOption: PERMISSIVE
- inPredicateSubQuerySelectValue, compileOption: LEGACY
- notInPredicate, compileOption: PERMISSIVE
- notInPredicate, compileOption: LEGACY
- notInPredicateSingleItem, compileOption: PERMISSIVE
- notInPredicateSingleItem, compileOption: LEGACY
- notInPredicateSingleExpr, compileOption: PERMISSIVE
- notInPredicateSingleItemListVar, compileOption: PERMISSIVE
- notInPredicateSingleItemListVar, compileOption: LEGACY
- notInPredicateSingleListVar, compileOption: PERMISSIVE
- notInPredicateSingleListVar, compileOption: LEGACY
- notInPredicateSubQuerySelectValue, compileOption: PERMISSIVE
- notInPredicateSubQuerySelectValue, compileOption: LEGACY
- inPredicateWithTableConstructor, compileOption: PERMISSIVE
- inPredicateWithTableConstructor, compileOption: LEGACY
- notInPredicateWithTableConstructor, compileOption: PERMISSIVE
- notInPredicateWithTableConstructor, compileOption: LEGACY
- inPredicateWithExpressionOnRightSide, compileOption: PERMISSIVE
- inPredicateWithExpressionOnRightSide, compileOption: LEGACY
- notInPredicateWithExpressionOnRightSide, compileOption: PERMISSIVE
- notInPredicateWithExpressionOnRightSide, compileOption: LEGACY
- || valid cases{lparam:"null",rparam:"missing",result:missing::null}, compileOption: PERMISSIVE
- || valid cases{lparam:"missing",rparam:"null",result:missing::null}, compileOption: PERMISSIVE
- || valid cases{lparam:"missing",rparam:"'b'",result:missing::null}, compileOption: PERMISSIVE
- || valid cases{lparam:"'a'",rparam:"missing",result:missing::null}, compileOption: PERMISSIVE
- || valid cases{lparam:"missing",rparam:"missing",result:missing::null}, compileOption: PERMISSIVE
- repeatingDecimal, compileOption: PERMISSIVE
- repeatingDecimal, compileOption: LEGACY
- repeatingDecimalHigherPrecision, compileOption: PERMISSIVE
- repeatingDecimalHigherPrecision, compileOption: LEGACY
- divDecimalInt, compileOption: PERMISSIVE
- divDecimalInt, compileOption: LEGACY
- subtractionOutOfAllowedPrecision, compileOption: PERMISSIVE
- subtractionOutOfAllowedPrecision, compileOption: LEGACY
- equalListDifferentTypesTrue, compileOption: PERMISSIVE
- equalListDifferentTypesTrue, compileOption: LEGACY
- simpleCase, compileOption: PERMISSIVE
- simpleCase, compileOption: LEGACY
- simpleCaseNoElse, compileOption: PERMISSIVE
- simpleCaseNoElse, compileOption: LEGACY
- searchedCase, compileOption: PERMISSIVE
- searchedCase, compileOption: LEGACY
- searchedCaseNoElse, compileOption: PERMISSIVE
- searchedCaseNoElse, compileOption: LEGACY
- dateTimePartsAsVariableNames, compileOption: LEGACY
- pathDotMissingAttribute, compileOption: LEGACY
- pathMissingDotName, compileOption: PERMISSIVE
- pathMissingDotName, compileOption: LEGACY
- pathNullDotName, compileOption: PERMISSIVE
- pathNullDotName, compileOption: LEGACY
- pathIndexBagLiteral, compileOption: PERMISSIVE
- pathIndexBagLiteral, compileOption: LEGACY
- pathIndexStructLiteral, compileOption: PERMISSIVE
- pathIndexStructLiteral, compileOption: LEGACY
- pathIndexStructOutOfBoundsLowLiteral, compileOption: PERMISSIVE
- pathIndexStructOutOfBoundsLowLiteral, compileOption: LEGACY
- pathIndexStructOutOfBoundsHighLiteral, compileOption: PERMISSIVE
- pathIndexStructOutOfBoundsHighLiteral, compileOption: LEGACY
- pathDoubleWildCard, compileOption: PERMISSIVE
- pathDoubleWildCard, compileOption: LEGACY
- pathWildCardOverScalar, compileOption: LEGACY
- pathUnpivotWildCardOverScalar, compileOption: LEGACY
- pathWildCardOverScalarMultiple, compileOption: LEGACY
- pathUnpivotWildCardOverScalarMultiple, compileOption: LEGACY
- pathWildCardOverStructMultiple, compileOption: LEGACY
- unpivotMissing, compileOption: PERMISSIVE
- unpivotMissing, compileOption: LEGACY
- unpivotEmptyStruct, compileOption: PERMISSIVE
- unpivotEmptyStruct, compileOption: LEGACY
- unpivotStructWithMissingField, compileOption: PERMISSIVE
- unpivotStructWithMissingField, compileOption: LEGACY
- unpivotMissingWithAsAndAt, compileOption: LEGACY
- unpivotMissingCrossJoinWithAsAndAt, compileOption: LEGACY
- pathUnpivotEmptyStruct1, compileOption: PERMISSIVE
- pathUnpivotEmptyStruct1, compileOption: LEGACY
- pathUnpivotEmptyStruct2, compileOption: PERMISSIVE
- pathUnpivotEmptyStruct2, compileOption: LEGACY
- pathUnpivotEmptyStruct3, compileOption: PERMISSIVE
- pathUnpivotEmptyStruct3, compileOption: LEGACY
- dotted path expression with quoted field name accesses field UNAMBIGUOUS_FIELD (uppercase), compileOption: LEGACY
- subscript with variable in lowercase, compileOption: PERMISSIVE
- subscript with variable in lowercase, compileOption: LEGACY
- subscript with variable in uppercase, compileOption: PERMISSIVE
- subscript with variable in uppercase, compileOption: LEGACY
- subscript with variable in mixed case, compileOption: PERMISSIVE
- subscript with variable in mixed case, compileOption: LEGACY
- subscript with non-existent variable in lowercase, compileOption: PERMISSIVE
- subscript with non-existent variable in uppercase, compileOption: PERMISSIVE
- null comparison{sql:"MISSING IS NULL",result:true}, compileOption: PERMISSIVE
- null comparison{sql:"MISSING IS NULL",result:true}, compileOption: LEGACY
- null comparison{sql:"MISSING = NULL",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"NULL = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.sexp` = NULL",result:null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.sexp` = NULL",result:null}, compileOption: LEGACY
- null comparison{sql:"`null.null` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.bool` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.int` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.decimal` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.string` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.symbol` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.clob` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.blob` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.list` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.struct` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.sexp` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- concatenation with null values{left:"MISSING",right:"MISSING"}, compileOption: PERMISSIVE
- concatenation with null values{left:"''",right:"MISSING"}, compileOption: PERMISSIVE
- concatenation with null values{left:"MISSING",right:"''"}, compileOption: PERMISSIVE
- concatenation with null values{left:"'a'",right:"MISSING"}, compileOption: PERMISSIVE
- concatenation with null values{left:"MISSING",right:"'b'"}, compileOption: PERMISSIVE
- char_length null and missing propagation{in:"missing",result:(success missing::null)}, compileOption: PERMISSIVE
- character_length null and missing propagation{in:"missing",result:(success missing::null)}, compileOption: PERMISSIVE
- CHARACTER_LENGTH invalid type, compileOption: PERMISSIVE
- upper null and missing propagation{param:"missing"}, compileOption: PERMISSIVE
- cardinality null and missing propagation{param:"missing"}, compileOption: PERMISSIVE
- CARDINALITY('foo') type mismatch, compileOption: PERMISSIVE
- EXTRACT(YEAR FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(MONTH FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(DAY FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(HOUR FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(MINUTE FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(SECOND FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(TIMEZONE_HOUR FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(TIMEZONE_MINUTE FROM MISSING), compileOption: PERMISSIVE
- invalid extract year from time, compileOption: PERMISSIVE
- invalid extract month from time, compileOption: PERMISSIVE
- invalid extract day from time, compileOption: PERMISSIVE
- invalid extract month from time with time zone, compileOption: PERMISSIVE
- invalid extract day from time with time zone, compileOption: PERMISSIVE
- POSITION MISSING in string, compileOption: PERMISSIVE
- POSITION string in MISSING, compileOption: PERMISSIVE
- POSITION NULL in MISSING, compileOption: PERMISSIVE
- POSITION MISSING in NULL, compileOption: PERMISSIVE
- POSITION MISSING in MISSING, compileOption: PERMISSIVE
- POSITION invalid type in string, compileOption: PERMISSIVE
- POSITION string in invalid type, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"missing",start_pos:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"''",start_pos:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"missing",start_pos:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"null",start_pos:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"missing",start_pos:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"1",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"null",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"1",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"null",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"missing"}, compileOption: PERMISSIVE
- lower null and missing propagation{param:"missing"}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"1",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"1",result:null}, compileOption: LEGACY
- nullif valid cases{first:"1.0",second:"1",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"1.0",second:"1",result:null}, compileOption: LEGACY
- nullif valid cases{first:"1",second:"2",result:1}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"2",result:1}, compileOption: LEGACY
- nullif valid cases{first:"2",second:"'2'",result:2}, compileOption: PERMISSIVE
- nullif valid cases{first:"2",second:"'2'",result:2}, compileOption: LEGACY
- nullif valid cases{first:"{}",second:"{}",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"{}",second:"{}",result:null}, compileOption: LEGACY
- nullif valid cases{first:"[]",second:"[]",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"[]",second:"[]",result:null}, compileOption: LEGACY
- nullif valid cases{first:"{}",second:"[]",result:{}}, compileOption: PERMISSIVE
- nullif valid cases{first:"{}",second:"[]",result:{}}, compileOption: LEGACY
- nullif valid cases{first:"1",second:"null",result:1}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"null",result:1}, compileOption: LEGACY
- nullif valid cases{first:"null",second:"1",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"null",second:"1",result:null}, compileOption: LEGACY
- nullif valid cases{first:"null",second:"null",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"null",second:"null",result:null}, compileOption: LEGACY
- nullif valid cases{first:"1",second:"missing",result:1}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"missing",result:1}, compileOption: LEGACY
- nullif valid cases{first:"missing",second:"1",result:missing::null}, compileOption: PERMISSIVE
- nullif valid cases{first:"missing",second:"1",result:missing::null}, compileOption: LEGACY
- ABS(MISSING) null propogation, compileOption: PERMISSIVE
- ABS('foo'), compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading '' from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing '' from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both '' from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading missing from '')"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing missing from '')"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both missing from '')"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading null from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing null from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both null from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading missing from null)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing missing from null)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both missing from null)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading missing from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing missing from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both missing from missing)"}, compileOption: PERMISSIVE
- MOD(MISSING, 3), compileOption: PERMISSIVE
- MOD(3, MISSING), compileOption: PERMISSIVE
- MOD(MISSING, NULL), compileOption: PERMISSIVE
- MOD(NULL, MISSING), compileOption: PERMISSIVE
- MOD(MISSING, 'some string'), compileOption: PERMISSIVE
- MOD('some string', MISSING), compileOption: PERMISSIVE
- MOD(3, 'some string'), compileOption: PERMISSIVE
- MOD('some string', 3), compileOption: PERMISSIVE
- BIT_LENGTH MISSING, compileOption: PERMISSIVE
- BIT_LENGTH invalid type, compileOption: PERMISSIVE
- OCTET_LENGTH MISSING, compileOption: PERMISSIVE
- OCTET_LENGTH invalid type, compileOption: PERMISSIVE
- OVERLAY MISSING, compileOption: PERMISSIVE
- OVERLAY PLACING MISSING, compileOption: PERMISSIVE
- OVERLAY FROM MISSING, compileOption: PERMISSIVE
- OVERLAY FOR MISSING, compileOption: PERMISSIVE
- OVERLAY mismatched type, compileOption: PERMISSIVE
- OVERLAY PLACING mismatched type, compileOption: PERMISSIVE
- OVERLAY FROM mismatched type, compileOption: PERMISSIVE
- OVERLAY FOR mismatched type, compileOption: PERMISSIVE
- coalesce valid cases{args:"1",result:(success 1)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"1",result:(success 1)}, compileOption: LEGACY
- coalesce valid cases{args:"1, 2",result:(success 1)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"1, 2",result:(success 1)}, compileOption: LEGACY
- coalesce valid cases{args:"null, 2",result:(success 2)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, 2",result:(success 2)}, compileOption: LEGACY
- coalesce valid cases{args:"missing, 3",result:(success 3)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"missing, 3",result:(success 3)}, compileOption: LEGACY
- coalesce valid cases{args:"null, null, 3",result:(success 3)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, null, 3",result:(success 3)}, compileOption: LEGACY
- coalesce valid cases{args:"null, missing, 3",result:(success 3)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, missing, 3",result:(success 3)}, compileOption: LEGACY
- coalesce valid cases{args:"null, missing, null, null, missing, 9, 4, 5, 6",result:(success 9)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, missing, null, null, missing, 9, 4, 5, 6",result:(success 9)}, compileOption: LEGACY
- Empty Symbol in table, compileOption: LEGACY
- Empty Symbol in globals, compileOption: LEGACY
- Empty Symbol in alias, compileOption: LEGACY
- functionCall, compileOption: PERMISSIVE
- functionCall, compileOption: LEGACY
- division with mixed StaticType, compileOption: PERMISSIVE
- division with mixed StaticType, compileOption: LEGACY
- Example 2.3 — Union of Compatible Relations; Mismatch Column Names; Using OUTER UNION, compileOption: PERMISSIVE
- Example 2.3 — Union of Compatible Relations; Mismatch Column Names; Using OUTER UNION, compileOption: LEGACY
- Example 3 — Outer union of Heterogenous Relations, compileOption: PERMISSIVE
- Example 3 — Outer union of Heterogenous Relations, compileOption: LEGACY
- Example 6 — Value Coercion; Coercion of single value, compileOption: PERMISSIVE
- Example 6 — Value Coercion; Coercion of single value, compileOption: LEGACY
- Example 7 — `SELECT * FROM engineering.employees OUTER EXCEPT << >>`, compileOption: PERMISSIVE
- Example 7 — `SELECT * FROM engineering.employees OUTER EXCEPT << >>`, compileOption: LEGACY
- Example 7 — `engineering.employees OUTER UNION << MISSING >>`, compileOption: PERMISSIVE
- Example 7 — `engineering.employees OUTER UNION << MISSING >>`, compileOption: LEGACY
- Example 7 — result is the empty bag, compileOption: PERMISSIVE
- Example 7 — result is the empty bag, compileOption: LEGACY
- undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing, compileOption: PERMISSIVE
- undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing, compileOption: PERMISSIVE
- undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing, compileOption: PERMISSIVE
- undefinedUnqualifiedVariableInSelectWithUndefinedVariableBehaviorMissing, compileOption: LEGACY
- join on column - all column values non-null, compileOption: PERMISSIVE
- join on column - all column values non-null, compileOption: LEGACY
- join on column - some column values are null, compileOption: PERMISSIVE
- join on column - some column values are null, compileOption: LEGACY
- join on column - 1 table contains 1 row with the value null, compileOption: PERMISSIVE
- join on column - 1 table contains 1 row with the value null, compileOption: LEGACY
- join on column - ON condition = false, compileOption: PERMISSIVE
- join on column - ON condition = false, compileOption: LEGACY
- PG_JOIN_01, compileOption: PERMISSIVE
- PG_JOIN_01, compileOption: LEGACY
- PG_JOIN_02, compileOption: PERMISSIVE
- PG_JOIN_02, compileOption: LEGACY
- PG_JOIN_03, compileOption: PERMISSIVE
- PG_JOIN_03, compileOption: LEGACY
- PG_JOIN_06, compileOption: PERMISSIVE
- PG_JOIN_06, compileOption: LEGACY
- PG_JOIN_07, compileOption: PERMISSIVE
- PG_JOIN_07, compileOption: LEGACY
- PG_JOIN_08, compileOption: PERMISSIVE
- PG_JOIN_08, compileOption: LEGACY
- PG_JOIN_09, compileOption: PERMISSIVE
- PG_JOIN_09, compileOption: LEGACY
- PG_JOIN_10, compileOption: PERMISSIVE
- PG_JOIN_10, compileOption: LEGACY
- offset 0, compileOption: PERMISSIVE
- offset 0, compileOption: LEGACY
- offset 1, compileOption: PERMISSIVE
- offset 1, compileOption: LEGACY
- offset 2, compileOption: PERMISSIVE
- offset 2, compileOption: LEGACY
- limit 1 offset 1, compileOption: PERMISSIVE
- limit 1 offset 1, compileOption: LEGACY
- limit 10 offset 1, compileOption: PERMISSIVE
- limit 10 offset 1, compileOption: LEGACY
- limit 2 offset 2, compileOption: PERMISSIVE
- limit 2 offset 2, compileOption: LEGACY
- limit offset after group by, compileOption: PERMISSIVE
- limit offset after group by, compileOption: LEGACY
- offset 2-1, compileOption: PERMISSIVE
- offset 2-1, compileOption: LEGACY
- offset 2+1, compileOption: PERMISSIVE
- offset 2+1, compileOption: LEGACY
- offset 2*1, compileOption: PERMISSIVE
- offset 2*1, compileOption: LEGACY
- offset 2/1, compileOption: PERMISSIVE
- offset 2/1, compileOption: LEGACY
- offset group by having, compileOption: PERMISSIVE
- offset group by having, compileOption: LEGACY
- offset with pivot, compileOption: PERMISSIVE
- offset with pivot, compileOption: LEGACY
- pivotBadFieldType, compileOption: LEGACY
- col1 asc, compileOption: PERMISSIVE
- col1 asc, compileOption: LEGACY
- col1 desc, compileOption: PERMISSIVE
- col1 desc, compileOption: LEGACY
- col1 asc, col2 asc, compileOption: PERMISSIVE
- col1 asc, col2 asc, compileOption: LEGACY
- price desc, productId asc, compileOption: PERMISSIVE
- price desc, productId asc, compileOption: LEGACY
- supplierId_nulls nulls last, compileOption: PERMISSIVE
- supplierId_nulls nulls last, compileOption: LEGACY
- supplierId_nulls nulls first, compileOption: PERMISSIVE
- supplierId_nulls nulls first, compileOption: LEGACY
- supplierId_nulls asc nulls last, productId asc, compileOption: PERMISSIVE
- supplierId_nulls asc nulls last, productId asc, compileOption: LEGACY
- nulls first as default for supplierId_nulls desc, compileOption: PERMISSIVE
- nulls first as default for supplierId_nulls desc, compileOption: LEGACY
- group and order by asc sellerId, compileOption: PERMISSIVE
- group and order by asc sellerId, compileOption: LEGACY
- group and order by desc sellerId, compileOption: PERMISSIVE
- group and order by desc sellerId, compileOption: LEGACY
- group and order by DESC (NULLS FIRST as default), compileOption: PERMISSIVE
- group and order by DESC (NULLS FIRST as default), compileOption: LEGACY
- group and order by ASC (NULLS LAST as default), compileOption: PERMISSIVE
- group and order by ASC (NULLS LAST as default), compileOption: LEGACY
- group and place nulls first (asc as default), compileOption: PERMISSIVE
- group and place nulls first (asc as default), compileOption: LEGACY
- group and place nulls last (asc as default), compileOption: PERMISSIVE
- group and place nulls last (asc as default), compileOption: LEGACY
- group and order by asc and place nulls first, compileOption: PERMISSIVE
- group and order by asc and place nulls first, compileOption: LEGACY
- false before true (ASC), compileOption: PERMISSIVE
- false before true (ASC), compileOption: LEGACY
- true before false (DESC), compileOption: PERMISSIVE
- true before false (DESC), compileOption: LEGACY
- nan before -inf, then numeric values then +inf (ASC), compileOption: PERMISSIVE
- nan before -inf, then numeric values then +inf (ASC), compileOption: LEGACY
- +inf before numeric values then -inf then nan (DESC), compileOption: PERMISSIVE
- +inf before numeric values then -inf then nan (DESC), compileOption: LEGACY
- LOB types follow their lexicographical ordering by octet (ASC), compileOption: PERMISSIVE
- LOB types follow their lexicographical ordering by octet (ASC), compileOption: LEGACY
- LOB types should ordered (DESC), compileOption: PERMISSIVE
- LOB types should ordered (DESC), compileOption: LEGACY
- shorter array comes first (ASC), compileOption: PERMISSIVE
- shorter array comes first (ASC), compileOption: LEGACY
- longer array comes first (DESC), compileOption: PERMISSIVE
- longer array comes first (DESC), compileOption: LEGACY
- lists compared lexicographically based on comparison of elements (ASC), compileOption: PERMISSIVE
- lists compared lexicographically based on comparison of elements (ASC), compileOption: LEGACY
- lists compared lexicographically based on comparison of elements (DESC), compileOption: PERMISSIVE
- lists compared lexicographically based on comparison of elements (DESC), compileOption: LEGACY
- lists items should be ordered by data types (ASC) (nulls last as default for asc), compileOption: PERMISSIVE
- lists items should be ordered by data types (ASC) (nulls last as default for asc), compileOption: LEGACY
- lists items should be ordered by data types (DESC) (nulls first as default for desc), compileOption: PERMISSIVE
- lists items should be ordered by data types (DESC) (nulls first as default for desc), compileOption: LEGACY
- structs compared lexicographically first by key then by value (ASC), compileOption: PERMISSIVE
- structs compared lexicographically first by key then by value (ASC), compileOption: LEGACY
- structs compared lexicographically first by key then by value (DESC), compileOption: PERMISSIVE
- structs compared lexicographically first by key then by value (DESC), compileOption: LEGACY
- structs should be ordered by data types (ASC) (nulls last as default for asc), compileOption: PERMISSIVE
- structs should be ordered by data types (ASC) (nulls last as default for asc), compileOption: LEGACY
- structs should be ordered by data types (DESC) (nulls first as default for desc), compileOption: PERMISSIVE
- structs should be ordered by data types (DESC) (nulls first as default for desc), compileOption: LEGACY
- bags compared as sorted lists (ASC), compileOption: PERMISSIVE
- bags compared as sorted lists (ASC), compileOption: LEGACY
- bags compared as sorted lists (DESC), compileOption: PERMISSIVE
- bags compared as sorted lists (DESC), compileOption: LEGACY
- testing alias support, compileOption: PERMISSIVE
- testing alias support, compileOption: LEGACY
- testing nested alias support, compileOption: PERMISSIVE
- testing nested alias support, compileOption: LEGACY
- Empty Output (ordered), compileOption: PERMISSIVE
- Empty Output (ordered), compileOption: LEGACY
- GROUP BY binding referenced in FROM clause, compileOption: PERMISSIVE
- GROUP BY binding referenced in WHERE clause, compileOption: PERMISSIVE
- GROUP AS binding referenced in FROM clause, compileOption: PERMISSIVE
- GROUP AS binding referenced in WHERE clause, compileOption: PERMISSIVE
- SELECT COUNT(1) AS the_count, COUNT(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, SUM(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, MIN(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, MAX(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, AVG(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, COUNT(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, SUM(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, MIN(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, MAX(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, AVG(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, COUNT(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, SUM(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, MIN(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, MAX(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, AVG(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, COUNT(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, SUM(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, MIN(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, MAX(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, AVG(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, COUNT(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, SUM(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, MIN(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, MAX(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, AVG(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, COUNT(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, SUM(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, MIN(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, MAX(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, AVG(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- Expression with multiple subqueriees containing aggregates : CAST((SELECT COUNT(1) FROM products) AS LIST)[0]._1 / CAST((SELECT COUNT(1) FROM suppliers) AS LIST)[0]._1, compileOption: PERMISSIVE
- Expression with multiple subqueriees containing aggregates : CAST((SELECT COUNT(1) FROM products) AS LIST)[0]._1 / CAST((SELECT COUNT(1) FROM suppliers) AS LIST)[0]._1, compileOption: LEGACY
- Aggregates with subquery containing another aggregate : SELECT COUNT(1) + CAST((SELECT SUM(numInStock) FROM products) AS LIST)[0]._1 as a_number FROM products, compileOption: PERMISSIVE
- Aggregates with subquery containing another aggregate : SELECT COUNT(1) + CAST((SELECT SUM(numInStock) FROM products) AS LIST)[0]._1 as a_number FROM products, compileOption: LEGACY
- GROUP BY with JOIN : SELECT supplierName, COUNT(*) as the_count FROM suppliers AS s INNER JOIN products AS p ON s.supplierId = p.supplierId GROUP BY supplierName, compileOption: PERMISSIVE
- GROUP BY with JOIN : SELECT supplierName, COUNT(*) as the_count FROM suppliers AS s INNER JOIN products AS p ON s.supplierId = p.supplierId GROUP BY supplierName, compileOption: LEGACY
- SELECT VALUE with nested aggregates : SELECT VALUE (SELECT SUM(outerFromSource.col1) AS the_sum FROM <<1>>) FROM simple_1_col_1_group as outerFromSource, compileOption: PERMISSIVE
- SELECT VALUE with nested aggregates : SELECT VALUE (SELECT SUM(outerFromSource.col1) AS the_sum FROM <<1>>) FROM simple_1_col_1_group as outerFromSource, compileOption: LEGACY
- SELECT col1, g FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g, compileOption: PERMISSIVE
- SELECT col1, g FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g, compileOption: LEGACY
- SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g, compileOption: PERMISSIVE
- SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g, compileOption: LEGACY
- SELECT col1, g FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g, compileOption: PERMISSIVE
- SELECT col1, g FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g, compileOption: LEGACY
- SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g, compileOption: PERMISSIVE
- SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g, compileOption: LEGACY
- MYSQL_SELECT_20, compileOption: PERMISSIVE
- MYSQL_SELECT_20, compileOption: LEGACY
- MYSQL_SELECT_21, compileOption: PERMISSIVE
- MYSQL_SELECT_21, compileOption: LEGACY
- MYSQL_SELECT_26, compileOption: PERMISSIVE
- MYSQL_SELECT_26, compileOption: LEGACY
- selectFromScalarAndAtUnpivotWildCardOverScalar, compileOption: PERMISSIVE
- selectFromScalarAndAtUnpivotWildCardOverScalar, compileOption: LEGACY
- selectFromListAndAtUnpivotWildCardOverScalar, compileOption: PERMISSIVE
- selectFromListAndAtUnpivotWildCardOverScalar, compileOption: LEGACY
- selectFromBagAndAtUnpivotWildCardOverScalar, compileOption: PERMISSIVE
- selectFromBagAndAtUnpivotWildCardOverScalar, compileOption: LEGACY
- selectPathUnpivotWildCardOverStructMultiple, compileOption: PERMISSIVE
- selectPathUnpivotWildCardOverStructMultiple, compileOption: LEGACY
- selectStarSingleSourceHoisted, compileOption: PERMISSIVE
- selectStarSingleSourceHoisted, compileOption: LEGACY
- ordinalAccessWithNegativeIndex, compileOption: LEGACY
- ordinalAccessWithNegativeIndexAndBindings, compileOption: LEGACY
- rangeOverScalar, compileOption: LEGACY
- rangeTwiceOverScalar, compileOption: LEGACY
- rangeOverSexp, compileOption: PERMISSIVE
- rangeOverSexp, compileOption: LEGACY
- rangeOverStruct, compileOption: LEGACY
- rangeOverBagWithAt, compileOption: LEGACY
- rangeOverNestedWithAt, compileOption: LEGACY
- avg group by{agg:'AVG(t.b)',expectedF1:1.25,expectedF2:3.}, compileOption: PERMISSIVE
- avg group by{agg:'AVG(t.b)',expectedF1:1.25,expectedF2:3.}, compileOption: LEGACY
- avg group by{agg:'AVG(ALL t.b)',expectedF1:1.25,expectedF2:3.}, compileOption: PERMISSIVE
- avg group by{agg:'AVG(ALL t.b)',expectedF1:1.25,expectedF2:3.}, compileOption: LEGACY
- avg group by{agg:'AVG(DISTINCT t.b)',expectedF1:1.5,expectedF2:3.}, compileOption: PERMISSIVE
- avg group by{agg:'AVG(DISTINCT t.b)',expectedF1:1.5,expectedF2:3.}, compileOption: LEGACY
- ANY with GROUP BY, compileOption: LEGACY
- ANY DISTINCT with GROUP BY, compileOption: LEGACY
- SOME with GROUP BY, compileOption: LEGACY
- SOME DISTINCT with GROUP BY, compileOption: LEGACY
- EVERY with GROUP BY, compileOption: LEGACY
- EVERY DISTINCT with GROUP BY, compileOption: LEGACY
- selectListMultipleAggregatesNestedQuery, compileOption: PERMISSIVE
- selectListMultipleAggregatesNestedQuery, compileOption: LEGACY
- undefinedUnqualifiedVariable_inSelect_withProjectionOption, compileOption: LEGACY
- projectionIterationBehaviorUnfiltered_select_star, compileOption: PERMISSIVE
- projectionIterationBehaviorUnfiltered_select_star, compileOption: LEGACY
- projectOfSexp, compileOption: PERMISSIVE
- projectOfSexp, compileOption: LEGACY
- projectOfUnpivotPath, compileOption: LEGACY
- alias1.alias2.*, compileOption: LEGACY
- selectImplicitAndExplicitAliasSingleSourceHoisted, compileOption: PERMISSIVE
- selectImplicitAndExplicitAliasSingleSourceHoisted, compileOption: LEGACY
- selectListWithMissing, compileOption: LEGACY
- selectCorrelatedJoin, compileOption: PERMISSIVE
- selectCorrelatedJoin, compileOption: LEGACY
- selectCorrelatedLeftJoin, compileOption: PERMISSIVE
- selectCorrelatedLeftJoin, compileOption: LEGACY
- selectCorrelatedLeftJoinOnClause, compileOption: PERMISSIVE
- selectCorrelatedLeftJoinOnClause, compileOption: LEGACY
- selectJoinOnClauseScoping, compileOption: PERMISSIVE
- selectJoinOnClauseScoping, compileOption: LEGACY
- selectNonCorrelatedJoin, compileOption: LEGACY
- correlatedJoinWithShadowedAttributes, compileOption: LEGACY
- correlatedJoinWithoutLexicalScope, compileOption: LEGACY
- joinWithShadowedGlobal, compileOption: LEGACY
- selectDistinctStarBags, compileOption: PERMISSIVE
- selectDistinctStarBags, compileOption: LEGACY
- variableShadow, compileOption: LEGACY
- selectValueStructConstructorWithMissing, compileOption: LEGACY
- selectIndexStruct, compileOption: PERMISSIVE
- selectIndexStruct, compileOption: LEGACY
- emptySymbol, compileOption: LEGACY
- emptySymbolInGlobals, compileOption: LEGACY
</details>
The following test(s) are failing in legacy but pass in eval. Before merging, confirm they are intended to pass: 
<details><summary>Click here to see</summary>


- equiv group by with aggregates, compileOption: PERMISSIVE

- equiv group by with aggregates, compileOption: LEGACY

- missing and true, compileOption: PERMISSIVE

- coll_count with result of subquery, compileOption: PERMISSIVE

- coll_count with result of subquery, compileOption: LEGACY

- outerUnionAll, compileOption: PERMISSIVE

- outerUnionAll, compileOption: LEGACY

- outerExceptDistinct, compileOption: PERMISSIVE

- outerExceptDistinct, compileOption: LEGACY

- outerUnionCoerceList, compileOption: PERMISSIVE

- outerUnionCoerceList, compileOption: LEGACY

- max top level{agg:'COLL_MAX(data)',result:(success 2)}, compileOption: PERMISSIVE

- max top level{agg:'COLL_MAX(data)',result:(success 2)}, compileOption: LEGACY

- topLevelCollMax, compileOption: PERMISSIVE

- topLevelCollMax, compileOption: LEGACY

- COLL_MAX empty collection, compileOption: PERMISSIVE

- COLL_MAX empty collection, compileOption: LEGACY

- COLL_MAX null, compileOption: PERMISSIVE

- COLL_MAX null, compileOption: LEGACY

- COLL_MAX list of missing element, compileOption: PERMISSIVE

- COLL_MAX list of missing element, compileOption: LEGACY

- COLL_MAX bag of missing elements, compileOption: PERMISSIVE

- COLL_MAX bag of missing elements, compileOption: LEGACY

- COLL_MAX bag of heterogeneous element types, compileOption: PERMISSIVE

- COLL_MAX bag of heterogeneous element types, compileOption: LEGACY

- coll_avg top level{agg:'COLL_AVG(data)',result:(success 1.25)}, compileOption: PERMISSIVE

- coll_avg top level{agg:'COLL_AVG(data)',result:(success 1.25)}, compileOption: LEGACY

- topLevelCollAvg, compileOption: PERMISSIVE

- topLevelCollAvg, compileOption: LEGACY

- topLevelCollAvgOnlyInt, compileOption: PERMISSIVE

- topLevelCollAvgOnlyInt, compileOption: LEGACY

- COLL_AVG empty collection, compileOption: PERMISSIVE

- COLL_AVG empty collection, compileOption: LEGACY

- COLL_AVG null, compileOption: PERMISSIVE

- COLL_AVG null, compileOption: LEGACY

- COLL_AVG list of missing element, compileOption: PERMISSIVE

- COLL_AVG list of missing element, compileOption: LEGACY

- COLL_AVG bag of missing elements, compileOption: PERMISSIVE

- COLL_AVG bag of missing elements, compileOption: LEGACY

- COLL_AVG mistyped element, compileOption: PERMISSIVE

- coll_count top level{agg:'COLL_COUNT(data)',result:(success 4)}, compileOption: PERMISSIVE

- coll_count top level{agg:'COLL_COUNT(data)',result:(success 4)}, compileOption: LEGACY

- topLevelCollCount, compileOption: PERMISSIVE

- topLevelCollCount, compileOption: LEGACY

- COLL_COUNT empty collection, compileOption: PERMISSIVE

- COLL_COUNT empty collection, compileOption: LEGACY

- COLL_COUNT null, compileOption: PERMISSIVE

- COLL_COUNT null, compileOption: LEGACY

- COLL_COUNT list of missing element, compileOption: PERMISSIVE

- COLL_COUNT list of missing element, compileOption: LEGACY

- COLL_COUNT bag of missing elements, compileOption: PERMISSIVE

- COLL_COUNT bag of missing elements, compileOption: LEGACY

- COLL_COUNT bag of heterogeneous element types, compileOption: PERMISSIVE

- COLL_COUNT bag of heterogeneous element types, compileOption: LEGACY

- coll_sum top level{agg:'COLL_SUM(data)',result:(success 5)}, compileOption: PERMISSIVE

- coll_sum top level{agg:'COLL_SUM(data)',result:(success 5)}, compileOption: LEGACY

- topLevelCollSum, compileOption: PERMISSIVE

- topLevelCollSum, compileOption: LEGACY

- COLL_SUM empty collection, compileOption: PERMISSIVE

- COLL_SUM empty collection, compileOption: LEGACY

- COLL_SUM null, compileOption: PERMISSIVE

- COLL_SUM null, compileOption: LEGACY

- COLL_SUM list of missing element, compileOption: PERMISSIVE

- COLL_SUM list of missing element, compileOption: LEGACY

- COLL_SUM bag of missing elements, compileOption: PERMISSIVE

- COLL_SUM bag of missing elements, compileOption: LEGACY

- COLL_SUM mistyped element, compileOption: PERMISSIVE

- coll_min top level{agg:'COLL_MIN(data)',result:(success 1)}, compileOption: PERMISSIVE

- coll_min top level{agg:'COLL_MIN(data)',result:(success 1)}, compileOption: LEGACY

- topLevelCollMin, compileOption: PERMISSIVE

- topLevelCollMin, compileOption: LEGACY

- COLL_MIN empty collection, compileOption: PERMISSIVE

- COLL_MIN empty collection, compileOption: LEGACY

- COLL_MIN null, compileOption: PERMISSIVE

- COLL_MIN null, compileOption: LEGACY

- COLL_MIN list of missing element, compileOption: PERMISSIVE

- COLL_MIN list of missing element, compileOption: LEGACY

- COLL_MIN bag of missing elements, compileOption: PERMISSIVE

- COLL_MIN bag of missing elements, compileOption: LEGACY

- COLL_MIN bag of heterogeneous element types, compileOption: PERMISSIVE

- COLL_MIN bag of heterogeneous element types, compileOption: LEGACY

- COLL_ANY bag literals, compileOption: PERMISSIVE

- COLL_ANY bag literals, compileOption: LEGACY

- COLL_ANY list expressions, compileOption: PERMISSIVE

- COLL_ANY list expressions, compileOption: LEGACY

- COLL_ANY single true, compileOption: PERMISSIVE

- COLL_ANY single true, compileOption: LEGACY

- COLL_ANY single false, compileOption: PERMISSIVE

- COLL_ANY single false, compileOption: LEGACY

- COLL_ANY nulls with true, compileOption: PERMISSIVE

- COLL_ANY nulls with true, compileOption: LEGACY

- COLL_ANY nulls with false, compileOption: PERMISSIVE

- COLL_ANY nulls with false, compileOption: LEGACY

- COLL_ANY nulls only, compileOption: PERMISSIVE

- COLL_ANY nulls only, compileOption: LEGACY

- COLL_ANY null, compileOption: PERMISSIVE

- COLL_ANY null, compileOption: LEGACY

- COLL_ANY list of missing element, compileOption: PERMISSIVE

- COLL_ANY list of missing element, compileOption: LEGACY

- COLL_ANY bag of missing elements, compileOption: PERMISSIVE

- COLL_ANY bag of missing elements, compileOption: LEGACY

- COLL_ANY some empty, compileOption: PERMISSIVE

- COLL_ANY some empty, compileOption: LEGACY

- COLL_ANY one non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_ANY all non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_ANY nested collection, compileOption: PERMISSIVE

- COLL_SOME bag literals, compileOption: PERMISSIVE

- COLL_SOME bag literals, compileOption: LEGACY

- COLL_SOME list expressions, compileOption: PERMISSIVE

- COLL_SOME list expressions, compileOption: LEGACY

- COLL_SOME single true, compileOption: PERMISSIVE

- COLL_SOME single true, compileOption: LEGACY

- COLL_SOME single false, compileOption: PERMISSIVE

- COLL_SOME single false, compileOption: LEGACY

- COLL_SOME nulls with true, compileOption: PERMISSIVE

- COLL_SOME nulls with true, compileOption: LEGACY

- COLL_SOME nulls with false, compileOption: PERMISSIVE

- COLL_SOME nulls with false, compileOption: LEGACY

- COLL_SOME nulls only, compileOption: PERMISSIVE

- COLL_SOME nulls only, compileOption: LEGACY

- COLL_SOME null, compileOption: PERMISSIVE

- COLL_SOME null, compileOption: LEGACY

- COLL_SOME list of missing element, compileOption: PERMISSIVE

- COLL_SOME list of missing element, compileOption: LEGACY

- COLL_SOME bag of missing elements, compileOption: PERMISSIVE

- COLL_SOME bag of missing elements, compileOption: LEGACY

- COLL_SOME some empty, compileOption: PERMISSIVE

- COLL_SOME some empty, compileOption: LEGACY

- COLL_SOME one non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_SOME all non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_SOME nested collection, compileOption: PERMISSIVE

- COLL_EVERY bag literals, compileOption: PERMISSIVE

- COLL_EVERY bag literals, compileOption: LEGACY

- COLL_EVERY list expressions, compileOption: PERMISSIVE

- COLL_EVERY list expressions, compileOption: LEGACY

- COLL_EVERY single true, compileOption: PERMISSIVE

- COLL_EVERY single true, compileOption: LEGACY

- COLL_EVERY single false, compileOption: PERMISSIVE

- COLL_EVERY single false, compileOption: LEGACY

- COLL_EVERY null and missing with true, compileOption: PERMISSIVE

- COLL_EVERY null and missing with true, compileOption: LEGACY

- COLL_EVERY null with false, compileOption: PERMISSIVE

- COLL_EVERY null with false, compileOption: LEGACY

- COLL_EVERY null and missing only, compileOption: PERMISSIVE

- COLL_EVERY null and missing only, compileOption: LEGACY

- COLL_EVERY null, compileOption: PERMISSIVE

- COLL_EVERY null, compileOption: LEGACY

- COLL_EVERY list of missing element, compileOption: PERMISSIVE

- COLL_EVERY list of missing element, compileOption: LEGACY

- COLL_EVERY bag of missing elements, compileOption: PERMISSIVE

- COLL_EVERY bag of missing elements, compileOption: LEGACY

- COLL_EVERY empty collection, compileOption: PERMISSIVE

- COLL_EVERY empty collection, compileOption: LEGACY

- COLL_EVERY one non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_EVERY all non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_EVERY nested collection, compileOption: PERMISSIVE

- selectValueCollAggregate, compileOption: PERMISSIVE

- selectValueCollAggregate, compileOption: LEGACY

- EXTRACT(SECOND FROM `2000-01-02T03:04:05.67Z`), compileOption: PERMISSIVE

- EXTRACT(SECOND FROM `2000-01-02T03:04:05.67Z`), compileOption: LEGACY

- EXTRACT(SECOND FROM `2000-01-02T03:04:05.67+08:09`), compileOption: PERMISSIVE

- EXTRACT(SECOND FROM `2000-01-02T03:04:05.67+08:09`), compileOption: LEGACY

- offset 2^63, compileOption: PERMISSIVE

- offset 2^63, compileOption: LEGACY

- SELECT supplierId_missings FROM products_sparse p GROUP BY p.supplierId_missings, compileOption: PERMISSIVE

- SELECT p.supplierId_missings FROM products_sparse p GROUP BY p.supplierId_missings, compileOption: PERMISSIVE

- SELECT VALUE { 'supplierId_missings' : p.supplierId_missings } FROM products_sparse p GROUP BY p.supplierId_missings, compileOption: PERMISSIVE

- SELECT supplierId_mixed FROM products_sparse p GROUP BY p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT p.supplierId_mixed FROM products_sparse p GROUP BY p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT VALUE { 'supplierId_mixed' : p.supplierId_mixed } FROM products_sparse p GROUP BY p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT regionId, supplierId_missings FROM products_sparse p GROUP BY p.regionId, p.supplierId_missings, compileOption: PERMISSIVE

- SELECT p.regionId, p.supplierId_missings FROM products_sparse p GROUP BY p.regionId, p.supplierId_missings, compileOption: PERMISSIVE

- SELECT VALUE { 'regionId': p.regionId, 'supplierId_missings': p.supplierId_missings } FROM products_sparse p GROUP BY p.regionId, p.supplierId_missings, compileOption: PERMISSIVE

- SELECT regionId, supplierId_mixed FROM products_sparse p GROUP BY p.regionId, p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT regionId, p.supplierId_mixed FROM products_sparse p GROUP BY p.regionId, p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT VALUE { 'regionId': p.regionId, 'supplierId_mixed': p.supplierId_mixed } FROM products_sparse p GROUP BY p.regionId, p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT with nested aggregates (complex) 2, compileOption: PERMISSIVE

- SELECT with nested aggregates (complex) 2, compileOption: LEGACY

</details>

### Conformance comparison report-Cross Commit-LEGACY
| | Base (HEAD) | HEAD | +/- |
| --- | ---: | ---: | ---: |
| % Passing | 92.47% | 92.47% | 0.00% |
| :white_check_mark: Passing | 5380 | 5380 | 0 |
| :x: Failing | 438 | 438 | 0 |
| :large_orange_diamond: Ignored | 0 | 0 | 0 |
| Total Tests | 5818 | 5818 | 0 |
Number passing in both: 5380

Number failing in both: 438

Number passing in Base (HEAD) but now fail: 0

Number failing in Base (HEAD) but now pass: 0

### Conformance comparison report-Cross Commit-EVAL
| | Base (HEAD) | HEAD | +/- |
| --- | ---: | ---: | ---: |
| % Passing | 82.37% | 82.37% | 0.00% |
| :white_check_mark: Passing | 4792 | 4792 | 0 |
| :x: Failing | 1026 | 1026 | 0 |
| :large_orange_diamond: Ignored | 0 | 0 | 0 |
| Total Tests | 5818 | 5818 | 0 |
Number passing in both: 4792

Number failing in both: 1026

Number passing in Base (HEAD) but now fail: 0

Number failing in Base (HEAD) but now pass: 0

### Conformance comparison report-Cross Engine
| | Base (legacy) | eval | +/- |
| --- | ---: | ---: | ---: |
| % Passing | 92.47% | 82.37% | -10.11% |
| :white_check_mark: Passing | 5380 | 4792 | -588 |
| :x: Failing | 438 | 1026 | 588 |
| :large_orange_diamond: Ignored | 0 | 0 | 0 |
| Total Tests | 5818 | 5818 | 0 |
Number passing in both: 4614

Number failing in both: 260

Number passing in legacy engine but fail in eval engine: 766

Number failing in legacy engine but pass in eval engine: 178
:interrobang: CONFORMANCE REPORT REGRESSION DETECTED :interrobang:. The following test(s) are passing in legacy but fail in eval:
<details><summary>Click here to see</summary>


- equiv wildcard steps struct, compileOption: PERMISSIVE
- equiv wildcard steps struct, compileOption: LEGACY
- equiv path expression with wildcard steps, compileOption: PERMISSIVE
- equiv path expression with wildcard steps, compileOption: LEGACY
- equiv path collection expression with wildcard steps, compileOption: PERMISSIVE
- equiv path collection expression with wildcard steps, compileOption: LEGACY
- equiv attribute value pair unpivot missing, compileOption: PERMISSIVE
- equiv left join, compileOption: PERMISSIVE
- equiv left join, compileOption: LEGACY
- Example 6 — Value Coercion, compileOption: PERMISSIVE
- Example 6 — Value Coercion, compileOption: LEGACY
- path on string, compileOption: PERMISSIVE
- tuple navigation missing attribute dot notation, compileOption: PERMISSIVE
- tuple navigation missing attribute array notation, compileOption: PERMISSIVE
- array navigation with wrongly typed array index, compileOption: PERMISSIVE
- single source FROM with scalar, compileOption: LEGACY
- single source FROM with tuple, compileOption: LEGACY
- single source FROM with absent value null, compileOption: LEGACY
- single source FROM with absent value missing, compileOption: LEGACY
- tuple constructor and mistyped attribute name, compileOption: PERMISSIVE
- attribute value evaluates to MISSING, compileOption: LEGACY
- array element evaluates to MISSING, compileOption: LEGACY
- bag element evaluates to MISSING, compileOption: LEGACY
- bag element evaluates to MISSING in bag constructor, compileOption: LEGACY
- pivot into a tuple with invalid attribute name, compileOption: LEGACY
- missing value in arithmetic expression, compileOption: PERMISSIVE
- data type mismatch in comparison expression, compileOption: PERMISSIVE
- data type mismatch in logical expression, compileOption: PERMISSIVE
- equality of scalar missing, compileOption: PERMISSIVE
- equality of same element bags, compileOption: PERMISSIVE
- equality of same element bags, compileOption: LEGACY
- WHERE clause eliminating absent values, compileOption: PERMISSIVE
- WHERE clause eliminating absent values, compileOption: LEGACY
- group by with absent values, compileOption: LEGACY
- group by with differenciated absent values, compileOption: LEGACY
- Right with variables, compileOption: PERMISSIVE
- Right with variables, compileOption: LEGACY
- Right with spots, compileOption: PERMISSIVE
- Right with spots, compileOption: LEGACY
- Right shorthand, compileOption: PERMISSIVE
- Right shorthand, compileOption: LEGACY
- Left with variables, compileOption: PERMISSIVE
- Left with variables, compileOption: LEGACY
- Left with spots, compileOption: PERMISSIVE
- Left with spots, compileOption: LEGACY
- Left shorthand, compileOption: PERMISSIVE
- Left shorthand, compileOption: LEGACY
- Left+right with variables, compileOption: PERMISSIVE
- Left+right with variables, compileOption: LEGACY
- Left+right with spots, compileOption: PERMISSIVE
- Left+right with spots, compileOption: LEGACY
- Left+right shorthand, compileOption: PERMISSIVE
- Left+right shorthand, compileOption: LEGACY
- Left+right with variables and label, compileOption: PERMISSIVE
- Left+right with variables and label, compileOption: LEGACY
- Undirected with variables, compileOption: PERMISSIVE
- Undirected with variables, compileOption: LEGACY
- Undirected with spots, compileOption: PERMISSIVE
- Undirected with spots, compileOption: LEGACY
- Undirected shorthand, compileOption: PERMISSIVE
- Undirected shorthand, compileOption: LEGACY
- Undirected with variables and label, compileOption: PERMISSIVE
- Undirected with variables and label, compileOption: LEGACY
- Right+undirected with variables, compileOption: PERMISSIVE
- Right+undirected with variables, compileOption: LEGACY
- Right+undirected with spots, compileOption: PERMISSIVE
- Right+undirected with spots, compileOption: LEGACY
- Right+undirected shorthand, compileOption: PERMISSIVE
- Right+undirected shorthand, compileOption: LEGACY
- Right+undirected with variables and labels, compileOption: PERMISSIVE
- Right+undirected with variables and labels, compileOption: LEGACY
- Left+undirected with variables, compileOption: PERMISSIVE
- Left+undirected with variables, compileOption: LEGACY
- Left+undirected with spots, compileOption: PERMISSIVE
- Left+undirected with spots, compileOption: LEGACY
- Left+undirected shorthand, compileOption: PERMISSIVE
- Left+undirected shorthand, compileOption: LEGACY
- Left+undirected with variables and label, compileOption: PERMISSIVE
- Left+undirected with variables and label, compileOption: LEGACY
- Left+right+undirected with variables, compileOption: PERMISSIVE
- Left+right+undirected with variables, compileOption: LEGACY
- Left+right+undirected with spots, compileOption: PERMISSIVE
- Left+right+undirected with spots, compileOption: LEGACY
- Left+right+undirected shorthand, compileOption: PERMISSIVE
- Left+right+undirected shorthand, compileOption: LEGACY
- (N0E0 MATCH (x)), compileOption: PERMISSIVE
- (N0E0 MATCH (x)), compileOption: LEGACY
- (N0E0 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N0E0 MATCH -[y]-> ), compileOption: LEGACY
- (N0E0 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N0E0 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N1E0 MATCH (x)), compileOption: PERMISSIVE
- (N1E0 MATCH (x)), compileOption: LEGACY
- (N1E0 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N1E0 MATCH -[y]-> ), compileOption: LEGACY
- (N1E0 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N1E0 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N1E0 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N1E0 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N1U1 MATCH (x)), compileOption: PERMISSIVE
- (N1U1 MATCH (x)), compileOption: LEGACY
- (N1U1 MATCH ~[y]~ ), compileOption: PERMISSIVE
- (N1U1 MATCH ~[y]~ ), compileOption: LEGACY
- (N1U1 MATCH (x)~[y]~(z) ), compileOption: PERMISSIVE
- (N1U1 MATCH (x)~[y]~(z) ), compileOption: LEGACY
- (N1U1 MATCH (x)~[y]~(x) ), compileOption: PERMISSIVE
- (N1U1 MATCH (x)~[y]~(x) ), compileOption: LEGACY
- (N1U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: PERMISSIVE
- (N1U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: LEGACY
- (N1D2 MATCH (x)), compileOption: PERMISSIVE
- (N1D2 MATCH (x)), compileOption: LEGACY
- (N1D2 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N1D2 MATCH -[y]-> ), compileOption: LEGACY
- (N1D2 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N1D2 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N1D2 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N1D2 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N1D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N1D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2E0 MATCH (x)), compileOption: PERMISSIVE
- (N2E0 MATCH (x)), compileOption: LEGACY
- (N2E0 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N2E0 MATCH -[y]-> ), compileOption: LEGACY
- (N2E0 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N2E0 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N2E0 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N2E0 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N2D1 MATCH (x)), compileOption: PERMISSIVE
- (N2D1 MATCH (x)), compileOption: LEGACY
- (N2D1 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N2D1 MATCH -[y]-> ), compileOption: LEGACY
- (N2D1 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N2D1 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N2D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D1 MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D1 MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2U1 MATCH (x)), compileOption: PERMISSIVE
- (N2U1 MATCH (x)), compileOption: LEGACY
- (N2U1 MATCH ~[y]~ ), compileOption: PERMISSIVE
- (N2U1 MATCH ~[y]~ ), compileOption: LEGACY
- (N2U1 MATCH (x)~[y]~(z) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x)~[y]~(z) ), compileOption: LEGACY
- (N2U1 MATCH (x)~[y]~(x) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x)~[y]~(x) ), compileOption: LEGACY
- (N2U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: LEGACY
- (N2U1 MATCH (x1)~[y1]~(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x1)~[y1]~(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2U1 MATCH (x1)-[y1]-(x2)~[y2]~(x3) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x1)-[y1]-(x2)~[y2]~(x3) ), compileOption: LEGACY
- (N2U1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2U1 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D2 MATCH (x)), compileOption: PERMISSIVE
- (N2D2 MATCH (x)), compileOption: LEGACY
- (N2D2 MATCH -[y]-> ), compileOption: PERMISSIVE
- (N2D2 MATCH -[y]-> ), compileOption: LEGACY
- (N2D2 MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N2D2 MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N2D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D2 MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D2 MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D2 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D2 MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D2c MATCH (x)), compileOption: PERMISSIVE
- (N2D2c MATCH (x)), compileOption: LEGACY
- (N2D2c MATCH -[y]-> ), compileOption: PERMISSIVE
- (N2D2c MATCH -[y]-> ), compileOption: LEGACY
- (N2D2c MATCH (x)-[y]->(z) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x)-[y]->(z) ), compileOption: LEGACY
- (N2D2c MATCH (x)-[y]->(x) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x)-[y]->(x) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x1) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]->(x1) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]->(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]-(x2)-[y2]->(x3) ), compileOption: LEGACY
- (N2D2c MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: PERMISSIVE
- (N2D2c MATCH (x1)-[y1]-(x2)-[y2]-(x3) ), compileOption: LEGACY
- (N2U2 MATCH (x)), compileOption: PERMISSIVE
- (N2U2 MATCH (x)), compileOption: LEGACY
- (N2U2 MATCH ~[y]~ ), compileOption: PERMISSIVE
- (N2U2 MATCH ~[y]~ ), compileOption: LEGACY
- (N2U2 MATCH (x)~[y]~(z) ), compileOption: PERMISSIVE
- (N2U2 MATCH (x)~[y]~(z) ), compileOption: LEGACY
- (N2U2 MATCH (x)~[y]~(x) ), compileOption: PERMISSIVE
- (N2U2 MATCH (x)~[y]~(x) ), compileOption: LEGACY
- (N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: PERMISSIVE
- (N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x3) ), compileOption: LEGACY
- (N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x1) ), compileOption: PERMISSIVE
- (N2U2 MATCH (x1)~[y1]~(x2)~[y2]~(x1) ), compileOption: LEGACY
- cast to MISSING valid cases{value:"NULL"}, compileOption: PERMISSIVE
- cast to MISSING valid cases{value:"NULL"}, compileOption: LEGACY
- cast to MISSING valid cases{value:"MISSING"}, compileOption: PERMISSIVE
- cast to MISSING valid cases{value:"MISSING"}, compileOption: LEGACY
- cast to NULL valid cases{value:"MISSING"}, compileOption: PERMISSIVE
- cast to NULL valid cases{value:"MISSING"}, compileOption: LEGACY
- cast to int invalid target type{value:"`2017T`",target:"TIMESTAMP"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:" `{{\"\"}}` ",target:"CLOB"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:" `{{\"1\"}}` ",target:"CLOB"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"`{{}}`",target:"BLOB"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"[1, 2]",target:"LIST"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"[1]",target:"LIST"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"[]",target:"LIST"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"`(1 2)`",target:"SEXP"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"`(1)`",target:"SEXP"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"`()`",target:"SEXP"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"{'a': 1}",target:"STRUCT"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"{'a': '12'}",target:"STRUCT"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"{}",target:"STRUCT"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"<<1, 2>>",target:"BAG"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"<<1>>",target:"BAG"}, compileOption: PERMISSIVE
- cast to int invalid target type{value:"<<>>",target:"BAG"}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null ESCAPE null "}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null ESCAPE null "}, compileOption: LEGACY
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null ESCAPE '['  "}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null ESCAPE '['  "}, compileOption: LEGACY
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE 'S1' ESCAPE null "}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE 'S1' ESCAPE null "}, compileOption: LEGACY
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null "}, compileOption: PERMISSIVE
- null value on any of the 3 inputs returns false{likeExpr:" d.sid LIKE null "}, compileOption: LEGACY
- MISSING LIKE 'some pattern', compileOption: PERMISSIVE
- 'some value' LIKE MISSING, compileOption: PERMISSIVE
- MISSING LIKE MISSING, compileOption: PERMISSIVE
- NULL LIKE MISSING, compileOption: PERMISSIVE
- MISSING LIKE NULL, compileOption: PERMISSIVE
- MISSING LIKE 'some pattern' ESCAPE '/', compileOption: PERMISSIVE
- 'some value' LIKE MISSING ESCAPE '/', compileOption: PERMISSIVE
- 'some value' LIKE 'some pattern' ESCAPE MISSING, compileOption: PERMISSIVE
- NULL LIKE 'some pattern' ESCAPE MISSING, compileOption: PERMISSIVE
- 'some value' LIKE NULL ESCAPE MISSING, compileOption: PERMISSIVE
- outerUnionCoerceScalar, compileOption: PERMISSIVE
- outerUnionCoerceScalar, compileOption: LEGACY
- outerUnionCoerceStruct, compileOption: PERMISSIVE
- outerUnionCoerceStruct, compileOption: LEGACY
- outerUnionCoerceNullMissing, compileOption: PERMISSIVE
- outerUnionCoerceNullMissing, compileOption: LEGACY
- inPredicate, compileOption: PERMISSIVE
- inPredicate, compileOption: LEGACY
- inPredicateSingleItem, compileOption: PERMISSIVE
- inPredicateSingleItem, compileOption: LEGACY
- inPredicateSingleExpr, compileOption: PERMISSIVE
- inPredicateSingleExpr, compileOption: LEGACY
- inPredicateSingleItemListVar, compileOption: PERMISSIVE
- inPredicateSingleItemListVar, compileOption: LEGACY
- inPredicateSingleListVar, compileOption: PERMISSIVE
- inPredicateSingleListVar, compileOption: LEGACY
- inPredicateSubQuerySelectValue, compileOption: PERMISSIVE
- inPredicateSubQuerySelectValue, compileOption: LEGACY
- notInPredicate, compileOption: PERMISSIVE
- notInPredicate, compileOption: LEGACY
- notInPredicateSingleItem, compileOption: PERMISSIVE
- notInPredicateSingleItem, compileOption: LEGACY
- notInPredicateSingleExpr, compileOption: PERMISSIVE
- notInPredicateSingleItemListVar, compileOption: PERMISSIVE
- notInPredicateSingleItemListVar, compileOption: LEGACY
- notInPredicateSingleListVar, compileOption: PERMISSIVE
- notInPredicateSingleListVar, compileOption: LEGACY
- notInPredicateSubQuerySelectValue, compileOption: PERMISSIVE
- notInPredicateSubQuerySelectValue, compileOption: LEGACY
- inPredicateWithTableConstructor, compileOption: PERMISSIVE
- inPredicateWithTableConstructor, compileOption: LEGACY
- notInPredicateWithTableConstructor, compileOption: PERMISSIVE
- notInPredicateWithTableConstructor, compileOption: LEGACY
- inPredicateWithExpressionOnRightSide, compileOption: PERMISSIVE
- inPredicateWithExpressionOnRightSide, compileOption: LEGACY
- notInPredicateWithExpressionOnRightSide, compileOption: PERMISSIVE
- notInPredicateWithExpressionOnRightSide, compileOption: LEGACY
- || valid cases{lparam:"null",rparam:"missing",result:missing::null}, compileOption: PERMISSIVE
- || valid cases{lparam:"missing",rparam:"null",result:missing::null}, compileOption: PERMISSIVE
- || valid cases{lparam:"missing",rparam:"'b'",result:missing::null}, compileOption: PERMISSIVE
- || valid cases{lparam:"'a'",rparam:"missing",result:missing::null}, compileOption: PERMISSIVE
- || valid cases{lparam:"missing",rparam:"missing",result:missing::null}, compileOption: PERMISSIVE
- repeatingDecimal, compileOption: PERMISSIVE
- repeatingDecimal, compileOption: LEGACY
- repeatingDecimalHigherPrecision, compileOption: PERMISSIVE
- repeatingDecimalHigherPrecision, compileOption: LEGACY
- divDecimalInt, compileOption: PERMISSIVE
- divDecimalInt, compileOption: LEGACY
- subtractionOutOfAllowedPrecision, compileOption: PERMISSIVE
- subtractionOutOfAllowedPrecision, compileOption: LEGACY
- equalListDifferentTypesTrue, compileOption: PERMISSIVE
- equalListDifferentTypesTrue, compileOption: LEGACY
- simpleCase, compileOption: PERMISSIVE
- simpleCase, compileOption: LEGACY
- simpleCaseNoElse, compileOption: PERMISSIVE
- simpleCaseNoElse, compileOption: LEGACY
- searchedCase, compileOption: PERMISSIVE
- searchedCase, compileOption: LEGACY
- searchedCaseNoElse, compileOption: PERMISSIVE
- searchedCaseNoElse, compileOption: LEGACY
- dateTimePartsAsVariableNames, compileOption: LEGACY
- pathDotMissingAttribute, compileOption: LEGACY
- pathMissingDotName, compileOption: PERMISSIVE
- pathMissingDotName, compileOption: LEGACY
- pathNullDotName, compileOption: PERMISSIVE
- pathNullDotName, compileOption: LEGACY
- pathIndexBagLiteral, compileOption: PERMISSIVE
- pathIndexBagLiteral, compileOption: LEGACY
- pathIndexStructLiteral, compileOption: PERMISSIVE
- pathIndexStructLiteral, compileOption: LEGACY
- pathIndexStructOutOfBoundsLowLiteral, compileOption: PERMISSIVE
- pathIndexStructOutOfBoundsLowLiteral, compileOption: LEGACY
- pathIndexStructOutOfBoundsHighLiteral, compileOption: PERMISSIVE
- pathIndexStructOutOfBoundsHighLiteral, compileOption: LEGACY
- pathDoubleWildCard, compileOption: PERMISSIVE
- pathDoubleWildCard, compileOption: LEGACY
- pathWildCardOverScalar, compileOption: LEGACY
- pathUnpivotWildCardOverScalar, compileOption: LEGACY
- pathWildCardOverScalarMultiple, compileOption: LEGACY
- pathUnpivotWildCardOverScalarMultiple, compileOption: LEGACY
- pathWildCardOverStructMultiple, compileOption: LEGACY
- unpivotMissing, compileOption: PERMISSIVE
- unpivotMissing, compileOption: LEGACY
- unpivotEmptyStruct, compileOption: PERMISSIVE
- unpivotEmptyStruct, compileOption: LEGACY
- unpivotStructWithMissingField, compileOption: PERMISSIVE
- unpivotStructWithMissingField, compileOption: LEGACY
- unpivotMissingWithAsAndAt, compileOption: LEGACY
- unpivotMissingCrossJoinWithAsAndAt, compileOption: LEGACY
- pathUnpivotEmptyStruct1, compileOption: PERMISSIVE
- pathUnpivotEmptyStruct1, compileOption: LEGACY
- pathUnpivotEmptyStruct2, compileOption: PERMISSIVE
- pathUnpivotEmptyStruct2, compileOption: LEGACY
- pathUnpivotEmptyStruct3, compileOption: PERMISSIVE
- pathUnpivotEmptyStruct3, compileOption: LEGACY
- dotted path expression with quoted field name accesses field UNAMBIGUOUS_FIELD (uppercase), compileOption: LEGACY
- subscript with variable in lowercase, compileOption: PERMISSIVE
- subscript with variable in lowercase, compileOption: LEGACY
- subscript with variable in uppercase, compileOption: PERMISSIVE
- subscript with variable in uppercase, compileOption: LEGACY
- subscript with variable in mixed case, compileOption: PERMISSIVE
- subscript with variable in mixed case, compileOption: LEGACY
- subscript with non-existent variable in lowercase, compileOption: PERMISSIVE
- subscript with non-existent variable in uppercase, compileOption: PERMISSIVE
- null comparison{sql:"MISSING IS NULL",result:true}, compileOption: PERMISSIVE
- null comparison{sql:"MISSING IS NULL",result:true}, compileOption: LEGACY
- null comparison{sql:"MISSING = NULL",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"NULL = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.sexp` = NULL",result:null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.sexp` = NULL",result:null}, compileOption: LEGACY
- null comparison{sql:"`null.null` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.bool` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.int` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.decimal` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.string` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.symbol` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.clob` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.blob` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.list` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.struct` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- null comparison{sql:"`null.sexp` = MISSING",result:missing::null}, compileOption: PERMISSIVE
- concatenation with null values{left:"MISSING",right:"MISSING"}, compileOption: PERMISSIVE
- concatenation with null values{left:"''",right:"MISSING"}, compileOption: PERMISSIVE
- concatenation with null values{left:"MISSING",right:"''"}, compileOption: PERMISSIVE
- concatenation with null values{left:"'a'",right:"MISSING"}, compileOption: PERMISSIVE
- concatenation with null values{left:"MISSING",right:"'b'"}, compileOption: PERMISSIVE
- char_length null and missing propagation{in:"missing",result:(success missing::null)}, compileOption: PERMISSIVE
- character_length null and missing propagation{in:"missing",result:(success missing::null)}, compileOption: PERMISSIVE
- CHARACTER_LENGTH invalid type, compileOption: PERMISSIVE
- upper null and missing propagation{param:"missing"}, compileOption: PERMISSIVE
- cardinality null and missing propagation{param:"missing"}, compileOption: PERMISSIVE
- CARDINALITY('foo') type mismatch, compileOption: PERMISSIVE
- EXTRACT(YEAR FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(MONTH FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(DAY FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(HOUR FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(MINUTE FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(SECOND FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(TIMEZONE_HOUR FROM MISSING), compileOption: PERMISSIVE
- EXTRACT(TIMEZONE_MINUTE FROM MISSING), compileOption: PERMISSIVE
- invalid extract year from time, compileOption: PERMISSIVE
- invalid extract month from time, compileOption: PERMISSIVE
- invalid extract day from time, compileOption: PERMISSIVE
- invalid extract month from time with time zone, compileOption: PERMISSIVE
- invalid extract day from time with time zone, compileOption: PERMISSIVE
- POSITION MISSING in string, compileOption: PERMISSIVE
- POSITION string in MISSING, compileOption: PERMISSIVE
- POSITION NULL in MISSING, compileOption: PERMISSIVE
- POSITION MISSING in NULL, compileOption: PERMISSIVE
- POSITION MISSING in MISSING, compileOption: PERMISSIVE
- POSITION invalid type in string, compileOption: PERMISSIVE
- POSITION string in invalid type, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"missing",start_pos:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"''",start_pos:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"missing",start_pos:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"null",start_pos:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 2 arguments{target:"missing",start_pos:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"1",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"null",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"null",start_pos:"missing",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"1",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"null",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"missing",start_pos:"missing",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"1",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"null",quantity:"missing"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"1"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"null"}, compileOption: PERMISSIVE
- substring null and missing propagation 3 arguments{target:"''",start_pos:"missing",quantity:"missing"}, compileOption: PERMISSIVE
- lower null and missing propagation{param:"missing"}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"1",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"1",result:null}, compileOption: LEGACY
- nullif valid cases{first:"1.0",second:"1",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"1.0",second:"1",result:null}, compileOption: LEGACY
- nullif valid cases{first:"1",second:"2",result:1}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"2",result:1}, compileOption: LEGACY
- nullif valid cases{first:"2",second:"'2'",result:2}, compileOption: PERMISSIVE
- nullif valid cases{first:"2",second:"'2'",result:2}, compileOption: LEGACY
- nullif valid cases{first:"{}",second:"{}",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"{}",second:"{}",result:null}, compileOption: LEGACY
- nullif valid cases{first:"[]",second:"[]",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"[]",second:"[]",result:null}, compileOption: LEGACY
- nullif valid cases{first:"{}",second:"[]",result:{}}, compileOption: PERMISSIVE
- nullif valid cases{first:"{}",second:"[]",result:{}}, compileOption: LEGACY
- nullif valid cases{first:"1",second:"null",result:1}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"null",result:1}, compileOption: LEGACY
- nullif valid cases{first:"null",second:"1",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"null",second:"1",result:null}, compileOption: LEGACY
- nullif valid cases{first:"null",second:"null",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"null",second:"null",result:null}, compileOption: LEGACY
- nullif valid cases{first:"1",second:"missing",result:1}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"missing",result:1}, compileOption: LEGACY
- nullif valid cases{first:"missing",second:"1",result:missing::null}, compileOption: PERMISSIVE
- nullif valid cases{first:"missing",second:"1",result:missing::null}, compileOption: LEGACY
- ABS(MISSING) null propogation, compileOption: PERMISSIVE
- ABS('foo'), compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading '' from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing '' from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both '' from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading missing from '')"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing missing from '')"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both missing from '')"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading null from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing null from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both null from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading missing from null)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing missing from null)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both missing from null)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(leading missing from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(trailing missing from missing)"}, compileOption: PERMISSIVE
- trim null and missing propagation{sql:"trim(both missing from missing)"}, compileOption: PERMISSIVE
- MOD(MISSING, 3), compileOption: PERMISSIVE
- MOD(3, MISSING), compileOption: PERMISSIVE
- MOD(MISSING, NULL), compileOption: PERMISSIVE
- MOD(NULL, MISSING), compileOption: PERMISSIVE
- MOD(MISSING, 'some string'), compileOption: PERMISSIVE
- MOD('some string', MISSING), compileOption: PERMISSIVE
- MOD(3, 'some string'), compileOption: PERMISSIVE
- MOD('some string', 3), compileOption: PERMISSIVE
- BIT_LENGTH MISSING, compileOption: PERMISSIVE
- BIT_LENGTH invalid type, compileOption: PERMISSIVE
- OCTET_LENGTH MISSING, compileOption: PERMISSIVE
- OCTET_LENGTH invalid type, compileOption: PERMISSIVE
- OVERLAY MISSING, compileOption: PERMISSIVE
- OVERLAY PLACING MISSING, compileOption: PERMISSIVE
- OVERLAY FROM MISSING, compileOption: PERMISSIVE
- OVERLAY FOR MISSING, compileOption: PERMISSIVE
- OVERLAY mismatched type, compileOption: PERMISSIVE
- OVERLAY PLACING mismatched type, compileOption: PERMISSIVE
- OVERLAY FROM mismatched type, compileOption: PERMISSIVE
- OVERLAY FOR mismatched type, compileOption: PERMISSIVE
- coalesce valid cases{args:"1",result:(success 1)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"1",result:(success 1)}, compileOption: LEGACY
- coalesce valid cases{args:"1, 2",result:(success 1)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"1, 2",result:(success 1)}, compileOption: LEGACY
- coalesce valid cases{args:"null, 2",result:(success 2)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, 2",result:(success 2)}, compileOption: LEGACY
- coalesce valid cases{args:"missing, 3",result:(success 3)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"missing, 3",result:(success 3)}, compileOption: LEGACY
- coalesce valid cases{args:"null, null, 3",result:(success 3)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, null, 3",result:(success 3)}, compileOption: LEGACY
- coalesce valid cases{args:"null, missing, 3",result:(success 3)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, missing, 3",result:(success 3)}, compileOption: LEGACY
- coalesce valid cases{args:"null, missing, null, null, missing, 9, 4, 5, 6",result:(success 9)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, missing, null, null, missing, 9, 4, 5, 6",result:(success 9)}, compileOption: LEGACY
- Empty Symbol in table, compileOption: LEGACY
- Empty Symbol in globals, compileOption: LEGACY
- Empty Symbol in alias, compileOption: LEGACY
- functionCall, compileOption: PERMISSIVE
- functionCall, compileOption: LEGACY
- division with mixed StaticType, compileOption: PERMISSIVE
- division with mixed StaticType, compileOption: LEGACY
- Example 2.3 — Union of Compatible Relations; Mismatch Column Names; Using OUTER UNION, compileOption: PERMISSIVE
- Example 2.3 — Union of Compatible Relations; Mismatch Column Names; Using OUTER UNION, compileOption: LEGACY
- Example 3 — Outer union of Heterogenous Relations, compileOption: PERMISSIVE
- Example 3 — Outer union of Heterogenous Relations, compileOption: LEGACY
- Example 6 — Value Coercion; Coercion of single value, compileOption: PERMISSIVE
- Example 6 — Value Coercion; Coercion of single value, compileOption: LEGACY
- Example 7 — `SELECT * FROM engineering.employees OUTER EXCEPT << >>`, compileOption: PERMISSIVE
- Example 7 — `SELECT * FROM engineering.employees OUTER EXCEPT << >>`, compileOption: LEGACY
- Example 7 — `engineering.employees OUTER UNION << MISSING >>`, compileOption: PERMISSIVE
- Example 7 — `engineering.employees OUTER UNION << MISSING >>`, compileOption: LEGACY
- Example 7 — result is the empty bag, compileOption: PERMISSIVE
- Example 7 — result is the empty bag, compileOption: LEGACY
- undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing, compileOption: PERMISSIVE
- undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing, compileOption: PERMISSIVE
- undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing, compileOption: PERMISSIVE
- undefinedUnqualifiedVariableInSelectWithUndefinedVariableBehaviorMissing, compileOption: LEGACY
- join on column - all column values non-null, compileOption: PERMISSIVE
- join on column - all column values non-null, compileOption: LEGACY
- join on column - some column values are null, compileOption: PERMISSIVE
- join on column - some column values are null, compileOption: LEGACY
- join on column - 1 table contains 1 row with the value null, compileOption: PERMISSIVE
- join on column - 1 table contains 1 row with the value null, compileOption: LEGACY
- join on column - ON condition = false, compileOption: PERMISSIVE
- join on column - ON condition = false, compileOption: LEGACY
- PG_JOIN_01, compileOption: PERMISSIVE
- PG_JOIN_01, compileOption: LEGACY
- PG_JOIN_02, compileOption: PERMISSIVE
- PG_JOIN_02, compileOption: LEGACY
- PG_JOIN_03, compileOption: PERMISSIVE
- PG_JOIN_03, compileOption: LEGACY
- PG_JOIN_06, compileOption: PERMISSIVE
- PG_JOIN_06, compileOption: LEGACY
- PG_JOIN_07, compileOption: PERMISSIVE
- PG_JOIN_07, compileOption: LEGACY
- PG_JOIN_08, compileOption: PERMISSIVE
- PG_JOIN_08, compileOption: LEGACY
- PG_JOIN_09, compileOption: PERMISSIVE
- PG_JOIN_09, compileOption: LEGACY
- PG_JOIN_10, compileOption: PERMISSIVE
- PG_JOIN_10, compileOption: LEGACY
- offset 0, compileOption: PERMISSIVE
- offset 0, compileOption: LEGACY
- offset 1, compileOption: PERMISSIVE
- offset 1, compileOption: LEGACY
- offset 2, compileOption: PERMISSIVE
- offset 2, compileOption: LEGACY
- limit 1 offset 1, compileOption: PERMISSIVE
- limit 1 offset 1, compileOption: LEGACY
- limit 10 offset 1, compileOption: PERMISSIVE
- limit 10 offset 1, compileOption: LEGACY
- limit 2 offset 2, compileOption: PERMISSIVE
- limit 2 offset 2, compileOption: LEGACY
- limit offset after group by, compileOption: PERMISSIVE
- limit offset after group by, compileOption: LEGACY
- offset 2-1, compileOption: PERMISSIVE
- offset 2-1, compileOption: LEGACY
- offset 2+1, compileOption: PERMISSIVE
- offset 2+1, compileOption: LEGACY
- offset 2*1, compileOption: PERMISSIVE
- offset 2*1, compileOption: LEGACY
- offset 2/1, compileOption: PERMISSIVE
- offset 2/1, compileOption: LEGACY
- offset group by having, compileOption: PERMISSIVE
- offset group by having, compileOption: LEGACY
- offset with pivot, compileOption: PERMISSIVE
- offset with pivot, compileOption: LEGACY
- pivotBadFieldType, compileOption: LEGACY
- col1 asc, compileOption: PERMISSIVE
- col1 asc, compileOption: LEGACY
- col1 desc, compileOption: PERMISSIVE
- col1 desc, compileOption: LEGACY
- col1 asc, col2 asc, compileOption: PERMISSIVE
- col1 asc, col2 asc, compileOption: LEGACY
- price desc, productId asc, compileOption: PERMISSIVE
- price desc, productId asc, compileOption: LEGACY
- supplierId_nulls nulls last, compileOption: PERMISSIVE
- supplierId_nulls nulls last, compileOption: LEGACY
- supplierId_nulls nulls first, compileOption: PERMISSIVE
- supplierId_nulls nulls first, compileOption: LEGACY
- supplierId_nulls asc nulls last, productId asc, compileOption: PERMISSIVE
- supplierId_nulls asc nulls last, productId asc, compileOption: LEGACY
- nulls first as default for supplierId_nulls desc, compileOption: PERMISSIVE
- nulls first as default for supplierId_nulls desc, compileOption: LEGACY
- group and order by asc sellerId, compileOption: PERMISSIVE
- group and order by asc sellerId, compileOption: LEGACY
- group and order by desc sellerId, compileOption: PERMISSIVE
- group and order by desc sellerId, compileOption: LEGACY
- group and order by DESC (NULLS FIRST as default), compileOption: PERMISSIVE
- group and order by DESC (NULLS FIRST as default), compileOption: LEGACY
- group and order by ASC (NULLS LAST as default), compileOption: PERMISSIVE
- group and order by ASC (NULLS LAST as default), compileOption: LEGACY
- group and place nulls first (asc as default), compileOption: PERMISSIVE
- group and place nulls first (asc as default), compileOption: LEGACY
- group and place nulls last (asc as default), compileOption: PERMISSIVE
- group and place nulls last (asc as default), compileOption: LEGACY
- group and order by asc and place nulls first, compileOption: PERMISSIVE
- group and order by asc and place nulls first, compileOption: LEGACY
- false before true (ASC), compileOption: PERMISSIVE
- false before true (ASC), compileOption: LEGACY
- true before false (DESC), compileOption: PERMISSIVE
- true before false (DESC), compileOption: LEGACY
- nan before -inf, then numeric values then +inf (ASC), compileOption: PERMISSIVE
- nan before -inf, then numeric values then +inf (ASC), compileOption: LEGACY
- +inf before numeric values then -inf then nan (DESC), compileOption: PERMISSIVE
- +inf before numeric values then -inf then nan (DESC), compileOption: LEGACY
- LOB types follow their lexicographical ordering by octet (ASC), compileOption: PERMISSIVE
- LOB types follow their lexicographical ordering by octet (ASC), compileOption: LEGACY
- LOB types should ordered (DESC), compileOption: PERMISSIVE
- LOB types should ordered (DESC), compileOption: LEGACY
- shorter array comes first (ASC), compileOption: PERMISSIVE
- shorter array comes first (ASC), compileOption: LEGACY
- longer array comes first (DESC), compileOption: PERMISSIVE
- longer array comes first (DESC), compileOption: LEGACY
- lists compared lexicographically based on comparison of elements (ASC), compileOption: PERMISSIVE
- lists compared lexicographically based on comparison of elements (ASC), compileOption: LEGACY
- lists compared lexicographically based on comparison of elements (DESC), compileOption: PERMISSIVE
- lists compared lexicographically based on comparison of elements (DESC), compileOption: LEGACY
- lists items should be ordered by data types (ASC) (nulls last as default for asc), compileOption: PERMISSIVE
- lists items should be ordered by data types (ASC) (nulls last as default for asc), compileOption: LEGACY
- lists items should be ordered by data types (DESC) (nulls first as default for desc), compileOption: PERMISSIVE
- lists items should be ordered by data types (DESC) (nulls first as default for desc), compileOption: LEGACY
- structs compared lexicographically first by key then by value (ASC), compileOption: PERMISSIVE
- structs compared lexicographically first by key then by value (ASC), compileOption: LEGACY
- structs compared lexicographically first by key then by value (DESC), compileOption: PERMISSIVE
- structs compared lexicographically first by key then by value (DESC), compileOption: LEGACY
- structs should be ordered by data types (ASC) (nulls last as default for asc), compileOption: PERMISSIVE
- structs should be ordered by data types (ASC) (nulls last as default for asc), compileOption: LEGACY
- structs should be ordered by data types (DESC) (nulls first as default for desc), compileOption: PERMISSIVE
- structs should be ordered by data types (DESC) (nulls first as default for desc), compileOption: LEGACY
- bags compared as sorted lists (ASC), compileOption: PERMISSIVE
- bags compared as sorted lists (ASC), compileOption: LEGACY
- bags compared as sorted lists (DESC), compileOption: PERMISSIVE
- bags compared as sorted lists (DESC), compileOption: LEGACY
- testing alias support, compileOption: PERMISSIVE
- testing alias support, compileOption: LEGACY
- testing nested alias support, compileOption: PERMISSIVE
- testing nested alias support, compileOption: LEGACY
- Empty Output (ordered), compileOption: PERMISSIVE
- Empty Output (ordered), compileOption: LEGACY
- GROUP BY binding referenced in FROM clause, compileOption: PERMISSIVE
- GROUP BY binding referenced in WHERE clause, compileOption: PERMISSIVE
- GROUP AS binding referenced in FROM clause, compileOption: PERMISSIVE
- GROUP AS binding referenced in WHERE clause, compileOption: PERMISSIVE
- SELECT COUNT(1) AS the_count, COUNT(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, SUM(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, MIN(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, MAX(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, AVG(p.price_missings) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, COUNT(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, SUM(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, MIN(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, MAX(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT COUNT(1) AS the_count, AVG(p.price_mixed) AS the_agg FROM products_sparse AS p, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, COUNT(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, SUM(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, MIN(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, MAX(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, AVG(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, COUNT(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, SUM(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, MIN(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, MAX(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT categoryId, COUNT(1) AS the_count, AVG(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, COUNT(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, SUM(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, MIN(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, MAX(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, AVG(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, COUNT(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, SUM(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, MIN(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, MAX(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- SELECT p.categoryId, COUNT(1) AS the_count, AVG(p.price_mixed) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId, compileOption: LEGACY
- Expression with multiple subqueriees containing aggregates : CAST((SELECT COUNT(1) FROM products) AS LIST)[0]._1 / CAST((SELECT COUNT(1) FROM suppliers) AS LIST)[0]._1, compileOption: PERMISSIVE
- Expression with multiple subqueriees containing aggregates : CAST((SELECT COUNT(1) FROM products) AS LIST)[0]._1 / CAST((SELECT COUNT(1) FROM suppliers) AS LIST)[0]._1, compileOption: LEGACY
- Aggregates with subquery containing another aggregate : SELECT COUNT(1) + CAST((SELECT SUM(numInStock) FROM products) AS LIST)[0]._1 as a_number FROM products, compileOption: PERMISSIVE
- Aggregates with subquery containing another aggregate : SELECT COUNT(1) + CAST((SELECT SUM(numInStock) FROM products) AS LIST)[0]._1 as a_number FROM products, compileOption: LEGACY
- GROUP BY with JOIN : SELECT supplierName, COUNT(*) as the_count FROM suppliers AS s INNER JOIN products AS p ON s.supplierId = p.supplierId GROUP BY supplierName, compileOption: PERMISSIVE
- GROUP BY with JOIN : SELECT supplierName, COUNT(*) as the_count FROM suppliers AS s INNER JOIN products AS p ON s.supplierId = p.supplierId GROUP BY supplierName, compileOption: LEGACY
- SELECT VALUE with nested aggregates : SELECT VALUE (SELECT SUM(outerFromSource.col1) AS the_sum FROM <<1>>) FROM simple_1_col_1_group as outerFromSource, compileOption: PERMISSIVE
- SELECT VALUE with nested aggregates : SELECT VALUE (SELECT SUM(outerFromSource.col1) AS the_sum FROM <<1>>) FROM simple_1_col_1_group as outerFromSource, compileOption: LEGACY
- SELECT col1, g FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g, compileOption: PERMISSIVE
- SELECT col1, g FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g, compileOption: LEGACY
- SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g, compileOption: PERMISSIVE
- SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g, compileOption: LEGACY
- SELECT col1, g FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g, compileOption: PERMISSIVE
- SELECT col1, g FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g, compileOption: LEGACY
- SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g, compileOption: PERMISSIVE
- SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g, compileOption: LEGACY
- MYSQL_SELECT_20, compileOption: PERMISSIVE
- MYSQL_SELECT_20, compileOption: LEGACY
- MYSQL_SELECT_21, compileOption: PERMISSIVE
- MYSQL_SELECT_21, compileOption: LEGACY
- MYSQL_SELECT_26, compileOption: PERMISSIVE
- MYSQL_SELECT_26, compileOption: LEGACY
- selectFromScalarAndAtUnpivotWildCardOverScalar, compileOption: PERMISSIVE
- selectFromScalarAndAtUnpivotWildCardOverScalar, compileOption: LEGACY
- selectFromListAndAtUnpivotWildCardOverScalar, compileOption: PERMISSIVE
- selectFromListAndAtUnpivotWildCardOverScalar, compileOption: LEGACY
- selectFromBagAndAtUnpivotWildCardOverScalar, compileOption: PERMISSIVE
- selectFromBagAndAtUnpivotWildCardOverScalar, compileOption: LEGACY
- selectPathUnpivotWildCardOverStructMultiple, compileOption: PERMISSIVE
- selectPathUnpivotWildCardOverStructMultiple, compileOption: LEGACY
- selectStarSingleSourceHoisted, compileOption: PERMISSIVE
- selectStarSingleSourceHoisted, compileOption: LEGACY
- ordinalAccessWithNegativeIndex, compileOption: LEGACY
- ordinalAccessWithNegativeIndexAndBindings, compileOption: LEGACY
- rangeOverScalar, compileOption: LEGACY
- rangeTwiceOverScalar, compileOption: LEGACY
- rangeOverSexp, compileOption: PERMISSIVE
- rangeOverSexp, compileOption: LEGACY
- rangeOverStruct, compileOption: LEGACY
- rangeOverBagWithAt, compileOption: LEGACY
- rangeOverNestedWithAt, compileOption: LEGACY
- avg group by{agg:'AVG(t.b)',expectedF1:1.25,expectedF2:3.}, compileOption: PERMISSIVE
- avg group by{agg:'AVG(t.b)',expectedF1:1.25,expectedF2:3.}, compileOption: LEGACY
- avg group by{agg:'AVG(ALL t.b)',expectedF1:1.25,expectedF2:3.}, compileOption: PERMISSIVE
- avg group by{agg:'AVG(ALL t.b)',expectedF1:1.25,expectedF2:3.}, compileOption: LEGACY
- avg group by{agg:'AVG(DISTINCT t.b)',expectedF1:1.5,expectedF2:3.}, compileOption: PERMISSIVE
- avg group by{agg:'AVG(DISTINCT t.b)',expectedF1:1.5,expectedF2:3.}, compileOption: LEGACY
- ANY with GROUP BY, compileOption: LEGACY
- ANY DISTINCT with GROUP BY, compileOption: LEGACY
- SOME with GROUP BY, compileOption: LEGACY
- SOME DISTINCT with GROUP BY, compileOption: LEGACY
- EVERY with GROUP BY, compileOption: LEGACY
- EVERY DISTINCT with GROUP BY, compileOption: LEGACY
- selectListMultipleAggregatesNestedQuery, compileOption: PERMISSIVE
- selectListMultipleAggregatesNestedQuery, compileOption: LEGACY
- undefinedUnqualifiedVariable_inSelect_withProjectionOption, compileOption: LEGACY
- projectionIterationBehaviorUnfiltered_select_star, compileOption: PERMISSIVE
- projectionIterationBehaviorUnfiltered_select_star, compileOption: LEGACY
- projectOfSexp, compileOption: PERMISSIVE
- projectOfSexp, compileOption: LEGACY
- projectOfUnpivotPath, compileOption: LEGACY
- alias1.alias2.*, compileOption: LEGACY
- selectImplicitAndExplicitAliasSingleSourceHoisted, compileOption: PERMISSIVE
- selectImplicitAndExplicitAliasSingleSourceHoisted, compileOption: LEGACY
- selectListWithMissing, compileOption: LEGACY
- selectCorrelatedJoin, compileOption: PERMISSIVE
- selectCorrelatedJoin, compileOption: LEGACY
- selectCorrelatedLeftJoin, compileOption: PERMISSIVE
- selectCorrelatedLeftJoin, compileOption: LEGACY
- selectCorrelatedLeftJoinOnClause, compileOption: PERMISSIVE
- selectCorrelatedLeftJoinOnClause, compileOption: LEGACY
- selectJoinOnClauseScoping, compileOption: PERMISSIVE
- selectJoinOnClauseScoping, compileOption: LEGACY
- selectNonCorrelatedJoin, compileOption: LEGACY
- correlatedJoinWithShadowedAttributes, compileOption: LEGACY
- correlatedJoinWithoutLexicalScope, compileOption: LEGACY
- joinWithShadowedGlobal, compileOption: LEGACY
- selectDistinctStarBags, compileOption: PERMISSIVE
- selectDistinctStarBags, compileOption: LEGACY
- variableShadow, compileOption: LEGACY
- selectValueStructConstructorWithMissing, compileOption: LEGACY
- selectIndexStruct, compileOption: PERMISSIVE
- selectIndexStruct, compileOption: LEGACY
- emptySymbol, compileOption: LEGACY
- emptySymbolInGlobals, compileOption: LEGACY
</details>
The following test(s) are failing in legacy but pass in eval. Before merging, confirm they are intended to pass: 
<details><summary>Click here to see</summary>


- equiv group by with aggregates, compileOption: PERMISSIVE

- equiv group by with aggregates, compileOption: LEGACY

- missing and true, compileOption: PERMISSIVE

- coll_count with result of subquery, compileOption: PERMISSIVE

- coll_count with result of subquery, compileOption: LEGACY

- outerUnionAll, compileOption: PERMISSIVE

- outerUnionAll, compileOption: LEGACY

- outerExceptDistinct, compileOption: PERMISSIVE

- outerExceptDistinct, compileOption: LEGACY

- outerUnionCoerceList, compileOption: PERMISSIVE

- outerUnionCoerceList, compileOption: LEGACY

- max top level{agg:'COLL_MAX(data)',result:(success 2)}, compileOption: PERMISSIVE

- max top level{agg:'COLL_MAX(data)',result:(success 2)}, compileOption: LEGACY

- topLevelCollMax, compileOption: PERMISSIVE

- topLevelCollMax, compileOption: LEGACY

- COLL_MAX empty collection, compileOption: PERMISSIVE

- COLL_MAX empty collection, compileOption: LEGACY

- COLL_MAX null, compileOption: PERMISSIVE

- COLL_MAX null, compileOption: LEGACY

- COLL_MAX list of missing element, compileOption: PERMISSIVE

- COLL_MAX list of missing element, compileOption: LEGACY

- COLL_MAX bag of missing elements, compileOption: PERMISSIVE

- COLL_MAX bag of missing elements, compileOption: LEGACY

- COLL_MAX bag of heterogeneous element types, compileOption: PERMISSIVE

- COLL_MAX bag of heterogeneous element types, compileOption: LEGACY

- coll_avg top level{agg:'COLL_AVG(data)',result:(success 1.25)}, compileOption: PERMISSIVE

- coll_avg top level{agg:'COLL_AVG(data)',result:(success 1.25)}, compileOption: LEGACY

- topLevelCollAvg, compileOption: PERMISSIVE

- topLevelCollAvg, compileOption: LEGACY

- topLevelCollAvgOnlyInt, compileOption: PERMISSIVE

- topLevelCollAvgOnlyInt, compileOption: LEGACY

- COLL_AVG empty collection, compileOption: PERMISSIVE

- COLL_AVG empty collection, compileOption: LEGACY

- COLL_AVG null, compileOption: PERMISSIVE

- COLL_AVG null, compileOption: LEGACY

- COLL_AVG list of missing element, compileOption: PERMISSIVE

- COLL_AVG list of missing element, compileOption: LEGACY

- COLL_AVG bag of missing elements, compileOption: PERMISSIVE

- COLL_AVG bag of missing elements, compileOption: LEGACY

- COLL_AVG mistyped element, compileOption: PERMISSIVE

- coll_count top level{agg:'COLL_COUNT(data)',result:(success 4)}, compileOption: PERMISSIVE

- coll_count top level{agg:'COLL_COUNT(data)',result:(success 4)}, compileOption: LEGACY

- topLevelCollCount, compileOption: PERMISSIVE

- topLevelCollCount, compileOption: LEGACY

- COLL_COUNT empty collection, compileOption: PERMISSIVE

- COLL_COUNT empty collection, compileOption: LEGACY

- COLL_COUNT null, compileOption: PERMISSIVE

- COLL_COUNT null, compileOption: LEGACY

- COLL_COUNT list of missing element, compileOption: PERMISSIVE

- COLL_COUNT list of missing element, compileOption: LEGACY

- COLL_COUNT bag of missing elements, compileOption: PERMISSIVE

- COLL_COUNT bag of missing elements, compileOption: LEGACY

- COLL_COUNT bag of heterogeneous element types, compileOption: PERMISSIVE

- COLL_COUNT bag of heterogeneous element types, compileOption: LEGACY

- coll_sum top level{agg:'COLL_SUM(data)',result:(success 5)}, compileOption: PERMISSIVE

- coll_sum top level{agg:'COLL_SUM(data)',result:(success 5)}, compileOption: LEGACY

- topLevelCollSum, compileOption: PERMISSIVE

- topLevelCollSum, compileOption: LEGACY

- COLL_SUM empty collection, compileOption: PERMISSIVE

- COLL_SUM empty collection, compileOption: LEGACY

- COLL_SUM null, compileOption: PERMISSIVE

- COLL_SUM null, compileOption: LEGACY

- COLL_SUM list of missing element, compileOption: PERMISSIVE

- COLL_SUM list of missing element, compileOption: LEGACY

- COLL_SUM bag of missing elements, compileOption: PERMISSIVE

- COLL_SUM bag of missing elements, compileOption: LEGACY

- COLL_SUM mistyped element, compileOption: PERMISSIVE

- coll_min top level{agg:'COLL_MIN(data)',result:(success 1)}, compileOption: PERMISSIVE

- coll_min top level{agg:'COLL_MIN(data)',result:(success 1)}, compileOption: LEGACY

- topLevelCollMin, compileOption: PERMISSIVE

- topLevelCollMin, compileOption: LEGACY

- COLL_MIN empty collection, compileOption: PERMISSIVE

- COLL_MIN empty collection, compileOption: LEGACY

- COLL_MIN null, compileOption: PERMISSIVE

- COLL_MIN null, compileOption: LEGACY

- COLL_MIN list of missing element, compileOption: PERMISSIVE

- COLL_MIN list of missing element, compileOption: LEGACY

- COLL_MIN bag of missing elements, compileOption: PERMISSIVE

- COLL_MIN bag of missing elements, compileOption: LEGACY

- COLL_MIN bag of heterogeneous element types, compileOption: PERMISSIVE

- COLL_MIN bag of heterogeneous element types, compileOption: LEGACY

- COLL_ANY bag literals, compileOption: PERMISSIVE

- COLL_ANY bag literals, compileOption: LEGACY

- COLL_ANY list expressions, compileOption: PERMISSIVE

- COLL_ANY list expressions, compileOption: LEGACY

- COLL_ANY single true, compileOption: PERMISSIVE

- COLL_ANY single true, compileOption: LEGACY

- COLL_ANY single false, compileOption: PERMISSIVE

- COLL_ANY single false, compileOption: LEGACY

- COLL_ANY nulls with true, compileOption: PERMISSIVE

- COLL_ANY nulls with true, compileOption: LEGACY

- COLL_ANY nulls with false, compileOption: PERMISSIVE

- COLL_ANY nulls with false, compileOption: LEGACY

- COLL_ANY nulls only, compileOption: PERMISSIVE

- COLL_ANY nulls only, compileOption: LEGACY

- COLL_ANY null, compileOption: PERMISSIVE

- COLL_ANY null, compileOption: LEGACY

- COLL_ANY list of missing element, compileOption: PERMISSIVE

- COLL_ANY list of missing element, compileOption: LEGACY

- COLL_ANY bag of missing elements, compileOption: PERMISSIVE

- COLL_ANY bag of missing elements, compileOption: LEGACY

- COLL_ANY some empty, compileOption: PERMISSIVE

- COLL_ANY some empty, compileOption: LEGACY

- COLL_ANY one non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_ANY all non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_ANY nested collection, compileOption: PERMISSIVE

- COLL_SOME bag literals, compileOption: PERMISSIVE

- COLL_SOME bag literals, compileOption: LEGACY

- COLL_SOME list expressions, compileOption: PERMISSIVE

- COLL_SOME list expressions, compileOption: LEGACY

- COLL_SOME single true, compileOption: PERMISSIVE

- COLL_SOME single true, compileOption: LEGACY

- COLL_SOME single false, compileOption: PERMISSIVE

- COLL_SOME single false, compileOption: LEGACY

- COLL_SOME nulls with true, compileOption: PERMISSIVE

- COLL_SOME nulls with true, compileOption: LEGACY

- COLL_SOME nulls with false, compileOption: PERMISSIVE

- COLL_SOME nulls with false, compileOption: LEGACY

- COLL_SOME nulls only, compileOption: PERMISSIVE

- COLL_SOME nulls only, compileOption: LEGACY

- COLL_SOME null, compileOption: PERMISSIVE

- COLL_SOME null, compileOption: LEGACY

- COLL_SOME list of missing element, compileOption: PERMISSIVE

- COLL_SOME list of missing element, compileOption: LEGACY

- COLL_SOME bag of missing elements, compileOption: PERMISSIVE

- COLL_SOME bag of missing elements, compileOption: LEGACY

- COLL_SOME some empty, compileOption: PERMISSIVE

- COLL_SOME some empty, compileOption: LEGACY

- COLL_SOME one non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_SOME all non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_SOME nested collection, compileOption: PERMISSIVE

- COLL_EVERY bag literals, compileOption: PERMISSIVE

- COLL_EVERY bag literals, compileOption: LEGACY

- COLL_EVERY list expressions, compileOption: PERMISSIVE

- COLL_EVERY list expressions, compileOption: LEGACY

- COLL_EVERY single true, compileOption: PERMISSIVE

- COLL_EVERY single true, compileOption: LEGACY

- COLL_EVERY single false, compileOption: PERMISSIVE

- COLL_EVERY single false, compileOption: LEGACY

- COLL_EVERY null and missing with true, compileOption: PERMISSIVE

- COLL_EVERY null and missing with true, compileOption: LEGACY

- COLL_EVERY null with false, compileOption: PERMISSIVE

- COLL_EVERY null with false, compileOption: LEGACY

- COLL_EVERY null and missing only, compileOption: PERMISSIVE

- COLL_EVERY null and missing only, compileOption: LEGACY

- COLL_EVERY null, compileOption: PERMISSIVE

- COLL_EVERY null, compileOption: LEGACY

- COLL_EVERY list of missing element, compileOption: PERMISSIVE

- COLL_EVERY list of missing element, compileOption: LEGACY

- COLL_EVERY bag of missing elements, compileOption: PERMISSIVE

- COLL_EVERY bag of missing elements, compileOption: LEGACY

- COLL_EVERY empty collection, compileOption: PERMISSIVE

- COLL_EVERY empty collection, compileOption: LEGACY

- COLL_EVERY one non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_EVERY all non-bool, non-unknown, compileOption: PERMISSIVE

- COLL_EVERY nested collection, compileOption: PERMISSIVE

- selectValueCollAggregate, compileOption: PERMISSIVE

- selectValueCollAggregate, compileOption: LEGACY

- EXTRACT(SECOND FROM `2000-01-02T03:04:05.67Z`), compileOption: PERMISSIVE

- EXTRACT(SECOND FROM `2000-01-02T03:04:05.67Z`), compileOption: LEGACY

- EXTRACT(SECOND FROM `2000-01-02T03:04:05.67+08:09`), compileOption: PERMISSIVE

- EXTRACT(SECOND FROM `2000-01-02T03:04:05.67+08:09`), compileOption: LEGACY

- offset 2^63, compileOption: PERMISSIVE

- offset 2^63, compileOption: LEGACY

- SELECT supplierId_missings FROM products_sparse p GROUP BY p.supplierId_missings, compileOption: PERMISSIVE

- SELECT p.supplierId_missings FROM products_sparse p GROUP BY p.supplierId_missings, compileOption: PERMISSIVE

- SELECT VALUE { 'supplierId_missings' : p.supplierId_missings } FROM products_sparse p GROUP BY p.supplierId_missings, compileOption: PERMISSIVE

- SELECT supplierId_mixed FROM products_sparse p GROUP BY p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT p.supplierId_mixed FROM products_sparse p GROUP BY p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT VALUE { 'supplierId_mixed' : p.supplierId_mixed } FROM products_sparse p GROUP BY p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT regionId, supplierId_missings FROM products_sparse p GROUP BY p.regionId, p.supplierId_missings, compileOption: PERMISSIVE

- SELECT p.regionId, p.supplierId_missings FROM products_sparse p GROUP BY p.regionId, p.supplierId_missings, compileOption: PERMISSIVE

- SELECT VALUE { 'regionId': p.regionId, 'supplierId_missings': p.supplierId_missings } FROM products_sparse p GROUP BY p.regionId, p.supplierId_missings, compileOption: PERMISSIVE

- SELECT regionId, supplierId_mixed FROM products_sparse p GROUP BY p.regionId, p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT regionId, p.supplierId_mixed FROM products_sparse p GROUP BY p.regionId, p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT VALUE { 'regionId': p.regionId, 'supplierId_mixed': p.supplierId_mixed } FROM products_sparse p GROUP BY p.regionId, p.supplierId_mixed, compileOption: PERMISSIVE

- SELECT with nested aggregates (complex) 2, compileOption: PERMISSIVE

- SELECT with nested aggregates (complex) 2, compileOption: LEGACY

</details>

### Conformance comparison report-Cross Commit-LEGACY
| | Base (HEAD) | HEAD | +/- |
| --- | ---: | ---: | ---: |
| % Passing | 92.51% | 92.47% | -0.03% |
| :white_check_mark: Passing | 5382 | 5380 | -2 |
| :x: Failing | 436 | 438 | 2 |
| :large_orange_diamond: Ignored | 0 | 0 | 0 |
| Total Tests | 5818 | 5818 | 0 |
Number passing in both: 5380

Number failing in both: 436

Number passing in Base (HEAD) but now fail: 2

Number failing in Base (HEAD) but now pass: 0
:interrobang: CONFORMANCE REPORT REGRESSION DETECTED :interrobang:. The following test(s) were previously passing but now fail:
<details><summary>Click here to see</summary>


- outerExceptDistinct, compileOption: PERMISSIVE
- outerExceptDistinct, compileOption: LEGACY
</details>

### Conformance comparison report-Cross Commit-EVAL
| | Base (HEAD) | HEAD | +/- |
| --- | ---: | ---: | ---: |
| % Passing | 82.81% | 82.37% | -0.45% |
| :white_check_mark: Passing | 4819 | 4792 | -27 |
| :x: Failing | 1000 | 1026 | 26 |
| :large_orange_diamond: Ignored | 0 | 0 | 0 |
| Total Tests | 5819 | 5818 | -1 |
Number passing in both: 4711

Number failing in both: 918

Number passing in Base (HEAD) but now fail: 108

Number failing in Base (HEAD) but now pass: 82
:interrobang: CONFORMANCE REPORT REGRESSION DETECTED :interrobang:. The following test(s) were previously passing but now fail:
<details><summary>Click here to see</summary>


- inPredicate, compileOption: PERMISSIVE
- inPredicate, compileOption: LEGACY
- inPredicateSingleItem, compileOption: PERMISSIVE
- inPredicateSingleItem, compileOption: LEGACY
- inPredicateSingleItemListVar, compileOption: PERMISSIVE
- inPredicateSubQuerySelectValue, compileOption: PERMISSIVE
- inPredicateSubQuerySelectValue, compileOption: LEGACY
- notInPredicate, compileOption: PERMISSIVE
- notInPredicate, compileOption: LEGACY
- notInPredicateSingleItem, compileOption: PERMISSIVE
- notInPredicateSingleItem, compileOption: LEGACY
- notInPredicateSingleItemListVar, compileOption: PERMISSIVE
- notInPredicateSubQuerySelectValue, compileOption: PERMISSIVE
- notInPredicateSubQuerySelectValue, compileOption: LEGACY
- inPredicateWithTableConstructor, compileOption: PERMISSIVE
- inPredicateWithTableConstructor, compileOption: LEGACY
- notInPredicateWithTableConstructor, compileOption: PERMISSIVE
- notInPredicateWithTableConstructor, compileOption: LEGACY
- inPredicateWithExpressionOnRightSide, compileOption: PERMISSIVE
- inPredicateWithExpressionOnRightSide, compileOption: LEGACY
- notInPredicateWithExpressionOnRightSide, compileOption: PERMISSIVE
- notInPredicateWithExpressionOnRightSide, compileOption: LEGACY
- pathDoubleWildCard, compileOption: PERMISSIVE
- pathDoubleWildCard, compileOption: LEGACY
- nullif valid cases{first:"1",second:"1",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"1",result:null}, compileOption: LEGACY
- nullif valid cases{first:"1",second:"2",result:1}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"2",result:1}, compileOption: LEGACY
- nullif valid cases{first:"2",second:"'2'",result:2}, compileOption: PERMISSIVE
- nullif valid cases{first:"2",second:"'2'",result:2}, compileOption: LEGACY
- nullif valid cases{first:"{}",second:"{}",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"{}",second:"{}",result:null}, compileOption: LEGACY
- nullif valid cases{first:"[]",second:"[]",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"[]",second:"[]",result:null}, compileOption: LEGACY
- nullif valid cases{first:"{}",second:"[]",result:{}}, compileOption: PERMISSIVE
- nullif valid cases{first:"{}",second:"[]",result:{}}, compileOption: LEGACY
- nullif valid cases{first:"1",second:"null",result:1}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"null",result:1}, compileOption: LEGACY
- nullif valid cases{first:"null",second:"1",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"null",second:"1",result:null}, compileOption: LEGACY
- nullif valid cases{first:"null",second:"null",result:null}, compileOption: PERMISSIVE
- nullif valid cases{first:"null",second:"null",result:null}, compileOption: LEGACY
- nullif valid cases{first:"1",second:"missing",result:1}, compileOption: PERMISSIVE
- nullif valid cases{first:"1",second:"missing",result:1}, compileOption: LEGACY
- nullif valid cases{first:"missing",second:"1",result:missing::null}, compileOption: PERMISSIVE
- nullif valid cases{first:"missing",second:"1",result:missing::null}, compileOption: LEGACY
- nullif valid cases{first:"missing",second:"missing",result:missing}, compileOption: PERMISSIVE
- nullif valid cases{first:"missing",second:"missing",result:missing}, compileOption: LEGACY
- coalesce valid cases{args:"1",result:(success 1)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"1",result:(success 1)}, compileOption: LEGACY
- coalesce valid cases{args:"1, 2",result:(success 1)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"1, 2",result:(success 1)}, compileOption: LEGACY
- coalesce valid cases{args:"null, 2",result:(success 2)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, 2",result:(success 2)}, compileOption: LEGACY
- coalesce valid cases{args:"null, null, 3",result:(success 3)}, compileOption: PERMISSIVE
- coalesce valid cases{args:"null, null, 3",result:(success 3)}, compileOption: LEGACY
- join on column - all column values non-null, compileOption: PERMISSIVE
- join on column - all column values non-null, compileOption: LEGACY
- join on column - some column values are null, compileOption: PERMISSIVE
- join on column - some column values are null, compileOption: LEGACY
- join on column - 1 table contains 1 row with the value null, compileOption: PERMISSIVE
- join on column - ON condition = false, compileOption: PERMISSIVE
- join on column - ON condition = false, compileOption: LEGACY
- PG_JOIN_01, compileOption: PERMISSIVE
- PG_JOIN_01, compileOption: LEGACY
- PG_JOIN_02, compileOption: PERMISSIVE
- PG_JOIN_02, compileOption: LEGACY
- PG_JOIN_03, compileOption: PERMISSIVE
- PG_JOIN_03, compileOption: LEGACY
- PG_JOIN_06, compileOption: PERMISSIVE
- PG_JOIN_06, compileOption: LEGACY
- PG_JOIN_08, compileOption: PERMISSIVE
- PG_JOIN_08, compileOption: LEGACY
- PG_JOIN_10, compileOption: PERMISSIVE
- PG_JOIN_10, compileOption: LEGACY
- offset 0, compileOption: PERMISSIVE
- offset 0, compileOption: LEGACY
- offset 1, compileOption: PERMISSIVE
- offset 1, compileOption: LEGACY
- offset 2, compileOption: PERMISSIVE
- offset 2, compileOption: LEGACY
- limit 1 offset 1, compileOption: PERMISSIVE
- limit 1 offset 1, compileOption: LEGACY
- limit 10 offset 1, compileOption: PERMISSIVE
- limit 10 offset 1, compileOption: LEGACY
- limit 2 offset 2, compileOption: PERMISSIVE
- limit 2 offset 2, compileOption: LEGACY
- limit offset after group by, compileOption: PERMISSIVE
- limit offset after group by, compileOption: LEGACY
- offset 2-1, compileOption: PERMISSIVE
- offset 2-1, compileOption: LEGACY
- offset 2+1, compileOption: PERMISSIVE
- offset 2+1, compileOption: LEGACY
- offset 2*1, compileOption: PERMISSIVE
- offset 2*1, compileOption: LEGACY
- offset 2/1, compileOption: PERMISSIVE
- offset 2/1, compileOption: LEGACY
- offset group by having, compileOption: PERMISSIVE
- offset group by having, compileOption: LEGACY
- offset with pivot, compileOption: PERMISSIVE
- offset with pivot, compileOption: LEGACY
- offset 1-2, compileOption: PERMISSIVE
- selectStarSingleSourceHoisted, compileOption: PERMISSIVE
- selectStarSingleSourceHoisted, compileOption: LEGACY
- selectImplicitAndExplicitAliasSingleSourceHoisted, compileOption: PERMISSIVE
- selectImplicitAndExplicitAliasSingleSourceHoisted, compileOption: LEGACY
- selectCorrelatedJoin, compileOption: PERMISSIVE
- selectCorrelatedJoin, compileOption: LEGACY
</details>
The following test(s) were previously failing but now pass. Before merging, confirm they are intended to pass: 
<details><summary>Click here to see</summary>


- equiv attribute value pair unpivot missing, compileOption: LEGACY

- tuple navigation with array notation without explicit CAST to string, compileOption: LEGACY

- path on string, compileOption: LEGACY

- tuple navigation missing attribute dot notation, compileOption: LEGACY

- tuple navigation missing attribute array notation, compileOption: LEGACY

- array navigation with wrongly typed array index, compileOption: LEGACY

- data type mismatch in comparison expression, compileOption: LEGACY

- data type mismatch in logical expression, compileOption: LEGACY

- LIKE bad value type, compileOption: LEGACY

- LIKE bad pattern type, compileOption: LEGACY

- LIKE bad escape type, compileOption: LEGACY

- outerUnionDistinct, compileOption: PERMISSIVE

- outerUnionDistinct, compileOption: LEGACY

- outerUnionAll, compileOption: PERMISSIVE

- outerUnionAll, compileOption: LEGACY

- outerIntersectDistinct, compileOption: PERMISSIVE

- outerIntersectDistinct, compileOption: LEGACY

- outerIntersectAll, compileOption: PERMISSIVE

- outerIntersectAll, compileOption: LEGACY

- outerExceptDistinct, compileOption: PERMISSIVE

- outerExceptDistinct, compileOption: LEGACY

- outerExceptAll, compileOption: PERMISSIVE

- outerExceptAll, compileOption: LEGACY

- outerUnionCoerceList, compileOption: PERMISSIVE

- outerUnionCoerceList, compileOption: LEGACY

- notInPredicateSingleExpr, compileOption: LEGACY

- betweenPredicate, compileOption: PERMISSIVE

- betweenPredicate, compileOption: LEGACY

- notBetweenPredicate, compileOption: PERMISSIVE

- notBetweenPredicate, compileOption: LEGACY

- pathUnpivotWildcardFieldsAfter, compileOption: PERMISSIVE

- pathUnpivotWildcardFieldsAfter, compileOption: LEGACY

- pathDoubleUnpivotWildCard, compileOption: PERMISSIVE

- pathDoubleUnpivotWildCard, compileOption: LEGACY

- subscript with non-existent variable in lowercase, compileOption: LEGACY

- subscript with non-existent variable in uppercase, compileOption: LEGACY

- path expression with ambiguous table alias (lowercase), compileOption: LEGACY

- COLL_MAX non-collection, compileOption: LEGACY

- COLL_AVG non-collection, compileOption: LEGACY

- COLL_COUNT non-collection, compileOption: LEGACY

- COLL_SUM non-collection, compileOption: LEGACY

- COLL_MIN non-collection, compileOption: LEGACY

- COLL_ANY non-collection, compileOption: LEGACY

- COLL_SOME non-collection, compileOption: LEGACY

- COLL_EVERY non-collection, compileOption: LEGACY

- selectValueCollAggregate, compileOption: PERMISSIVE

- selectValueCollAggregate, compileOption: LEGACY

- CHARACTER_LENGTH invalid type, compileOption: LEGACY

- CARDINALITY('foo') type mismatch, compileOption: LEGACY

- invalid extract year from time, compileOption: LEGACY

- invalid extract month from time, compileOption: LEGACY

- invalid extract day from time, compileOption: LEGACY

- invalid extract month from time with time zone, compileOption: LEGACY

- invalid extract day from time with time zone, compileOption: LEGACY

- POSITION invalid type in string, compileOption: LEGACY

- POSITION string in invalid type, compileOption: LEGACY

- ABS('foo'), compileOption: LEGACY

- MOD(3, 'some string'), compileOption: LEGACY

- MOD('some string', 3), compileOption: LEGACY

- BIT_LENGTH invalid type, compileOption: LEGACY

- OCTET_LENGTH invalid type, compileOption: LEGACY

- OVERLAY mismatched type, compileOption: LEGACY

- OVERLAY PLACING mismatched type, compileOption: LEGACY

- OVERLAY FROM mismatched type, compileOption: LEGACY

- OVERLAY FOR mismatched type, compileOption: LEGACY

- undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing, compileOption: LEGACY

- undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing, compileOption: LEGACY

- undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing, compileOption: LEGACY

- offset <str>, compileOption: LEGACY

- offset <float>>, compileOption: LEGACY

- GROUP BY binding referenced in FROM clause, compileOption: LEGACY

- GROUP BY binding referenced in WHERE clause, compileOption: LEGACY

- GROUP AS binding referenced in FROM clause, compileOption: LEGACY

- GROUP AS binding referenced in WHERE clause, compileOption: LEGACY

- SELECT COUNT( numInStock) + 2 AS agg FROM products, compileOption: PERMISSIVE

- SELECT COUNT( numInStock) + 2 AS agg FROM products, compileOption: LEGACY

- SELECT COUNT(p.numInStock) + 2 AS agg FROM products as p, compileOption: PERMISSIVE

- SELECT COUNT(p.numInStock) + 2 AS agg FROM products as p, compileOption: LEGACY

- MYSQL_SELECT_23, compileOption: PERMISSIVE

- MYSQL_SELECT_23, compileOption: LEGACY

- projectionIterationBehaviorUnfiltered_select_list, compileOption: PERMISSIVE

- projectionIterationBehaviorUnfiltered_select_list, compileOption: LEGACY

</details>

