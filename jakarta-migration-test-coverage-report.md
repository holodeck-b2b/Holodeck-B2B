# Test Coverage Rapport for Jakarta Migration

Denne rapport analyserer testdækningen i Holodeck B2B med fokus på de områder der påvirkes af javax til jakarta migrationen.

---

## Opsummering

| Modul | Kildefiler | Test Klasser | Test Metoder | Dækning |
|-------|------------|--------------|--------------|---------|
| holodeckb2b-default-mds | 18 JPA entities | 9 | 58 | ✅ GOD |
| holodeckb2b-certmanager | 2 | 0 | 0 | ❌ **KRITISK MANGLER** |
| holodeckb2b-as4secprovider | 25 | 3 | 21 | ⚠️ SVAG |
| holodeckb2b-ebms3as4 | 3 (migrations-kritiske) | 49 | 142 | ✅ MODERAT |
| holodeckb2b-core | Mange | 30 | 166 | ✅ MODERAT |

**Samlet migreringsrisiko: MODERAT-HØJ**

---

## Detaljeret Analyse per Modul

### 1. holodeckb2b-default-mds (JPA/Hibernate)

**Påvirkning**: 18 JPA entity klasser skal ændre `javax.persistence` → `jakarta.persistence`

#### ✅ Godt Dækket

| Test Klasse | Antal Tests | Dækker |
|-------------|-------------|--------|
| QueryTests | 9 | Komplekse queries, filtrering, pagination |
| StoreTests | ~10 | Entity persistence operationer |
| DeleteTests | ~5 | Sletning og cascade operationer |
| UpdateTests | ~5 | Update operationer |
| MessageUnitTest | ~8 | Basis entity funktionalitet |
| ReceiptTest | ~5 | Receipt entity |
| ServiceTest | ~5 | Service embeddable |
| AgreementReferenceTest | ~3 | Agreement reference |
| DescriptionTest | ~3 | Description embeddable |

**Test Infrastruktur:**
- `EntityManagerUtil` - Helper til EntityManager operationer
- `TestDataSet` - Genererer testdata (11 message units)
- Wrapper klasser til embeddable entities

#### ⚠️ Potentielle Huller
- Ingen edge case tests for komplekse queries
- Ingen performance tests
- Mangler tests for Hibernate 6.x specifikke ændringer (unified dialects)

**Migrationsrisiko: LAV-MEDIUM**

---

### 2. holodeckb2b-certmanager (JAXB)

**Påvirkning**: `DefaultCertManager.java` bruger JAXB til config parsing

#### ❌ KRITISK: Ingen Tests

**Filer uden testdækning:**
- `DefaultCertManager.java` (696 linjer) - **INGEN TESTS**
- JAXB XML unmarshalling - **IKKE TESTET**
- Certifikat loading fra keystores - **IKKE TESTET**
- Trust validation - **IKKE TESTET**

**Kritisk utestet kode:**
```java
JAXBContext jaxbContext = JAXBContext.newInstance("org.holodeckb2b.security.trust.config");
Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
JAXBElement<CertManagerConfigurationType> rootConfigElement =
    jaxbUnmarshaller.unmarshal(new StreamSource(fis), CertManagerConfigurationType.class);
```

**Nødvendige tests før migration:**
1. XML config fil parsing
2. Keystore loading
3. Password retrieval fra system properties
4. Trust chain validation
5. Revocation checking
6. Fejlhåndtering ved manglende/ugyldig config

**Migrationsrisiko: MEGET HØJ**

---

### 3. holodeckb2b-as4secprovider (WSS4J Security)

**Påvirkning**: WSS4J 3.0.4 → 4.0.0, `javax.activation` → `jakarta.activation`

#### ⚠️ Svag Dækning (21 tests i 3 klasser)

**Testet:**
| Test Klasse | Tests | Dækker |
|-------------|-------|--------|
| AttachmentCallbackHandlerTest | 5 | Attachment request/result callbacks |
| PasswordCallbackHandlerTest | ~8 | Password callbacks |
| PModeValidatorTest | ~8 | PMode validation |

**IKKE Testet (Kritisk):**

| Klasse | Linjer | Status |
|--------|--------|--------|
| SignatureProcessor.java | 200+ | ❌ INGEN TESTS |
| SecurityHeaderCreator.java | 300+ | ❌ INGEN TESTS |
| SecurityHeaderProcessor.java | 250+ | ❌ INGEN TESTS |
| CertManWSS4JCrypto.java | 150+ | ❌ INGEN TESTS |
| ExtEncryptedKeyProcessor.java | 100+ | ❌ INGEN TESTS |
| SignatureTrustValidator.java | 100+ | ❌ INGEN TESTS |
| EncryptionAction.java | 80+ | ❌ INGEN TESTS |
| DecryptionAction.java | 80+ | ❌ INGEN TESTS |

**Nødvendige tests før migration:**
1. Signatur oprettelse og verifikation
2. Kryptering og dekryptering
3. WS-Security header processing
4. Certificate trust validation
5. WSS4J 4.0.0 API kompatibilitet

**Migrationsrisiko: MEGET HØJ**

---

### 4. holodeckb2b-ebms3as4 (Activation API)

**Påvirkning**: 3 filer bruger `javax.activation.DataHandler`

#### ✅ Moderat Dækning (142 tests i 49 klasser)

**Testet:**
| Test Klasse | Tests | Dækker |
|-------------|-------|--------|
| SaveUserMsgAttachmentsTest | 2 | Attachment saving fra DataHandler |
| CompressionHandlerTest | 1 | Komprimering med DataHandler |
| DecompressionHandlerTest | ~3 | Dekomprimering |
| GZIPCompressingInputStreamTest | ~5 | GZIP stream operationer |

**Filer med activation imports:**
- `SaveUserMsgAttachments.java` - ✅ Testet
- `CompressionHandler.java` - ✅ Testet
- `CompressionDataHandler.java` - ✅ Indirekte testet

**Potentielle huller:**
- Edge cases for kompression/dekompression fejl
- MIME type håndtering
- DataHandler lifecycle

**Migrationsrisiko: LAV-MEDIUM**

---

### 5. holodeckb2b-core (Axis2 Integration)

**Påvirkning**: Axis2 1.8.2 → 2.0.0 (HTTPClient 4.x → 5.x)

#### ✅ Moderat-God Dækning (166 tests i 30 klasser)

**Testet:**
- Handler tests (inflow/outflow)
- Message processing context
- Delivery manager
- Workers og tasks
- Event handling
- PMode validation

**Ikke direkte testet:**
- HTTP transport konfiguration
- HTTPClient 5.x specifikke features
- Multi-hop scenarios

**Migrationsrisiko: LAV**

---

## Prioriteret Liste over Manglende Tests

### Prioritet 1: KRITISK (Skal laves før migration)

#### A. DefaultCertManager Tests (certmanager)
```
Nødvendige tests:
- testLoadValidConfig()
- testLoadInvalidConfig()
- testMissingConfigFile()
- testKeystoreLoading()
- testKeystorePasswordFromEnv()
- testKeystorePasswordFromSystemProp()
- testTrustChainValidation()
- testRevocationChecking()
- testDirectTrustConfig()
- testCertificateExpiry()
```
**Estimat: 10-15 tests**

#### B. Security Processor Tests (as4secprovider)
```
Nødvendige tests for SignatureProcessor:
- testCreateSignature()
- testVerifyValidSignature()
- testVerifyInvalidSignature()
- testSignatureWithAttachments()
- testSignatureAlgorithms()

Nødvendige tests for SecurityHeaderCreator/Processor:
- testCreateSecurityHeader()
- testProcessSecurityHeader()
- testHeaderWithSignature()
- testHeaderWithEncryption()
- testHeaderWithBothSignatureAndEncryption()
```
**Estimat: 15-20 tests**

### Prioritet 2: HØJ (Anbefales før migration)

#### C. Encryption/Decryption Tests
```
- testEncryptPayload()
- testDecryptPayload()
- testEncryptionKeyManagement()
- testSymmetricEncryption()
- testAsymmetricEncryption()
```
**Estimat: 8-10 tests**

#### D. Integration Tests
```
- testEndToEndMessageWithSecurity()
- testMessageExchangeWithSignature()
- testMessageExchangeWithEncryption()
```
**Estimat: 5-8 tests**

### Prioritet 3: MEDIUM (Nice to have)

