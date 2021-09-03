# Installation (arch linux)

install java 8 & tooling
```sh
pacman -S jdk8-openjdk jre8-openjdk mvn node npm
```

configure IntelliJ to use Java 8
- go to `File` -> `Project Structure`
- select left `Platform Settings` -> `SDKs`
- point to `/usr/lib/jvm/java-8-openjdk`

add to `.bashrc` to use Java 8
```
# Java 8 (for E-Voting)
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk"
export PATH="/usr/lib/jvm/java-8-openjdk/bin:$PATH"
```
(reload with `source .bashrc` if you want to keep the cmd open)

install the [iaikPkcs11Wrapper](https://jce.iaik.tugraz.at/products/core-crypto-toolkits/pkcs11-wrapper/)
```sh
cd maven-artifacts/iaikPkcs11Wrapper_1.6.2/bin/ # or your own download location
mvn install:install-file -Dfile=iaikPkcs11Wrapper_1.6.2.jar -DgroupId=iaik -DartifactId=iaikPkcs11Wrapper -Dversion=1.6.2 -Dpackaging=jar 
```

clone & build crypto-primites
```sh
git clone git@gitlab.com:swisspost-evoting/crypto-primitives/crypto-primitives.git
cd crypto-primitives && mvn clean install -DskipTests && cd ..
```

build main system
```sh
mvn clean install -DskipTests
```
