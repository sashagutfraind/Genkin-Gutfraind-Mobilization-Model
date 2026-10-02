#!/bin/bash
#generates a list of .pf files without corresponding pkls
#ls *.pkl > pklList1
ls *.pf > pfList
file=pfList

while read line
do
    #echo $line
    pklName="${line}.pkl"
    #echo $pklName
    if [ ! -f $pklName ]
    then
        echo $line
    fi
done < $file
exit 0 

