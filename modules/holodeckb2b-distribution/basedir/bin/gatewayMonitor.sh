#!/bin/sh
# -----------------------------------------------------------------------------
# Startup script for the local Holobeck B2B command line monitoring tool
#
#   AXIS2_HOME   MAY point at the Holodeck B2B home directory
#
#   JAVA_HOME    MUST point at your Java Runtime Environment installation.
#
# -----------------------------------------------------------------------------

# Get the context and from that find the location of setenv.sh
. `dirname $0`/setenv.sh > /dev/null

exec "$JAVA_HOME/bin/java" $JAVA_OPTS -classpath "$HB2B_CP" org.holodeckb2b.ui.app.cli.HB2BInfoTool $*
