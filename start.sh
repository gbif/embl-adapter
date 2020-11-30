mvn clean package

cp src/main/resources/eml.xml target/

java -jar target/embl-adapter.jar embl-adapter --log-config src/main/resources/logback.xml --conf src/main/resources/config.yaml
