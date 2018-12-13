
dev: clean package run

package:
	./mvnw -Dmaven.test.skip=true package

run:
	./mvnw hpi:run -Dhpi.prefix=/jenkins -Djetty.port=8090 -Djetty.httpConfig.requestHeaderSize=16384 -Djetty.httpConfig.responseHeaderSize=16384

build: clean
	./mvnw -Dmaven.test.skip=true package hpi:hpi

test:
	./mvnw test

clean:
	rm -rf ./work ./target
