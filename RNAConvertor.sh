#!/bin/bash

cd "$( dirname "${BASH_SOURCE[0]}" )"
echo "$@"
java -cp ./target/classes:./src/main/resources/* RNAdataConvertor "$@"
