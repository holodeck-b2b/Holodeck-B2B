#!/bin/sh

# ------------------------------------------------------------------------------
# This will set HB2B_HOME and HB2B_CP environment variables properly so the
# Holodeck B2B server or monitoring tools can be started. The HB2B_HOME variable
# may already be set before running the script.
# 
# The JAVA_HOME environment variable MUST already be set and point at the 
# Java Runtime Environment that should be used to start the server.
#
# NOTE: Borrowed generously from Apache Axis2 startup scripts.
# -----------------------------------------------------------------------------

# if JAVA_HOME is not set we're not happy
if [ -z "$JAVA_HOME" ]; then
  echo "You must set the JAVA_HOME variable before starting the Holodeck B2B Server."
  exit 1
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
os400=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set HB2B_HOME if not already set
[ -z "$HB2B_HOME" ] && HB2B_HOME=`cd "$PRGDIR/.." ; pwd`

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$HB2B_HOME" ] && HB2B_HOME=`cygpath --unix "$HB2B_HOME"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# For OS400
if $os400; then
  # Set job priority to standard for interactive (interactive - 6) by using
  # the interactive priority - 6, the helper threads that respond to requests
  # will be running at the same priority as interactive jobs.
  COMMAND='chgjob job('$JOBNAME') runpty(6)'
  system $COMMAND

  # Enable multi threading
  QIBM_MULTI_THREADED=Y
  export QIBM_MULTI_THREADED
fi

# update classpath
HB2B_CP=""
for f in "$HB2B_HOME"/lib/*.jar
do
  HB2B_CP="$HB2B_CP":$f
done
HB2B_CP="$HB2B_HOME":"$HB2B_HOME/conf":"$JAVA_HOME/lib/tools.jar":"$HB2B_CP":"$CLASSPATH"

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  HB2B_HOME=`cygpath --absolute --windows "$HB2B_HOME"`
  HB2B_CP=`cygpath --path --windows "$HB2B_CP"`
fi

export HB2B_HOME
export JAVA_HOME
export HB2B_CP

echo " Using HB2B_HOME: $HB2B_HOME"
echo " Using JAVA_HOME: $JAVA_HOME"



