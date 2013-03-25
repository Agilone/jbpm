#!/bin/bash

./apache-ant-1.9.0/bin/ant clean.demo
cp repository.dep.xml repository.xml
./apache-ant-1.9.0/bin/ant install.demo.noeclipse
./apache-ant-1.9.0/bin/ant start.demo.noeclipse