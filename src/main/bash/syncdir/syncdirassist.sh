#!/usr/bin/env bash
set -o nounset
set -o errexit

# script to watch a directory and if any changes occur, sync this with a remote,
# either using rclone or rsync
# distinction between dir to watch and local dir to allow use of gocrypt:
# inotifywait can't watch the encrypted view, only the original dir

# call as (with rclone)
# ./syncdir.sh --rclone --plaindir=<dirtowatch> --syncdir=<syncdir> --remote=<remotename> --remotepath=<remotedir>
# call as (with rsync)
# ./syncdir.sh --rsync --plaindir=<dirtowatch> --syncdir=<syncdir> --user=<username> --remote=<remotename> --remotepath=<remotedir>

readonly syncdirprog=syncdir1.sh
readonly configdir=$HOME/.config/syncdir
readonly autostartsfile=${configdir}/autostarts
declare -A ids
declare -A methods
declare -A plaindirs
declare -A syncdirs
declare -A urls

dryrun=""
idtosync=""
showconfig=0

readonly DATE='date +%Y-%m-%d_%H:%M:%S'
# log file
LOG=${configdir}/log
function echo_log {
  echo `$DATE`" $2" |tee -a ${LOG}_${1}
}

loadConfig() {
  if [ -e "${autostartsfile}" ] ; then
    echo_log ""  "============ Reading config from ${autostartsfile}"
    local lineNo=1
    while read line || [[ -n "$line" ]]; do
      IFS=\; read id method ldir sdir rurl <<< "${line}"

      if [ -z "${id}" ] || [ -z "${method}" ] || [ -z "${ldir}" ] || [ -z "${sdir}" ] || [ -z "${rurl}" ] ; then
        echo_log ""  ============ incorrect config file in line ${lineNo}
        exit 1
      fi

      # ignore lines starting with "#"
      [ ${id#"#"} != ${id} ] && continue

      ids[${id}]="${id}"
      methods[${id}]="${method}"
      eval plaindirs[${id}]="${ldir}"
      eval syncdirs[${id}]="${sdir}"
      urls[${id}]="${rurl}"

      ((lineNo++))
    done < "${autostartsfile}"

  else 
    echo `date +"%Y-%m-%d %H:%M:%S"` "============ No config file ${autostartsfile}"
  fi
}

mountgocrypt() {
  local id=${1:-""} 
  # mount syncdir if necessary
  if [ -n "${id}" ] ; then
    local syncdir=${syncdirs[${id}]}
    local plaindir=${plaindirs[${id}]}
    if [ ! -e "${syncdir}/gocryptfs.diriv" ] ; then
      echo_log "${id}"  "============ Reverse mounting crypted dir ${syncdir}"
      echo_log "${id}"  "Enter gocrypt password for ${plaindir} - ${syncdir}:"
      gocryptfs -reverse -q "${plaindir}" "${syncdir}" 
    else
      echo_log "${id}"  "============ Crypted dir already mounted: ${syncdir}"
    fi
  fi
}

function finish {
  echo_log ""  "============ Exiting"
}
trap finish EXIT

parseoptions() {
  for arg in $*; do
     # Split arg on "=". It is OK to have an "=" in the value, but not
     # the key.
     key=$(   echo ${arg} | cut --delimiter== --fields=1  )
     value=$( echo ${arg} | cut --delimiter== --fields=2- )

     case ${key} in
         "--dry-run")
           dryrun="--dry-run"
           ;;
         "--sync")
           idtosync="${value}"
           ;;
         "--showconfig")
           showconfig=1
           ;;
         "--log")
           LOG="${value}"
           ;;
         *)
          echo `date +"%Y-%m-%d %H:%M:%S"` "Unrecognised option ${key}"
              exit 1
     esac
  done
}

main() {
  parseoptions $@
  loadConfig 

  if [ ${showconfig} = 1 ] ; then
    echo `date +"%Y-%m-%d %H:%M:%S"` "============ Config:"
    for id in "${!ids[@]}"; do
      echo "[${id}]"
      echo "          Plain dir ${plaindirs[${id}]}"
      echo "          Synced dir ${syncdirs[${id}]}"
      echo "          Remote Url ${urls[${id}]}"
      echo "          Method ${methods[${id}]}"
    done
    exit
  fi

  if [ -z "${ids[${idtosync}]:-""}" ] ; then
    echo `date +"%Y-%m-%d %H:%M:%S"` No config for [$id] found
  fi

  for id in "${!ids[@]}"; do
    local id=${ids[${id}]}
    local method=${methods[${id}]}
    local syncdir=${syncdirs[${id}]}
    local plaindir=${plaindirs[${id}]}
    local url=${urls[${id}]}
    if [ "${id}" = "${idtosync}" ] ; then
      mountgocrypt ${id}
      echo_log ""  "===== Calling syncdir.sh for [$id]"
      ${syncdirprog} ${dryrun} --id="${id}" --method="${method}" --plaindir="${plaindir}" --syncdir="${syncdir}" --url="${url}" --log=${LOG}_${id}
    fi

  done

#  echo "Assist: I AM $$"
}

main $@
