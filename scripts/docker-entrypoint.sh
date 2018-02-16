#!/bin/bash
#
# Docker entrypoint script for rocksdbjava Docker container
# Based off of init scripts from the JPackage Project <http://www.jpackage.org/>

# Fail if any command in a pipeline fails
set -o pipefail

# Source functions library
_prefer_jre="true"
. /usr/share/java-utils/java-functions

# Helper functions
function join_by() {
  local IFS="$1"
  shift
  echo "$*"
} # join_by

# Expand classpath parameter
if [[ -n "${CLASSPATH}" ]]; then
  NEW_CP=""
  IFS=':' read -ra CP <<< "$CLASSPATH"
  for i in "${CP[@]}"; do
    if EXPANDED=($i); then
      NEW_CP="$NEW_CP:$(join_by ':' ${EXPANDED[@]})"
    else
      NEW_CP="$NEW_CP:$i"
    fi
  done
  CLASSPATH="$NEW_CP"
fi

# Configuration
BASE_JARS="rocksdbjni"

# Set parameters
set_jvm
set_classpath $BASE_JARS
JNI_CLASSPATH="${CLASSPATH}"

# Search for a classpath argument and add the rocksdbjni jar
# with the classpath specified
args=()
prev_cp='false'
for arg in "${@}"; do
  if [[ "${prev_cp}" == "true" ]]; then
    CLASSPATH="${arg}:${JNI_CLASSPATH}"
    prev_cp='false'
  elif [[ "${arg}" == "-cp" ]] || [[ "${arg}" == "-classpath" ]]; then
    prev_cp='true'
  else
    args+=("$arg")
  fi
done

# If the command is a java command, inject the classpath argument
if [[ "${args[0]}" == java* ]]; then
  args=("${args[0]}" -classpath "${CLASSPATH}" "${args[@]:1}")
fi

# Let's start
echo "[DEBUG] Running command: exec \"${args[@]}\""
exec "${args[@]}"
