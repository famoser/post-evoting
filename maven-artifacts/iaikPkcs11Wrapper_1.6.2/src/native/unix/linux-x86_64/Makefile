CC = gcc
SOURCE_DIR = ../../common/src/
INCLUDE_DIR = ../../common/include/
PLATFORM_SRC_INCLUDE = ../src/
DEBUG_OUTPUT_DIR = debug/
RELEASE_OUTPUT_DIR = release/
TARGETS = debug release

all : $(TARGETS)

VPATH = $(SOURCE_DIR) $(INCLUDE_DIR)

# we do not need '-fpack-struct' as option to get byte aligned structure members as required by PKCS#11,
# all PKCS#11 modules seem to be compiled without this option

.PHONY	: debug
debug : pkcs11wrapper.c pkcs11wrapper.h
	mkdir -p $(DEBUG_OUTPUT_DIR)
	$(CC) -fPIC -I $(PLATFORM_SRC_INCLUDE) -I $(INCLUDE_DIR) -DUNIX -DDEBUG -Wall -std=c11 -m64 -g -o $(DEBUG_OUTPUT_DIR)libpkcs11wrapper.so $(SOURCE_DIR)pkcs11wrapper.c -shared

.PHONY	: release
release : pkcs11wrapper.c pkcs11wrapper.h
	mkdir -p $(RELEASE_OUTPUT_DIR)
	$(CC) -fPIC -I $(PLATFORM_SRC_INCLUDE) -I $(INCLUDE_DIR) -DUNIX -Wall -std=c11 -m64 -o $(RELEASE_OUTPUT_DIR)libpkcs11wrapper.so $(SOURCE_DIR)pkcs11wrapper.c -shared

clean :
	rm -f $(DEBUG_OUTPUT_DIR)* $(RELEASE_OUTPUT_DIR)*
