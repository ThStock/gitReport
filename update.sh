#! /bin/sh
set -eu
LANG=en_US.UTF-8
cd /srv
date &> /srv/report.log
java -jar /srv/git-report.jar /srv/src &> /srv/report.log
