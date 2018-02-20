#!/bin/bash

# Having issues with
sbt "project generator" clean test
sbt "project integration-tests" test