#### E. Hibernate 6.x Kompatibilitet
```
- testDialectCompatibility()
- testQuerySyntaxChanges()
- testTransactionHandling()
```
**Estimat: 5 tests**

---

## Anbefalinger

### Før Migration Starter

1. **Opret minimum 25-35 nye tests:**
   - 10-15 for certmanager (JAXB)
   - 15-20 for as4secprovider (WSS4J/Security)

2. **Kør eksisterende tests med nye dependencies:**
   ```bash
   # Verificer at eksisterende tests stadig virker
   mvn test -pl modules/holodeckb2b-default-mds
   mvn test -pl modules/holodeckb2b-ebms3as4
   mvn test -pl modules/holodeckb2b-core
   ```

3. **Opret integrations test suite:**
   - Aktiver `holodeckb2b-it` modulet
   - Tilføj end-to-end message flow tests

### Under Migration

1. **Kør tests efter hver dependency opdatering:**
   ```bash
   # Efter Hibernate opdatering
   mvn test -pl modules/holodeckb2b-default-mds

   # Efter WSS4J opdatering
   mvn test -pl modules/holodeckb2b-as4secprovider

   # Efter Axis2 opdatering
   mvn test -pl modules/holodeckb2b-core
   ```

2. **Monitor for:**
   - ClassNotFoundException (manglende jakarta klasser)
   - NoSuchMethodError (API ændringer)
   - Runtime exceptions fra ændret semantik

### Efter Migration

1. **Fuld test suite:**
   ```bash
   mvn clean test
   mvn clean install
   ```

2. **Manuel verifikation:**
   - Start distribution package
   - Send/modtag test besked
   - Verificer signatur og kryptering

---

## Konklusion

Projektet har **god testdækning for data persistence** men **kritiske huller i sikkerhed og certifikat håndtering**.

**Minimum krav før migration kan anses for sikker:**
- [ ] 10-15 tests for DefaultCertManager
- [ ] 15-20 tests for security processors
- [ ] Integration tests for end-to-end flows

**Total estimat for nye tests: 60-75 tests**

---

# Plan for Etablering af Testdækning

## Oversigt

| Modul | Prioritet | Estimeret Tests | Kompleksitet |
|-------|-----------|-----------------|--------------|
| holodeckb2b-certmanager | KRITISK | 25-30 | Medium |
| holodeckb2b-as4secprovider | KRITISK | 35-45 | Høj |
| **Total** | | **60-75** | |

---

## Del 1: holodeckb2b-certmanager

### Klasse: DefaultCertManager.java

**Fil**: `modules/holodeckb2b-certmanager/src/main/java/org/holodeckb2b/security/trust/DefaultCertManager.java`

**Vigtig kontekst**: CertManager er en **obligatorisk komponent** - Holodeck B2B kan ikke starte uden den. Den loades via Java SPI og bruges af alle sikkerhedsoperationer (signering, kryptering, TLS).

#### Test Fixtures Nødvendige

1. **Test keystores** (JKS/PKCS12 filer):
   - `test-privatekeys.jks` - Private nøgler med certifikater
   - `test-partners.jks` - Partner certifikater
   - `test-trustanchors.jks` - Trust anchors/CA certifikater
   - `test-expired.jks` - Udløbne certifikater (til fejltest)

2. **Test konfigurationsfiler**:
   - `valid-config.xml` - Gyldig fuld konfiguration
   - `minimal-config.xml` - Kun påkrævede felter
   - `invalid-config.xml` - Ugyldig XML
   - `missing-keystores-config.xml` - Manglende keystore stier

3. **Mock objekter**:
   - `IConfiguration` mock med test HB2B home directory

#### Test Cases

##### A. Initialisering (init metode) - 10 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 1 | `testInitWithValidConfig` | Verificer succesfuld initialisering med gyldig config |
| 2 | `testInitWithMinimalConfig` | Verificer default værdier anvendes korrekt |
| 3 | `testInitWithMissingConfigFile` | Verificer SecurityProcessingException ved manglende fil |
| 4 | `testInitWithInvalidXml` | Verificer fejlhåndtering ved ugyldig XML |
| 5 | `testInitWithMissingKeystorePath` | Verificer fejl ved manglende keystore konfiguration |
| 6 | `testInitWithRelativePaths` | Verificer relativ sti resolution til HB2B home |
| 7 | `testInitWithAbsolutePaths` | Verificer absolutte stier bruges direkte |
| 8 | `testInitPasswordTypeLiteral` | Verificer literal password type |
| 9 | `testInitPasswordTypeSystemProperty` | Verificer system property password hentning |
| 10 | `testInitPasswordTypeEnvironment` | Verificer environment variable password hentning |

