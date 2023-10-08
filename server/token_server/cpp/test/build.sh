#!/bin/bash
g++ -std=c++14 -lgtest -lssl -lcrypto -lgtest -lz main.cpp token_build_test.cpp  -o main
