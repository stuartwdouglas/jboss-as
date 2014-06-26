#!/bin/sh
DEL="core-model-test io network core-build cli  system-jmx model-test subsystem-test server management-client-content security-manager ide-configs host-controller logging controller-client controller log-ids.txt build-config webservices process-controller build-plugin domain-management protocol domain-http remoting patching core-security deployment-repository testsuite-core jmx threads version platform-mbean deployment-scanner"
rm -r $DEL


#remove the modules
for i in $DEL; do
    sed -i '' "s/ *<module>$i<\/module>//" pom.xml
    sed -i  '' "s/ *<module>$i\/.*<\/module>//" pom.xml
done

for i in remainder-split-patches/*.diff; do
    patch -p1 <$i
done

CORE_ARTIFACTS=`perl -ne 'if ($p) { print; $p = 0 } $p++ if />org.wildfly.core/' pom.xml | cut -d '>' -f 2 | cut -d '<' -f 1`

for file in `find . -name pom.xml`; do
    echo $file
    if [ $file !=  './pom.xml' ]; then
        for i in $CORE_ARTIFACTS; do
            perl -i -pe "BEGIN{undef $/;} s/<groupId>org.wildfly<\/groupId>\s*\n\s*<artifactId>$i<\/artifactId>/<groupId>org.wildfly.core<\/groupId>\n            <artifactId>$i<\/artifactId>/smg" $file
        done
    fi
done


