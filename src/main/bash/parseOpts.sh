#!/bin/bash

set -o nounset
set -o errexit

for arg in $*; do

   # Split arg on "=". It is OK to have an "=" in the value, but not
   # the key.
   key=$(   echo ${arg} | cut --delimiter== --fields=1  )
   value=$( echo ${arg} | cut --delimiter== --fields=2- )

   case ${key} in
       "--root")
                # The base dir. Must be absolute path.
        root=${value};;
       "--id")
                # 
         id=${value};;
       *)
        echo "Unrecognised option ${key}"
            exit 1
   esac
done


