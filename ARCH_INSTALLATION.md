# Installation (arch linux)

## backend

install java 8 & tooling
```sh
pacman -S jdk8-openjdk jre8-openjdk mvn node npm
```

add to `.bashrc` to use Java 8
```sh
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

expected issues:
- `ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException: Could not add the entry.` crashes with root cause `Key protection  algorithm not found: java.security.UnrecoverableKeyException: Encrypt Private Key failed: unrecognized algorithm name: PBEWithSHA1AndDESede`. There is a bug in the JDK 8u292 (currently released in arch) which is fixed in 8u301 [source](https://github.com/bcgit/bc-java/issues/941#issuecomment-883767514). Arch packages are out of date and flagged appropriately [source](https://archlinux.org/packages/extra/x86_64/java8-openjdk/).
 - frontend fails (because not configured yet)

## frontend

add to `.bashrc`
```sh
export CHROME_BIN="/usr/bin/chromium"
```
(reload with `source .bashrc` if you want to keep the cmd open)

## configure IDEs

### using IntelliJ

test system is maven, which is only partially implemented in [IntelliJ](https://intellij-support.jetbrains.com/hc/en-us/community/posts/206884445-Run-Single-Test-Case-with-Maven)

configure to use Java 8
- go to `File` -> `Project Structure`
- select left `Platform Settings` -> `SDKs`
- point to `/usr/lib/jvm/java-8-openjdk`

configure to use Maven
- go to `Add configuration` (top right)
- click on plus icon
- choose `Maven`
- set the working directly
- set `install` or `test` as `Command Line`

## using NetBeans

configure to use Java 8
- right-click on `evoting` in the `Projects` window left
- select `Properties`
- go to `Build` -> `Compile`
- click on `Manage Java Platform`
- add the Java 8 platform in `/usr/lib/jvm/java-8-openjdk`
- close the subwindow again
- select the Java 8 platform

set Java 8 as default
- create `~/.netbeans/*version*/etc/netbeans.conf` for *version* the netbeans version (like `~/.netbeans/12.4/etc/netbeans.conf`)
- add `netbeans_jdkhome="/usr/lib/jvm/java-8-openjdk"`
- restart netbeans