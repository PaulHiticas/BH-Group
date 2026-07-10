#!/bin/sh
set -e

# The uploads directory is a Docker named volume, mounted fresh by the
# container runtime (owned by root) *after* the image is built — a
# Dockerfile-time `chown` never applies to it. Fix ownership here, at
# container start, before dropping to the unprivileged app user.
if [ -n "$UPLOAD_DIR" ]; then
  mkdir -p "$UPLOAD_DIR"
  chown -R bhgroup:bhgroup "$UPLOAD_DIR"
fi

exec su-exec bhgroup java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar
