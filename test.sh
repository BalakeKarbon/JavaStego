#!/bin/bash
javac CS160FinalDietzKarbonWriter.java && cat encode.txt | java CS160FinalDietzKarbonWriter && cat decode.txt | java CS160FinalDietzKarbonWriter
