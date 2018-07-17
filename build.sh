#!/bin/bash

SKIPTEST=true

pushd ionicgcs
mvn -Dmaven.test.skip=${SKIPTEST} package install
popd

pushd gcsexamples
mvn -Dmaven.test.skip=${SKIPTEST} package
popd
