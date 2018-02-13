#!/usr/bin/env sh

[ -z "$JAVA_XMX" ] && JAVA_XMX="256M"

JAVA_OPTS="-Xmx$JAVA_XMX -Xms$JAVA_XMX"

java $JAVA_OPTS -cp "/app/app.jar:/app/lib/*" io.hydrosphere.serving.grpc_spark.Main