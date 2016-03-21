* Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.


1. Contents of this directory
=============================

This directory is used by default to store the Java keystore files that 
hold the certificates that are used for signing and encrypting messages.

It contains three keystores:

1) "publickeys.jks" holding the public keys. These are used to validate
   the signature of a received message and to encrypt sent messages. 
   This keystore should be used to store the certificates of trading
   partners.

2) "privatekeys.jks" holding the private keys. These are used to sign
   sent messages and decrypt received messages.

3) “trustedcerts.jks” holding the certificates of trusted Certificate
   Authorities. These are used to validate the certificate of a 
   signature. If the certificate of the trading partner is included in
   the message it does not need to be in the public keystore if the 
   issuing CA is in this keystore.

The distribution package by default includes empty keystores, with simple
passwords "secret" for the private, "nosecrets" for the public and 
“trusted” for the trusted keystore. It is HIGHLY RECOMMENDED to change 
these passwords to more safer ones, see below how to configure Holodeck 
B2B for the new passwords.

2. Configuring Holodeck B2B
===========================

To give Holodeck B2B access to the keystores you need to configure the 
keystore passwords in the Holodeck B2B configuration file which is found 
in «Holodeck B2B base dir»/conf/holodeckb2b.xml
Set "PrivateKeyStorePassword”, "PublicKeyStorePassword" and 
“TrustStorePassword" parameters to the passwords of the respective
keystores. 

NOTE: If you want the change the passwords for the default keystores you
must also change the password on the keystore files by executing the
following command: 
    keytool -storepasswd -keystore «path to keystore»

Also the location where Holodeck B2B should look for the keystores can be 
specified by setting the "PrivateKeyStorePath”, "PublicKeyStorePath"
or “TrustStorePath"
parameters. 

3. Adding certificates and private keys
=======================================

Each certificate or private key that is added to a keystore is assigned an
id, called the alias. This alias is used in the P-Mode to identify the 
certificate / key that must be used for processing the message. It is 
RECOMMENDED to use descriptive aliases for easy identification.

To add a X.509v3 certificate holding the public key of a trading
partner to the public keystore or a certificate of a trusted CA use the
following command:

keytool -importcert \
        -keystore «path to the keystore» 
        -storepass «your keystore password» \
        -alias «alias for the cert in keystore» \
        -file «path to certificate file» 

To add a PKCS#12 formatted certificate holding the private of a 
trading partner to the private keystore use the following command:

keytool -importkeystore -srcstoretype PKCS12 \
        -srckeystore «path to certificate file» \
        -srcalias «the name of the certificate in the PKCS#12 file» \
        -srcstorepass «the password to access the PKCS#12 file» \
        -destkeystore «Holodeck B2B base dir»/repository/certs/privatekeys.jks \
        -deststorepass «your keystore password» \
        -destalias «alias for cert in keystore» \ 
        -destkeypass «the password to set on the new entry in the keystore»

NOTE: Use the following command to list the certificates in the PKCS#12 
file and show their names / aliases:
keytool -list -v -storetype pkcs12 -keystore «path to certificate file»

4. Examples
===========

The examples/certs directory contains three sample keystores which contain
the certificates that are used in the example P-Modes (contained in 
examples/pmodes). Their passwords are the same as the default keystores.
You can therefor just overwrite the default keystores with the example
keystores.

When using the private key in a P-Mode the password for is 
"Example" + 'A' | 'B' | 'C' | 'D'


