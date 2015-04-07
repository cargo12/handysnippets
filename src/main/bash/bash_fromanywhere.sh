#!/bin/sh

# set useful error properties
# determine base path of this script 

set -o nounset
set -o errexit

basename()
{
# remove whole path until and including last "/"
  case ${BASH_VERSION+set} in
    set ) echo "${1##*/}" ;;
    *   ) echo "$1" | sed -e 's=.*/==';;
  esac
}

dirname()
{
# remove file name including last "/"
  case ${BASH_VERSION+set} in
    set ) echo "${1%/*}" ;;
    *   ) echo "$1" | sed -e 's=/[^/]*$==' ;;
  esac
}

getrealfullprogname()
{
  # If possible, handle the case that someone has created a symlink in
  # /usr/local/bin back to this script in its original unpacked
  # distribution directory.
  thisfile=`{ readlink -f "$1" \
              || { ls -ld "$1" | sed -n -e 's/.* -> //p'; }
            } 2> /dev/null`
  case $thisfile in
    '' ) thisfile="$1" ;;
  esac

  echo "$thisfile"
}
topdir()
{
  progdir=`dirname "$1"`
  case $progdir in
    . | '' | "$1" ) progdir=`pwd` ;;
  esac

  case $progdir in
    */bin ) topdir=`dirname "$progdir"` ;;
    *     ) topdir="$progdir" ;;
  esac

  echo "$topdir"
}

main()
{
  realfullprogname=`getrealfullprogname "$0"`
  prefix=`topdir "$realfullprogname"`
  progname=`basename "$realfullprogname"`

  echo "basename: " ${progname}
  echo "top dir:  " ${prefix}
  echo "full name:" ${realfullprogname}

  PATH=$prefix/bin:$PATH
  export PATH

# execute binary:
  echo exec ${progname}.bin ${1+"$@"} 
}

main ${1+"$@"}

# eof
