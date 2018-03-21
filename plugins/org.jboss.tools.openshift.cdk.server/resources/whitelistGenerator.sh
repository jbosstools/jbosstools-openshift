#!/bin/sh
cat minishift.yaml | grep additionalDownloadURLs -A 3 | rev | cut -f 1 -d "/" | rev | grep -v "additionalDownload" | grep -v "\-\-" | sed 's/",//g' | awk '{ print "\"" $0 "\",";}' | grep -v "tgz" | grep -v "\.zip"

