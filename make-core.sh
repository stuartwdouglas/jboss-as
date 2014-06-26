#!/bin/sh
DEL="jaxrs embedded client spec-api naming mod_cluster batch xts messaging integration-tests.bat clustering rts security sar weld mail connector undertow ejb3 web-build ee jpa dist jsf jsr77 web-common legacy jdr picketlink appclient arquillian transactions pojo build jacorb integration-tests.sh webservices testsuite tools build.sh build.bat security-manager "
rm -r $DEL
#tools pom.xml testsuite build.bat build.sh	zanata.xml



for i in core-split-patches/*; do
    patch -p1 <$i
done


find . -name pom.xml -exec sed -i '' s/9.0.0.Alpha1-SNAPSHOT/1.0.0.Alpha1-SNAPSHOT/ {} \;
find . -name pom.xml -exec sed -i '' 's/<groupId>org.wildfly<\/groupId>/<groupId>org.wildfly.core<\/groupId>/' {} \;
find . -name pom.xml -exec sed -i '' 's/<artifactId>wildfly-parent<\/artifactId>/<artifactId>wildfly-core-parent<\/artifactId>/' {} \;
find . -name module.xml -exec sed -i '' 's/\${org.wildfly:/\${org.wildfly.core:/' {} \;



#remove the modules
for i in $DEL; do
    sed -i '' "s/ *<module>$i<\/module>//" pom.xml
    sed -i  '' "s/ *<module>$i\/.*<\/module>//" pom.xml
done





