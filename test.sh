#!/bin/bash
#echo "e
#bike.png
#8" > encode.txt
#python3 nums.py >> encode.txt
#echo "
#out.png
#" >> encode.txt
#echo "d
#out.png
#8" > decode.txt
javac KarbonDietzFinalProj.java
cat encode.txt | java KarbonDietzFinalProj
cat decode.txt | java KarbonDietzFinalProj
