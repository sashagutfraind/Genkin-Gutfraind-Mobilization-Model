#!/bin/bash
/cygdrive/c/cygwin/bin/find "." -name "*.pf" | while read F
do 
  TQFILE="${F}.sub"
  OUTFILE="${F}.out"
  ERRFILE="${F}.err"
  echo "#!/bin/bash" >> "${TQFILE}"
  echo "#PBS -o ${TQFILE}" >> "${TQFILE}"
  echo "java -jar radicalization.jar -b ${F} -NS" >> "${TQFILE}"
  qsub ${TQFILE}
done
