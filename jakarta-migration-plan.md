# Holodeck B2B: javax til jakarta Migration Plan

## Oversigt

Migration fra javax.* til jakarta.* namespaces i Holodeck B2B 8.1.0. Kræver opgradering til JDK 17+ og større dependency-opdateringer.

---

## Forudsætninger

**Java version**: 11 → **17** (krævet af WSS4J 4.0.0)

---

## Dependency Versionsændringer

### Parent POM (`pom.xml`)

| Dependency | Nuværende | Mål | Bemærkning |
|------------|-----------|-----|------------|
| Java | 11 | **17** | WSS4J 4.0.0 krav |
| Axis2 | 1.8.2 | **2.0.0** | Fuld Jakarta EE support |
| AXIOM | 1.4.0 | **2.0.0** | Følger Axis2 |
| JAXB API | javax.xml.bind:jaxb-api:2.3.1 | **jakarta.xml.bind:jakarta.xml.bind-api:4.0.2** | Namespace ændring |
| JAXB Runtime | org.glassfish.jaxb:jaxb-runtime:2.3.8 | **org.glassfish.jaxb:jaxb-runtime:4.0.5** | Jakarta version |
| Activation | com.sun.activation:jakarta.activation:1.2.1 | **jakarta.activation:jakarta.activation-api:2.1.3** | Namespace ændring |

### holodeckb2b-default-mds (`modules/holodeckb2b-default-mds/pom.xml`)

| Dependency | Nuværende | Mål | Bemærkning |
|------------|-----------|-----|------------|
| Hibernate | 5.6.15.Final | **6.4.4.Final** | jakarta.persistence |
| Derby | 10.12.1.1 | **10.16.1.1** | Java 17 kompatibilitet |

### holodeckb2b-as4secprovider (`modules/holodeckb2b-as4secprovider/pom.xml`)

| Dependency | Nuværende | Mål | Bemærkning |
|------------|-----------|-----|------------|
| WSS4J | 3.0.4 | **4.0.0** | Jakarta EE, JDK 17 krav |

### holodeckb2b-certmanager (`modules/holodeckb2b-certmanager/pom.xml`)

| Plugin | Nuværende | Mål | Bemærkning |
|--------|-----------|-----|------------|
| jaxb2-maven-plugin | 2.5.0 | **3.1.0** | Jakarta namespace generering |

---

## Source Code Ændringer

### VIGTIG: Hvad der IKKE ændres (JDK standard klasser)

Følgende forbliver uændret da de er del af JDK, ikke Jakarta EE:
- `javax.xml.namespace.QName` (50+ filer)
- `javax.xml.crypto.*` (SignatureProcessor.java)
- `javax.xml.parsers.*` (5 filer)
- `javax.xml.stream.*` (3 filer)
- `javax.xml.transform.*` (4 filer)
- `javax.security.auth.*` (6 filer)
- `javax.cache.*` (JSR-107, ikke Jakarta EE)

### Filer der kræver ændringer

#### 1. javax.xml.bind → jakarta.xml.bind (1 fil + genererede)

**`modules/holodeckb2b-certmanager/src/main/java/org/holodeckb2b/security/trust/DefaultCertManager.java`**
```java
// FRA:
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

// TIL:
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
```

#### 2. javax.persistence → jakarta.persistence (22 filer)

**Alle JPA entity filer i `modules/holodeckb2b-default-mds/src/main/java/org/holodeckb2b/storage/metadata/`:**

- `jpa/MessageUnit.java`
- `jpa/UserMessage.java`
- `jpa/Receipt.java`
- `jpa/PullRequest.java`
- `jpa/SelectivePullRequest.java`
- `jpa/ErrorMessage.java`
- `jpa/PayloadInfo.java`
- `jpa/TradingPartner.java`
- `jpa/MessageUnitProcessingState.java`
- `jpa/CollaborationInfo.java`
- `jpa/Service.java`
- `jpa/Description.java`
- `jpa/Property.java`
- `jpa/PartyId.java`
- `jpa/SchemaReference.java`
- `jpa/AgreementReference.java`
- `jpa/EbmsError.java`
- `DatabaseConfiguration.java`
- `DefaultMetadataStorageProvider.java`

