#!/bin/bash
#
# Pétur Ingi Egilsson ( petur@petur.eu )
# http://www.petur.eu/blog/?p=175
#
# cdf (Change to favorites)
# Usage: cdf favorite
ALIASES=~/bin/cdf.conf
#set -o errexit
#set -o nounset

fullpath=$(grep $1, $ALIASES|cut -d, -f2)
if [ ${#fullpath} -ne 0 ] ; then
    pushd $fullpath
    else
    echo "Error: '$1' has not been defined in $ALIASES"
    echo -n "Do you want to edit the file? (y/n): "
    read editFile
    case $editFile in 
        [yY])
            if [ ! -n "$EDITOR" ] ; then
                # Use the nano editor because
                # the EDITOR env has not been set.
                nano $ALIASES
            else
                $EDITOR $ALIASES
            fi
            ;;
        [nN])
            ;;
        *)
            echo "Please use y,Y,n or N."
            #exit 1
    esac
fi