##### B. Key Pair Operationer - 8 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 11 | `testFindKeyPairByCertificate` | Find key pair via certifikat |
| 12 | `testFindKeyPairByPublicKey` | Find key pair via public key |
| 13 | `testFindKeyPairBySKI` | Find key pair via Subject Key Identifier |
| 14 | `testFindKeyPairByIssuerSerial` | Find key pair via Issuer DN og Serial |
| 15 | `testFindKeyPairByThumbprint` | Find key pair via certifikat hash |
| 16 | `testGetKeyPairWithCorrectPassword` | Hent PrivateKeyEntry med korrekt password |
| 17 | `testGetKeyPairCertificateChain` | Hent certifikat chain for key pair |
| 18 | `testFindKeyPairNotFound` | Verificer null return når ikke fundet |

##### C. Partner Certifikat Operationer - 5 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 19 | `testGetPartnerCertificateByAlias` | Hent partner certifikat via alias |
| 20 | `testFindPartnerCertificateByIssuerSerial` | Find partner cert via Issuer/Serial |
| 21 | `testFindPartnerCertificateBySKI` | Find partner cert via SKI |
| 22 | `testFindPartnerCertificateByThumbprint` | Find partner cert via hash |
| 23 | `testPartnerCertificateNotFound` | Verificer null return når ikke fundet |

##### D. Trust Validering - 7 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 24 | `testValidateTrustedCertificate` | Validering af trusted certifikat giver OK |
| 25 | `testValidateUntrustedCertificate` | Validering af untrusted cert giver NOK |
| 26 | `testValidateExpiredCertificate` | Validering af udløbet cert giver NOK |
| 27 | `testValidateCertificateChain` | Validering af komplet chain |
| 28 | `testValidateWithDirectTrustEnabled` | Validering med direct trust aktiveret |
| 29 | `testGetAllTrustedCertificatesWithJDK` | Hent trust certs inkl. JDK trust store |
| 30 | `testValidateWithRevocationCheckFallback` | Validering falder tilbage ved OCSP fejl |

---

## Del 2: holodeckb2b-as4secprovider

### 2.1 Klasse: SignatureProcessor.java

**Fil**: `modules/holodeckb2b-as4secprovider/src/main/java/org/holodeckb2b/ebms3/security/SignatureProcessor.java`

#### Test Cases - 8 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 1 | `testVerifyValidSignature` | Verificer gyldig signatur |
| 2 | `testVerifyInvalidSignature` | Verificer afvisning af ugyldig signatur |
| 3 | `testVerifySignatureWithX509InKeyInfo` | Verificer med X509 cert i KeyInfo |
| 4 | `testVerifySignatureWithSTR` | Verificer med SecurityTokenReference |
| 5 | `testVerifySignatureWithMultiplePayloads` | Verificer med flere payload referencer |
| 6 | `testExtractCertificateFromSignature` | Verificer certifikat ekstraktion |
| 7 | `testReplayDetection` | Verificer replay attack detektion |
| 8 | `testAlgorithmValidation` | Verificer algoritme compliance |

### 2.2 Klasse: SecurityHeaderCreator.java

**Fil**: `modules/holodeckb2b-as4secprovider/src/main/java/org/holodeckb2b/ebms3/security/SecurityHeaderCreator.java`

#### Test Cases - 12 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 9 | `testCreateHeaderWithSigning` | Opret header med signering |
| 10 | `testCreateHeaderWithEncryption` | Opret header med kryptering |
| 11 | `testCreateHeaderWithUsernameToken` | Opret header med username token |
| 12 | `testCreateHeaderWithSigningAndEncryption` | Kombineret signering og kryptering |
| 13 | `testCreateEBMSTargetedHeader` | Opret EBMS-targeted header |
| 14 | `testSetupSigningParameters` | Verificer signing parameter opsætning |
| 15 | `testSetupEncryptionKeyTransport` | Verificer RSA-OAEP key transport |
| 16 | `testSetupEncryptionKeyAgreement` | Verificer ECDH-ES key agreement |
| 17 | `testRejectRSA15Algorithm` | Verificer RSA-1.5 afvises |
| 18 | `testExtractSignatureResults` | Verificer signature result ekstraktion |
| 19 | `testExtractEncryptionResults` | Verificer encryption result ekstraktion |
| 20 | `testHandleMissingConfiguration` | Verificer fejlhåndtering ved manglende config |

