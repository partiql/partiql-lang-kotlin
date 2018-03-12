
# IonSQL++ User Guide 

## Introduction 

This is the IonSQL++ implementation's user guide. The goal of this
document is to provide to users of IonSQL++ information on the features
implemented and any deviation from the IonSQL++ specification.



## Data Types

### Decimal
IonSQL++ decimals are based on [Ion decimals] from the Ion Specification[@IonSpec] but with a maximum precision of 38 digits, numbers outside this precision 
range will be rounded using a round [half even strategy]. Examples: 

    1.00000000000000000000000000000000000000000001 -> 1.0000000000000000000000000000000000000
    1.99999999999999999999999999999999999999999999 -> 2.0000000000000000000000000000000000000 


