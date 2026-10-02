#!/bin/bash
find -name "*.pf" -type f | while read file
do
  #AWKCMD='{ gsub( "lhs_2010_12_08", "'${PWD}'" ); print }'
  AWKCMD='{ gsub( "lhs_2010_12_08/", "" ); print }'
  awk "${AWKCMD}" $file > $file.$$
  mv $file.$$ $file
done

