#!/bin/sh
#PBS -N test1
#PBS -e output/test.err.txt
#PBS -o output/test.out.txt
#PBS -l nodes=1:ppn=1
dd if=/dev/zero count=100 of=/dev/null
