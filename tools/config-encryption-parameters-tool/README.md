# Config Encryption Parameters Tool

The Config Encryption Parameter Tool runs before the actual configuration phase and
generates the ElGamal encryption parameters in a verifiable manner. Moreover, the tool
provides a list of prime numbers (which have to be members of the group) that can encode the voting options.
We refer the reader to the crypto-primitives and system specification for details on how we verifiably pick
the ElGamal encryption parameters and voting options.

## Usage

The Config Encryption Parameters Tool is packaged as a standalone SpringBoot applications
and provides a simple command-line interface with two possibilities:

1. Generate the parameters of the ElGamal encryption scheme.
2. Generate the small prime group members to encode voting options.

The application displays "help" information if you run it without any parameters.

Below you find a more detailed description of the two commands.

### Generate the parameters of the ElGamal encryption scheme

java -jar config-encryption-parameters-tool-{VERSION}.jar -genEncryptionParameters -seed_path seed.txt -seed_sig_path seed.txt.p7 -p12_path
integration.p12 -trusted_ca_path rootCA.pem -out my_output/

- The -seed_path is the path for the txt file containing the seed as a String
- The -seed_sig_path is the path to the pkcs7 signature of the seed file
- The -trusted_ca_path is the path to the trusted certificate for verifying the signature
- The -p12_path is the path to the Keystore containing the signing key
- The -out is the optional path where the tool persists the output files.

### Generate the small prime group members to encode voting options (given a set of encryption parameters)

java -jar config-encryption-parameters-tool-{VERSION}.jar -primeGroupMembers -params_path encryptionParameters{TIMESTAMP}.json.sign
-p12_path integration.p12 -trusted_ca_path rootCA.pem -out my_output/

- The -params_path is the path to the file generated in the previous step.
- The -trusted_ca_path is the path to the trusted certificate for verifying the signature.
- The -p12_path is the path to the Keystore containing the signing key
- The -out is the optional path where the tool persists the CSV and p7 output files.

## Development

```bash
mvn clean install
```
