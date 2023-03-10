#!/bin/bash
#
# Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at:
#
#      http://aws.amazon.com/apache2.0/
#
# or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
# language governing permissions and limitations under the License.
#

cli_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

cd "$cli_path"
../gradlew :partiql-cli:install
../partiql-cli/build/install/partiql-cli/bin/partiql "$@"
