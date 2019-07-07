#!/bin/bash
set -ex

./gradlew bootRun &
sleep 15s
git clone https://github.com/VergiliusProject/kernels-data.git

for f in kernels-data/yml/*.yml
do
    echo importing $f
    #curl -s -F "file=@$f" http://localhost:8080/admin
done

(while true; do echo '[sending keep alive]'; sleep 60s; done;) &
#wget -q --mirror --page-requisites -E http://localhost:8080/
wget -q --mirror http://localhost:8080/BingSiteAuth.xml
wget -q --mirror http://localhost:8080/.nojekyll
wget -q --mirror http://localhost:8080/CNAME

set +e
wget -q --mirror --page-requisites --content-on-error -E http://localhost:8080/404
jobs -p | xargs kill -9