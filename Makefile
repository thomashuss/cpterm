all: build

build:
	which mvn node npm > /dev/null
	$(MAKE) -C host all
	$(MAKE) -C extension all
	mkdir -p target
	mv host/target/*.jar extension/dist/*.zip target
