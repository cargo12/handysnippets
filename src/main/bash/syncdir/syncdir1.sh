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

readonly rclone=/opt/rclone-v1.39-linux-amd64/rclone
rcloneopts="-v --stats=20s"
rsyncopts="--archive --stats --delete --progress"
readonly delay="15s"
readonly popupduration=5000
id="syncdir1"
method=${1}
plaindir=${2}
syncdir=${3}
url=${4}

# whether to use rclone (or rsync) (1=rclone, 0=rsync)
use_rclone=0

parseoptions() {
  for arg in $*; do
     # Split arg on "=". It is OK to have an "=" in the value, but not
     # the key.
     key=$(   echo ${arg} | cut --delimiter== --fields=1  )
     value=$( echo ${arg} | cut --delimiter== --fields=2- )

     case ${key} in
         "--id")
           id="${value}"
           ;;
         "--method")
           if [ "${value}" = "rclone" ] ; then
            use_rclone=1
          fi
          ;;
         "--dry-run")
           rcloneopts="${rcloneopts} --dry-run"
           rsyncopts="${rsyncopts} --dry-run";;
         "--plaindir")
           plaindir=${value};;
         "--syncdir")
           syncdir=${value}
           ;;
         "--url")
           url=${value}
          ;;
         *)
          echo `date +"%Y-%m-%d %H:%M:%S"` "Unrecognised option ${key}"
              exit 1
     esac
  done
}

mountgocrypt() {
  # mount syncdir if necessary
  if [ ! -e "${syncdir}/gocryptfs.diriv" ] ; then
    echo `date +"%Y-%m-%d %H:%M:%S"` "============ Reverse mounting crypted dir ${syncdir}"
    echo `date +"%Y-%m-%d %H:%M:%S"` "Enter gocrypt password for ${plaindir} - ${syncdir}:"
    gocryptfs -reverse -q "${plaindir}" "${syncdir}" 
  else
    echo `date +"%Y-%m-%d %H:%M:%S"` "============ Crypted dir already mounted: ${syncdir}"
  fi
}

function finish {
    if [ -n ${waitpid:-''} ] ; then
    echo `date +"%Y-%m-%d %H:%M:%S"` "--- Cleaning up, killing $waitpid and removing $waitpidfile and ${changedfile}"
    echo
    rm -rf "${changedfile}" $waitpidfile
    kill $waitpid
  else
    echo `date +"%Y-%m-%d %H:%M:%S"` "============ Exiting"
  fi
}
trap finish EXIT #SIGINT SIGTERM

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
  lock changesfile || echo `date +"%Y-%m-%d %H:%M:%S"` "Can\'t lock in readchanges()"
  echo "============ NOTIFIED CHANGE FOR $f"
  echo $f >> ${changedfile}
  unlock || echo `date +"%Y-%m-%d %H:%M:%S"` "--- Couldn\'t remove lock"
}

startinotifyw() {
  ( inotifywait -m "${plaindir}" -r -e close_write,create,delete --format %w%f & echo $! >&3 ) 3>$waitpidfile | egrep --line-buffered -v ${ignore} | 
    while read f ; do readchanges "$f" ; done &
  waitpid=$(<$waitpidfile)
  echo `date +"%Y-%m-%d %H:%M:%S"` "PID of inotifywait ${waitpid}"
  echo `date +"%Y-%m-%d %H:%M:%S"` "CHANGEDFILE ${changedfile}"
  echo `date +"%Y-%m-%d %H:%M:%S"` "PIDFILE ${waitpidfile}"
}

sync () {
  local force=${1:-0} 

  lock changesfile || echo "Can\'t lock in while"
  if [ $force = 1 ] || [ -s ${changedfile} ] ; then
    sort ${changedfile} | uniq | while read f ; do 
      echo `date +"%Y-%m-%d %H:%M:%S"` "============ SYNCING $f"
    done

    echo `date +"%Y-%m-%d %H:%M:%S"` "......................................................."
    if [ $use_rclone = 1 ] ; then
      echo `date +"%Y-%m-%d %H:%M:%S"` "============ CALLING rclone at "`date`
      ${rclone} sync ${rcloneopts} "${syncdir}" "${url}" 2>&1 && notify-send syncdir -t $popupduration "OK `date +%H:%M:%S` ${syncdir}" || notify-send -u critical "syncdir ERR" --icon=${icon}
    else
      echo `date +"%Y-%m-%d %H:%M:%S"` "============ CALLING rsync at "`date` " with options ${rsyncopts}"
      #rsync -avi --delete --progress ${syncdir}/ "${user}@${remote}:${remotepath}" | grep -E '^[^.]|^$'
      rsync ${rsyncopts} ${syncdir}/ "${url}" && notify-send syncdir -t $popupduration "OK `date +%H:%M:%S` ${plaindir}" || notify-send -u critical "syncdir ERR" --icon=${icon}
    fi
    rm -f ${changedfile}
  fi
  unlock || echo `date +"%Y-%m-%d %H:%M:%S"` "--- Couldn\'t remove lock"
}

main() {
  parseoptions $@

  readonly icon=/usr/share/icons/Adwaita/256x256/status/dialog-error.png
  readonly ignore="\.~lock\.|\.sw.?|~$"
  readonly changedfile=`mktemp --suffix=_${id}_sync`
  readonly waitpidfile=`mktemp --suffix=_${id}_sync`

  readonly LOCKFILE_DIR=/tmp
  readonly LOCK_FD=9
  readonly LOCKWAIT=120

  mountgocrypt

  startinotifyw

  sync 1

  while true ; do
    sleep $delay

    echo `date +"%Y-%m-%d %H:%M:%S"` " [$$] checking ${id} ${plaindir}"
    sync

  done
}

main $@
