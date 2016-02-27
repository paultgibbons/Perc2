#!/usr/bin/env bash

if which wget >/dev/null; then
    echo "wget exists"
else
    echo "wget not installed, exiting"
    exit
fi

if which mvn >/dev/null; then
    echo "mvn exists"
else
    echo "maven not installed, exiting"
    exit
fi


mkdir tmp
if [ ! -d data ]; then
    mkdir data
fi

if [ ! -d data/train ]; then
    mkdir data/train
fi



cd tmp
# Download data
if [ ! -d ../data/train/ace_tides_multling_train ]; then
    wget --no-check-certificate https://cogcomp.cs.illinois.edu/member_pages/sammons/tmp/ACE-2004.zip .
    unzip ACE-2004.zip
    mv ace_tides_multling_train ../data/train/ace_tides_multling_train
fi

if [ ! -d ../data/train/ACE05_English ]; then
    wget --no-check-certificate https://cogcomp.cs.illinois.edu/member_pages/sammons/tmp/ACE-2005-English.zip . 
    unzip ACE-2005-English.zip
    mv ACE05_English ../data/train/ACE05_English
    ### The below cp command is to ensure that the reader can read bn docs correctly.
    mv ../data/train/ACE05_English/bc/timex2norm/* ../data/train/ACE05_English/bc/.
    mv ../data/train/ACE05_English/bn/timex2norm/apf.v5.1.1.dtd ../data/train/ACE05_English/bn/.
    mv ../data/train/ACE05_English/cts/timex2norm/* ../data/train/ACE05_English/cts/.
    mv ../data/train/ACE05_English/nw/timex2norm/* ../data/train/ACE05_English/nw/.
    mv ../data/train/ACE05_English/un/timex2norm/* ../data/train/ACE05_English/un/.
    mv ../data/train/ACE05_English/wl/timex2norm/* ../data/train/ACE05_English/wl/.
fi

# Download reader and do MVN install.
# Note the one on remote Cogcomp maven is not up to date, and can't be used in our project. 
# We have to manually download and install them.

# Once you have illinois-ace-reader installed, you may need to restart your IDE, 
# or re-import mvn project to refresh its cache to use the new version.

if [ ! -d illinois-ace-reader ]; then
    wget --no-check-certificate https://cogcomp.cs.illinois.edu/member_pages/sammons/tmp/illinois-ace-reader.zip .
    unzip illinois-ace-reader.zip
    cd illinois-ace-reader
## Install ace reader.
    mvn install -DskipTests
fi