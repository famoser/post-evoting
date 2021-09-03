CC = cc
SOURCE_DIR = ../../common/src/
INCLUDE_DIR = ../../common/include/
PLATFORM_SRC_INCLUDE = ../src/
PLATFORM_LIBRARIES = lib/
DEBUG_OUTPUT_DIR = debug/
RELEASE_OUTPUT_DIR = release/
TARGETS = debug release

all: $(TARGETS)

.PHONY: init
init:
	find ../../ \( -name '*.h' -o -name '*.c' \) -exec dos2unix {} {} \;

# we do not need '-fpack-struct' as option to get byte aligned structure members as required by PKCS#11,
# all PKCS#11 modules seem to be compiled without this option

.PHONY: debug
debug: init $(SOURCE_DIR)/pkcs11wrapper.c $(INCLUDE_DIR)/pkcs11wrapper.h
	mkdir -p $(DEBUG_OUTPUT_DIR)
	$(CC) -G -I $(PLATFORM_SRC_INCLUDE) -I $(INCLUDE_DIR) -DUNIX -DDEBUG -m64 -g -o $(DEBUG_OUTPUT_DIR)libpkcs11wrapper.so $(SOURCE_DIR)pkcs11wrapper.c -shared -fPIC -L $(PLATFORM_LIBRARIES)

.PHONY: release
release: init $(SOURCE_DIR)/pkcs11wrapper.c $(INCLUDE_DIR)/pkcs11wrapper.h
	mkdir -p $(RELEASE_OUTPUT_DIR)
	$(CC) -G -I $(PLATFORM_SRC_INCLUDE) -I $(INCLUDE_DIR) -DUNIX -m64 -o $(RELEASE_OUTPUT_DIR)libpkcs11wrapper.so $(SOURCE_DIR)pkcs11wrapper.c -shared -fPIC -L $(PLATFORM_LIBRARIES)

clean:
	rm -f $(DEBUG_OUTPUT_DIR)* $(RELEASE_OUTPUT_DIR)*
