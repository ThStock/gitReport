#! /bin/sh
set -eu
/etc/periodic/hourly/update &
crond
nginx
touch /srv/report.log
tail -f /srv/report.log