### 2.3 Klasse: SecurityHeaderProcessor.java

**Fil**: `modules/holodeckb2b-as4secprovider/src/main/java/org/holodeckb2b/ebms3/security/SecurityHeaderProcessor.java`

#### Test Cases - 10 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 21 | `testProcessSignatureHeader` | Process signatur header |
| 22 | `testProcessEncryptionHeader` | Process krypterings header |
| 23 | `testProcessUsernameTokenHeader` | Process username token |
| 24 | `testProcessCombinedHeader` | Process kombineret header |
| 25 | `testProcessDefaultTargetedHeader` | Process DEFAULT targeted header |
| 26 | `testProcessEBMSTargetedHeader` | Process EBMS targeted header |
| 27 | `testConvertSignatureResults` | Konverter WSS4J signatur results |
| 28 | `testConvertEncryptionResults` | Konverter WSS4J encryption results |
| 29 | `testHandleSignatureVerificationFailure` | Håndter signatur verifikations fejl |
| 30 | `testHandleDecryptionFailure` | Håndter dekrypterings fejl |

### 2.4 Klasse: CertManWSS4JCrypto.java

**Fil**: `modules/holodeckb2b-as4secprovider/src/main/java/org/holodeckb2b/ebms3/security/CertManWSS4JCrypto.java`

#### Test Cases - 8 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 31 | `testGetCertificatesForSigning` | Hent certs til signering (key pair) |
| 32 | `testGetCertificatesForEncryption` | Hent certs til kryptering (partner) |
| 33 | `testGetCertificatesByIssuerSerial` | Lookup via Issuer/Serial |
| 34 | `testGetCertificatesBySKI` | Lookup via SKI |
| 35 | `testGetCertificatesByThumbprint` | Lookup via thumbprint |
| 36 | `testGetPrivateKeyWithCallback` | Hent private key med callback |
| 37 | `testUnsupportedLookupType` | Verificer SUBJECT_DN/ENDPOINT afvises |
| 38 | `testCertificateNotFound` | Håndter certifikat ikke fundet |

### 2.5 Klasse: SignatureTrustValidator.java

**Fil**: `modules/holodeckb2b-as4secprovider/src/main/java/org/holodeckb2b/ebms3/security/SignatureTrustValidator.java`

#### Test Cases - 5 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 39 | `testValidateTrustedCredential` | Validering af trusted credential |
| 40 | `testValidateUntrustedCredential` | Validering af untrusted credential |
| 41 | `testValidateCertificateChain` | Validering af certifikat chain |
| 42 | `testGetValidationResult` | Hent validerings resultat |
| 43 | `testHandleValidationException` | Håndter validerings exception |

### 2.6 Klasser: EncryptionAction.java & ExtEncryptedKeyProcessor.java

#### Test Cases - 7 tests

| # | Test Case | Formål |
|---|-----------|--------|
| 44 | `testEncryptionWithKeyTransport` | Kryptering med RSA-OAEP |
| 45 | `testEncryptionWithKeyAgreement` | Kryptering med ECDH-ES |
| 46 | `testRejectRSA15KeyTransport` | Afvis RSA-1.5 algoritme |
| 47 | `testDecryptWithKeyTransport` | Dekryptering med key transport |
| 48 | `testDecryptWithKeyAgreement` | Dekryptering med key agreement |
| 49 | `testExtractCertFromX509Data` | Ekstraher cert fra X509Data |
| 50 | `testExtractCertFromSTR` | Ekstraher cert fra SecurityTokenReference |

---

## Test Infrastruktur

### Eksisterende Ressourcer (kan genbruges)

Fra `holodeckb2b-core/src/test/java/`:
- `HolodeckB2BTestCore` - Test core implementation
- `TestCertificateManager` - Mock certificate manager

