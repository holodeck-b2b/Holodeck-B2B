#!/bin/sh

# ----------------------------------------------------------------------------
# Holodeck B2B Server start script
#
# Environment Variable Prequisites
#
#   H2B_HOME   Home of the Holodeck B2B installation. If not set I will try
#              to figure it out.
#
#   JAVA_HOME  Must point at your Java Runtime Environment
#
# -----------------------------------------------------------------------------

# Get the context and from that find the location of setenv.sh
. `dirname $0`/setenv.sh

JAVA_OPTS=""
while [ $# -ge 1 ]; do
    case $1 in
        -xdebug)
            JAVA_OPTS="$JAVA_OPTS -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8000"
            shift
        ;;
        -security)
            JAVA_OPTS="$JAVA_OPTS -Djava.security.manager -Djava.security.policy=$HB2B_HOME/conf/axis2.policy"
            shift
        ;;
        -h)
            echo "Usage: startServer.sh"
            echo "commands:"
            echo "  -xdebug    Start Holodeck B2B Server under JPDA debugger"
            echo "  -security  Enable Java 2 security"
            echo "  -h         help"
            shift
            exit 0
        ;;
        *)
            echo "Error: unknown command:$1"
            echo "For help: startServer.sh -h"
            shift
            exit 1
    esac
done

cd "$HB2B_HOME"

[ ! -d ./logs ] && mkdir logs

exec "$JAVA_HOME/bin/java" $JAVA_OPTS -classpath "$HB2B_CP" \
    -Dderby.stream.error.file="$HB2B_HOME/logs/derby.log" \
    org.holodeckb2b.core.HolodeckB2BServer \
    -home "$HB2B_HOME"
