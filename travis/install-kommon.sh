#!/bin/bash
cd /tmp
rm -rf /tmp/kommon
git clone https://github.com/andrewoma/kommon.git
cd kommon
./gradlew install