Fra `holodeckb2b-as4secprovider/src/test/`:
- `HB2BTestUtils` - Test P-Mode oprettelse
- Test keystores i `src/test/resources/`

### Nye Ressourcer Nødvendige

#### Test Keystores

```
src/test/resources/keystores/
├── test-signing-keypair.p12      # Key pair til signering
├── test-encryption-keypair.p12   # Key pair til dekryptering
├── test-partner-certs.jks        # Partner certifikater
├── test-trust-anchors.jks        # CA/Trust anchor certifikater
├── test-expired-cert.p12         # Udløbet certifikat
└── test-chain.p12                # Certifikat med chain
```

#### Test Konfigurationsfiler

```
src/test/resources/config/
├── valid-certmanager-config.xml
├── minimal-certmanager-config.xml
├── invalid-certmanager-config.xml
└── missing-keystores-config.xml
```

#### Test Helper Klasser

```java
// Ny base test klasse for certmanager
public class CertManagerTestBase {
    protected Path testResourcesPath;
    protected IConfiguration mockConfig;

    @BeforeEach
    void setUp() {
        // Setup test environment
    }

    protected Path getTestKeystore(String name) { ... }
    protected String createTestConfig(...) { ... }
}

// Ny base test klasse for security processing
public class SecurityProcessingTestBase {
    protected HolodeckB2BTestCore testCore;
    protected TestCertificateManager certManager;

    @BeforeEach
    void setUp() {
        // Setup test core and cert manager
    }

    protected SOAPEnvelope createTestEnvelope() { ... }
    protected IMessageProcessingContext createTestContext() { ... }
}
```

---

## Implementeringsrækkefølge

### Fase 1: Test Infrastruktur (1-2 dage)

1. Opret test keystores med test certifikater
2. Opret test konfigurationsfiler
3. Opret base test klasser
4. Verificer eksisterende test infrastruktur virker

### Fase 2: CertManager Tests (2-3 dage)

1. Implementer initialiserings tests (10 tests)
2. Implementer key pair tests (8 tests)
3. Implementer partner certifikat tests (5 tests)
4. Implementer trust validering tests (7 tests)

### Fase 3: Security Provider Tests (3-4 dage)

1. Implementer CertManWSS4JCrypto tests (8 tests)
2. Implementer SignatureTrustValidator tests (5 tests)
3. Implementer SignatureProcessor tests (8 tests)
4. Implementer SecurityHeaderCreator tests (12 tests)
5. Implementer SecurityHeaderProcessor tests (10 tests)
6. Implementer Encryption tests (7 tests)

### Fase 4: Integration Tests (1-2 dage)

1. End-to-end signering test
2. End-to-end kryptering test
3. Kombineret signering + kryptering test

---

## Verifikation

Efter hver fase:

```bash
# Kør tests for specifikt modul
mvn test -pl modules/holodeckb2b-certmanager
mvn test -pl modules/holodeckb2b-as4secprovider

# Verificer ingen regressioner
mvn test
```

---

## Risici og Mitigering

| Risiko | Mitigering |
|--------|------------|
| Test keystores med selvsignerede certs kan opføre sig anderledes end produktions certs | Inkluder tests med CA-signerede test certs |
| WSS4J mocking kan være komplekst | Start med integration tests der bruger rigtige WSS4J objekter |
| Tidsestimater kan være for optimistiske | Prioriter de mest kritiske tests først (init, trust validation) |
| BouncyCastle version forskelle | Test med samme BC version som produktionskode |

---

## Success Kriterier

- [ ] Alle 60-75 tests implementeret og kører grønt
- [ ] Test coverage for DefaultCertManager > 80%
- [ ] Test coverage for security processors > 70%
- [ ] Ingen regressioner i eksisterende tests
- [ ] Tests dokumenterer forventet adfærd for jakarta migration

---

## Estimeret Tidsforbrug

| Fase | Varighed |
|------|----------|
| Fase 1: Test Infrastruktur | 1-2 dage |
| Fase 2: CertManager Tests | 2-3 dage |
| Fase 3: Security Provider Tests | 3-4 dage |
| Fase 4: Integration Tests | 1-2 dage |
| **Total** | **7-11 dage** |
