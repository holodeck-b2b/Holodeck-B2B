* Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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

It should contain two keystores:

1) "publickeys.jks" holding the public keys. These are used to validate
   the signature of a received message and to encrypt sent messages.

2) "privatekeys.jks" holding the private keys. These are used to sign
   sent messages and decrypt received messages.

The delivery package does not include keystores, you must create them
when you start using the security features. The keystores are 
automatically created when you add the first certificate (see below).


2. Adding certificates
======================

To add a X.509v3 certificate holding the public key of a trading
partner to the public keystore use the following command:

keytool -importcert \
        -keystore «Holodeck B2B base dir»/repository/certs/publickeys.jks \
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


3. Configuring Holodeck B2B
===========================

To give Holodeck B2B access to the keystores you need to configure the 
keystore passwords in the Holodeck B2B configuration file which is found 
in «Holodeck B2B base dir»/conf/holodeckb2b.xml
Set "PrivateKeyStorePassword" and "PublicKeyStorePassword" parameters to
the passwords of the respective keystores. 
Also the location where Holodeck B2B should look for the keystores can be 
specified by setting the "PrivateKeyStorePath" and "PublicKeyStorePath"
parameters. 


4. Examples
===========

The examples/certs directory contains two sample keystores which contain
the certificates that are used in the example P-Modes (contained in 
examples/pmodes). Their passwords are "secrets" for the private keystore 
and "nosecrets" for the public one.

When using the private key certificates in a P-Mode the password for
a certificate is "Example" + 'A' | 'B' | 'C' | 'D'