Plus 5 test filer.

#### 3. javax.activation → jakarta.activation (6 filer)

**holodeckb2b-as4secprovider:**
- `src/main/java/org/holodeckb2b/ebms3/security/callbackhandlers/AttachmentCallbackHandler.java`

**holodeckb2b-ebms3as4:**
- `src/main/java/org/holodeckb2b/ebms3/handlers/inflow/SaveUserMsgAttachments.java`
- `src/main/java/org/holodeckb2b/as4/compression/CompressionHandler.java`
- `src/main/java/org/holodeckb2b/as4/compression/CompressionDataHandler.java`

Plus test filer.

---

## Konfigurationsfil Ændringer

### persistence.xml (`modules/holodeckb2b-default-mds/src/test/resources/META-INF/persistence.xml`)

```xml
<!-- FRA: -->
<persistence version="1.0"
    xmlns="http://java.sun.com/xml/ns/persistence">

<!-- TIL: -->
<persistence version="3.0"
    xmlns="https://jakarta.ee/xml/ns/persistence">
```

Property ændringer:
```xml
<!-- FRA: -->
<property name="javax.persistence.schema-generation.database.action" .../>

<!-- TIL: -->
<property name="jakarta.persistence.schema-generation.database.action" .../>
```

### Hibernate Dialect (DatabaseConfiguration.java)

```java
// FRA:
import org.hibernate.dialect.DerbyTenSevenDialect;

// TIL:
import org.hibernate.dialect.DerbyDialect;
// Hibernate 6.x har unified dialect klasser
```

---

## Implementeringsrækkefølge

1. **Opret feature branch**: `git checkout -b feature/jakarta-migration`
2. **Opdater JDK til 17** i parent pom.xml
3. **Opdater parent pom.xml** med nye dependency versioner
4. **Opdater holodeckb2b-default-mds** (største ændring - JPA/Hibernate)
5. **Opdater holodeckb2b-certmanager** (JAXB)
6. **Opdater holodeckb2b-as4secprovider** (WSS4J, activation)
7. **Opdater holodeckb2b-ebms3as4** (activation)
8. **Kør tests** for hvert modul
9. **Byg distribution** og test

---

## Test Strategi

```bash
# Trin-for-trin test
mvn clean compile -DskipTests
mvn test -pl modules/holodeckb2b-interfaces
mvn test -pl modules/holodeckb2b-core
mvn test -pl modules/holodeckb2b-default-mds
mvn test -pl modules/holodeckb2b-certmanager
mvn test -pl modules/holodeckb2b-as4secprovider
mvn test -pl modules/holodeckb2b-ebms3as4
mvn test -pl modules/holodeckb2b-ui

# Fuld test
mvn clean test

# Distribution build
mvn clean install
```

---

## Risici og Afbødning

| Risiko | Impact | Afbødning |
|--------|--------|-----------|
| WSS4J 4.0.0 API ændringer | Høj | Test alle sikkerhedsscenarier grundigt |
| Hibernate 6.x query ændringer | Medium | Gennemgå HQL/JPQL for deprecated syntax |
| Axis2 2.0.0 HTTPClient 5.x | Høj | Test HTTP/HTTPS transport konfiguration |
| JAXB 4.0 marshalling forskelle | Lav | Test config fil indlæsning |

---

## Rollback

```bash
# Tag før migration
git tag v8.1.0-pre-jakarta

# Ved problemer
git checkout master
```

---

## Verifikation

Efter migration er komplet:
1. `mvn clean install` skal køre uden fejl
2. Alle unit tests skal passere
3. Distribution package skal kunne startes
4. Test message exchange (send/receive) med ekstern partner eller loopback
