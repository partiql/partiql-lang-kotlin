# PartiQL Coverage

To render:
```shell
# Set Environment Variables
TEST_PROJECT=${PARTIQL_PROJECT}/test/coverage-tests # Path to project containing tests

genhtml --prefix ${TEST_PROJECT}/build/partiql/coverage/source \
  --ignore-errors source ${TEST_PROJECT}/build/partiql/coverage/report/cov.info --legend \
  --title "PartiQL Code Coverage Report" --output-directory=./ --branch-coverage --show-noncode \
  --show-navigation --show-details
```