#!/usr/bin/env bash
set -o nounset
set -o errexit

# script to watch a directory and if any changes occur, sync this with a remote,
# either using rclone or rsync
# distinction between dir to watch and local dir to allow use of gocrypt:
# inotifywait can't watch the encrypted view, only the original dir

# call as (with rclone)
# ./syncdir.sh --rclone --plaindir=<dirtowatch> --localdir=<localdir> --remote=<remotename> --remotepath=<remotedir>
# call as (with rsync)
# ./syncdir.sh --rsync --plaindir=<dirtowatch> --localdir=<localdir> --user=<username> --remote=<remotename> --remotepath=<remotedir>

readonly rclone=/opt/rclone-v1.39-linux-amd64/rclone
rcloneopts="-v --stats=10s"
rsyncopts="--archive --stats --delete --progress "
readonly delay="15s"
readonly popupduration=5000
plaindir=${1}
localdir=${2}

# whether to use rclone (or rsync) (1=rclone, 0=rsync)
use_rclone=0

parseoptions() {
  for arg in $*; do
     # Split arg on "=". It is OK to have an "=" in the value, but not
     # the key.
     key=$(   echo ${arg} | cut --delimiter== --fields=1  )
     value=$( echo ${arg} | cut --delimiter== --fields=2- )

     case ${key} in
         "--rsync")
          use_rclone=0;;
         "--rclone")
          use_rclone=1;;
         "--dry-run")
           rcloneopts="${rcloneopts} --dry-run"
           rsyncopts="${rsyncopts} --dry-run";;
         "--plaindir")
           plaindir=${value};;
         "--localdir")
           localdir=${value};;
         "--remote")
           remote=${value}
          if [ $use_rclone = 1 ] && ! rclone listremotes | grep -w ${remote} ; then 
            echo "--- Unknown remote for rclone: ${remote}"
            exit 1
          fi
          ;;
         "--remotepath")
           remotepath=${value};;
         "--user")
           user=${value};;
         *)
          echo "Unrecognised option ${key}"
              exit 1
     esac
  done
}

mountgocrypt() {
  # mount localdir if necessary
  if [ ! -e "${localdir}/gocryptfs.diriv" ] ; then
    echo "============ Reverse mounting crypted dir"
    echo "Enter gocrypt password:"
    gocryptfs -reverse "${plaindir}" "${localdir}" 
  fi
}

readonly icon=/usr/share/icons/Adwaita/256x256/status/dialog-error.png
readonly ignore="\.~lock\.|\.sw.?|~$"
readonly changedfile=`mktemp --suffix=_sync`
readonly waitpidfile=`mktemp --suffix=_sync`

readonly LOCKFILE_DIR=/tmp
readonly LOCK_FD=9
readonly LOCKWAIT=120

function finish {
  echo "--- Cleaning up, killing $waitpid and removing $changedfile"
  echo
  rm -rf "${changedfile}" $waitpidfile
  kill $waitpid
}
trap finish EXIT

lock() {
  local prefix=$1
  local fd=${2:-$LOCK_FD}
  local lock_file=$LOCKFILE_DIR/$prefix.lock

  # create lock file
  eval "exec $fd>$lock_file"

  # acquier the lock
  #flock --wait $LOCKWAIT $fd \
  flock $fd \
    && return 0 \
    || return 1
}

unlock() {
  local fd=${1:-$LOCK_FD}
  flock -u $fd && return 0 || return 1
}

readchanges () {
  f="$1"
  lock changesfile || echo "Can\'t lock in readchanges()"
#  echo "LOCKED rc()"
  echo "============ NOTIFIED CHANGE FOR $f"
  echo $f >> ${changedfile}
  unlock || echo "--- Couldn\'t remove lock"
#  echo "UNLOCKED rc()"
}

startinotifyw() {
  ( inotifywait -m "${plaindir}" -r -e close_write,create,delete --format %w%f & echo $! >&3 ) 3>$waitpidfile | egrep --line-buffered -v ${ignore} | 
    while read f ; do readchanges "$f" ; done &
  waitpid=$(<$waitpidfile)
  echo "PID of inotifywait ${waitpid}"
  echo "CHANGEDFILE ${changedfile}"
}

sync () {
  local force=${1:-0} 

  lock changesfile || echo "Can\'t lock in while"
#  echo "LOCKED while()"
  if [ $force = 1 ] || [ -s ${changedfile} ] ; then
    sort ${changedfile} | uniq | while read f ; do 
      echo "============ SYNCING $f"
    done

#    echo "============ WAITING $delay"
    sleep $delay
    if [ $use_rclone = 1 ] ; then
      echo "============ CALLING RCLONE at "`date`
      ${rclone} sync ${rloneopts} "${localdir}" "${remote}:${remotepath}" && notify-send syncdir -t $popupduration "OK `date +%H:%M:%S`" || notify-send -u critical "syncdir ERR" --icon=${icon}
    else
      echo "============ CALLING RSYNC at "`date` " with options ${rsyncopts}"
      #rsync -avi --delete --progress ${localdir}/ "${user}@${remote}:${remotepath}" | grep -E '^[^.]|^$'
      rsync ${rsyncopts} ${localdir}/ "${user}@${remote}:${remotepath}" && notify-send syncdir -t $popupduration "OK `date +%H:%M:%S`" || notify-send -u critical "syncdir ERR" --icon=${icon}
    fi
    rm -f ${changedfile}
  fi
  unlock || echo "--- Couldn\'t remove lock"
#  echo "UNLOCKED while()"
}

main() {
  parseoptions $@

  mountgocrypt

  startinotifyw

  sync 1

  while true ; do
    sleep $delay

    sync

  done
}

main $@
