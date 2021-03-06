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
readonly configdir=$HOME/.config/syncdir
LOG=${configdir}/log_syncdir1
readonly DATE='date +%Y-%m-%d_%H:%M:%S'
id="syncdir1"
method=${1}
plaindir=${2}
syncdir=${3}
url=${4}

# whether to use rclone (or rsync) (1=rclone, 0=rsync)
use_rclone=0

function echo_log {
  echo `$DATE`" $1" |tee -a ${LOG}
}
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
         "--log")
           LOG="${value}"
           ;;
         *)
          echo_log "Unrecognised option ${key}"
              exit 1
     esac
  done
}

mountgocrypt() {
  # mount syncdir if necessary
  if [ ! -e "${syncdir}/gocryptfs.diriv" ] ; then
    echo_log "============ Reverse mounting crypted dir ${syncdir}"
    echo_log "Enter gocrypt password for ${plaindir} - ${syncdir}:"
    gocryptfs -reverse -q "${plaindir}" "${syncdir}" 
  else
    echo_log "============ Crypted dir already mounted: ${syncdir}"
  fi
}

function finish {
    if [ -n ${waitpid:-''} ] ; then
    echo_log "--- Cleaning up, killing $waitpid and removing $waitpidfile and ${changedfile}"
    echo
    rm -rf "${changedfile}" $waitpidfile
    kill $waitpid
  else
    echo_log "============ Exiting"
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
  lock changesfile || echo_log "Can\'t lock in readchanges()"
  echo_log"============ NOTIFIED CHANGE FOR $f"
  echo $f >> ${changedfile}
  unlock || echo_log "--- Couldn\'t remove lock"
}

startinotifyw() {
  ( inotifywait -m "${plaindir}" -r -e close_write,create,delete --format %w%f & echo $! >&3 ) 3>$waitpidfile | egrep --line-buffered -v ${ignore} | 
    while read f ; do readchanges "$f" ; done &
  waitpid=$(<$waitpidfile)
  echo_log "PID of inotifywait ${waitpid}"
  echo_log "CHANGEDFILE ${changedfile}"
  echo_log "PIDFILE ${waitpidfile}"
}

sync () {
  local force=${1:-0} 

  lock changesfile || echo_log"Can\'t lock in while"
  if [ $force = 1 ] || [ -s ${changedfile} ] ; then
    sort ${changedfile} | uniq | while read f ; do 
      echo_log "============ SYNCING $f"
    done

    echo_log "......................................................."
    if [ $use_rclone = 1 ] ; then
      echo_log "============ CALLING rclone"
      ${rclone} sync ${rcloneopts} "${syncdir}" "${url}" 2>&1 | tee -a "${LOG}" && notify-send syncdir -t $popupduration "OK `date +%H:%M:%S` ${syncdir}" || notify-send -u critical "syncdir ERR" --icon=${icon} 2>&1 | tee -a "${LOG}"
    else
      echo_log "============ CALLING rsync at with options ${rsyncopts}"
      rsync ${rsyncopts} ${syncdir}/ "${url}" 2>&1 | tee -a "${LOG}" && notify-send syncdir -t $popupduration "OK `date +%H:%M:%S` ${plaindir}" || notify-send -u critical "syncdir ERR" --icon=${icon}
    fi
    rm -f ${changedfile}
  fi
  unlock || echo_log "--- Couldn\'t remove lock"
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

    echo_log " [$$] checking ${id} ${plaindir}"
    sync

  done
}

main $@
