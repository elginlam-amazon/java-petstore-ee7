# 📊 Executive Report: Petstore Java EE 7 → Java 25 / Jakarta EE 11 Upgrade (Iteration 2)  
  
## 1. 🧭 Executive Summary  
  
- 🎯 **Scope:** `org.agoncal.application:petstoreee7` (single-module WAR). It moved from Java 1.8 / Java EE 7 (`javax.*`) to **Java 25 + Jakarta EE 11**, with Jakarta Faces 4.1, Persistence 3.2 descriptors, PrimeFaces 14 (`jakarta` classifier) and Apache POI 5.  
- 🤖 **How:** OpenRewrite applied the bulk namespace, descriptor and build changes. The log reports 149 changed source files and an estimated 31h 23m saved. Amazon Kiro CLI then fixed the build until it was green and closed gaps the recipes left.  
- ✅ **Outcome:** `mvn clean verify` **passes** on Corretto 25 with 0 compilation errors and 9/9 unit tests green. Arquillian integration tests are skipped by default (`skipITs=true`) and were **not** run.  
- 🌱 **Spring:** not part of this run. The log says `Skipping Spring Boot upgrade: not requested (springBootTargetVersion=none)`. The application has no Spring dependency, so there is no Spring version to verify.  
- ⚠️ **Still open:** deploying to a Jakarta EE 11 server (WildFly 40/41), the integration-test stack (still on the javax/WildFly 26 line), Swagger contract generation, and the PrimeFaces UI items held for human review.  
  
## 2. 🏗️ Application Changes  
  
- ☕ **Java level:** `maven.compiler.source/target` went from 1.8 to **25**. The compiler, surefire, failsafe and war plugins were bumped by the Java 17/21/25 recipes.  
- 🔁 **Namespace:** Java EE `javax.*` became `jakarta.*` across 107 Java files: persistence, inject, ejb, enterprise, validation, ws.rs, faces, interceptor, decorator, annotation and xml.bind. JDK `javax.security.auth.*` and `javax.naming` were correctly left on javax.  
- 🖥️ **Faces 4.1:** 35 XHTML views now use the `jakarta.faces.*` and `primefaces` URNs. `faces-config.xml` was set to version 4.1 by `JakartaFacesConfigXml41`. In `web.xml`, `FacesServlet` and the context-params moved to `jakarta.faces.*`.  
- 🗄️ **Persistence:** `persistence.xml` moved to `https://jakarta.ee/xml/ns/persistence` version **3.2** (`UpgradePersistenceXmlToJakartaEE11`), and its `jakarta.persistence.*` property keys were updated. `validation.xml` moved to Jakarta Validation 3.0.  
- 🫘 **CDI:** `beans.xml` is on the Jakarta namespace and keeps `bean-discovery-mode="all"`. Kiro raised it to version 4.1.  
- 📦 **Dependencies:**  
  - PrimeFaces 6.1 → **14.0.18** (`jakarta` classifier).  
  - Apache POI 4.1.2 → **5.5.1**.  
  - Byte Buddy pinned to 1.17.8 so the EqualsVerifier tests run on Java 25.  
  - The Java EE 7 API bundle was replaced with `jakarta.jakartaee-api` **11.0.0** (Kiro).  
- 🐘 **Hibernate 7.1 recipe:** ran, but changed **0 files**, because the app uses plain JPA and gets Hibernate from the server. Kiro handled the Hibernate 7 runtime behaviour changes directly in the code (see section 4).  
- 📑 **Apache POI pre-passes:** ran, with **0 files changed**. The one flagged `setAlignment` call was a false positive: OpenPDF's `Element.ALIGN_CENTER` in `CatalogPdfExport.java:44`, not POI. The POI code already used the `HorizontalAlignment`/`FillPatternType` enums.  
- 🧩 **FreeMarker template recipe:** ran, with **0 files changed** (the project has no `.ftl` files).  
  
## 3. 🛠️ Tools Used  
  
- ☕ **Java SDK:** Amazon Corretto **25.0.4.1** (25.0.4.10.1 LTS), with Apache Maven 3.9.16.  
- 🔧 **OpenRewrite:** `rewrite-maven-plugin` **6.42.0** with these recipe libraries:  
  - `rewrite-migrate-java` 3.38.0 (JakartaEE11 and the Faces 2→3→4→4.1 chain)  
  - `rewrite-hibernate` 2.22.0 (`MigrateToHibernate71`)  
  - `rewrite-apache` 2.27.1 (`UpgradeApachePoi_5`)  
  - custom `com.amazonaws.java.migrate.*` wrappers: `UpgradeToJakartaEE11`, `UpgradeToJakartaFaces41`, `UpgradePersistenceXmlToJakartaEE11`, `UpgradeApachePoi5`, `UpgradeToJava25`, the POI pre-passes, and `MigrateJakartaEeTemplates`  
- 🤖 **Amazon Kiro CLI:** `kiro-cli` **2.14.2**, model **claude-opus-5.5**, running non-interactively. The session took 7m 38s and used 23.86 credits. It consulted migration knowledge docs 30, 31, 32, 33 and 35.  
  
## 4. 💻 Code Changes  
  
### 🔄 OpenRewrite (from `SourcesFileResults.csv`)  
- 📁 **149 files changed:** 107 `.java`, 35 `.xhtml` and 7 `.xml` (`pom.xml`, `persistence.xml`, `validation.xml`, `beans.xml`, `web.xml`, `faces-config.xml`, `forge.taglib.xml`).  
- 🔁 `ChangePackage` javax → jakarta. The `JavaxInjectMigrationToJakartaInject`, `JavaxPersistenceToJakartaPersistence`, `JavaxEjbToJakartaEjb` and `JavaxEnterpriseToJakartaEnterprise` recipes produced the most entries.  
- 🖥️ `JakartaFacesXhtmlEE10` / `EE9` updated the XHTML namespaces; `UpdateJakartaFacesApi3` and `JakartaFacesConfigXml41` updated the Faces API and descriptor.  
- ✨ Java 25 idioms:  
  - `ReplaceSystemOutWithIOPrint` (`GoldenMasterOutputs`, `LoginContextProducer`)  
  - `ReplaceUnusedVariablesWithUnderscore`  
  - `AddSerialAnnotationToSerialVersionUID`  
  - `PrimitiveWrapperClassConstructorToValueOf`  
  
### 🤖 Kiro CLI (build fix and gap closure)  
- 📦 **`pom.xml`:**  
  - ➕ `jakarta.platform:jakarta.jakartaee-api:11.0.0` (provided) replaces `org.jboss.spec:jboss-javaee-7.0`. This fixed the 100+ `package jakarta.faces.* does not exist` compile errors.  
  - ➖ Removed 6 compile-scope Jakarta API jars at EE 10 versions (annotation, ejb, cdi, persistence, validation, ws.rs) that the recipe had added; they were being bundled into `WEB-INF/lib`. The separate `jakarta.xml.bind-api` jar was also removed and re-added as provided (next bullet).  
  - ➕ `jakarta.xml.bind-api:4.0.5` (provided), because XML Binding is not in the EE 11 platform API jar.  
  - ➖ Removed `org.glassfish.jaxb:jaxb-runtime:2.3.9`, a javax-line runtime the Java 11 recipe had added.  
  - 🔼 `log4j-core` 2.19.0 → **2.24.3**, to match the log4j-api 2.24.3 that POI 5.5.1 pulls in.  
  - ⏸️ `swagger-maven-plugin` 3.1.8 now runs with `skipSwaggerGeneration=true`. It is javax-only and was overwriting `swagger.json` with an empty contract.  
- 📄 **Descriptors:** `web.xml` 5.0 → **6.1**, `beans.xml` 4.0 → **4.1**, and `forge.taglib.xml` moved from the Facelets 1.0 DOCTYPE to the **4.1** schema.  
- 🐘 **`CatalogExportService`** (Hibernate 7 silent breaks):  
  - 🕒 `lastCatalogChange()` accepts both `LocalDateTime` (Hibernate 7) and `Timestamp` from the native query.  
  - 🔄 `reload(Item)` uses `find` before `refresh`, because refreshing a detached entity now throws. It throws `EntityNotFoundException` if the row is gone.  
- 🧪 **Integration tests:** 25 IT classes now load `src/test/resources/test-beans.xml` (`bean-discovery-mode="all"`) instead of an empty `beans.xml`. This avoids the CDI 4 "annotated" default, which would drop the unscoped producers.  
  
## 5. ⏱️ Time Savings Estimate  
  
- 🔧 **OpenRewrite:** **31h 23m**, as reported in the log (`Estimate time saved`) for the 149-file namespace, descriptor and build migration.  
- 🤖 **Kiro CLI:** about **8–12 h** of manual work in 7m 38s. That covers diagnosing the API-bundle mismatch, consolidating dependencies, the Swagger regression, the descriptor bumps, the Hibernate 7 and CDI 4 fixes, the 25 IT updates, and the report. This figure is an estimate.  
- 📈 **Total:** about **39–43 engineer-hours** saved, against roughly 1 h of wall-clock pipeline time (10:14 → 10:33 UTC for the transform plus the Kiro session).  
  
## 6. 🚀 Next Steps  
  
- 🖥️ **Target server:** WildFly 26 (`version.wildfly`) cannot run `jakarta.*`. Deploy to **WildFly 40/41** (Jakarta EE 11). WildFly is EE 11-certified on SE 17/21 only, so choose between SE 25 (recommended, not certified) and SE 21 (certified).  
- 🫘 **Manual deployment test (CDI 4):** `WEB-INF/beans.xml` relies on `bean-discovery-mode="all"` for the unscoped producers (`LoggingProducer`, `DatabaseProducer`, `NumberProducer`, `ConfigPropertyProducer`, `LoginContextProducer`) and for `AbstractBean` subclasses. Deploy the app and open every page; the build cannot prove this.  
- 🐘 **Manual integration tests (Hibernate 7):**  
  - `CatalogExportService.lastCatalogChange()`: the native query now returns java.time types.  
  - `CatalogExportService.reload(Item)`: refresh of a detached entity.  
  - The `StatelessSession` cache change does not apply (no usage, and `shared-cache-mode` is NONE).  
- 🧪 **Integration-test stack:** upgrade WildFly 26.1.3 / wildfly-arquillian 3.0.1 / RESTEasy 3.15.3 / `arquillian-rest-client-impl-3x` and the `wildfly-11.0.0.Final` paths. Then run `-Parquillian-wildfly-managed`.  
- 📘 **Swagger:** migrate `io.swagger` 1.x (`@Api`/`@ApiOperation` in 5 endpoints) to swagger-core 2.x `-jakarta` (OpenAPI 3) and re-enable generation. Until then, `swagger.json` has to be maintained by hand.  
- 🎨 **PrimeFaces 15:** a single version bump, but tied to the theme decision in the UI gate below.  
- 🧹 **Housekeeping:** `src/main/forge/generate.fsh` still references the Java EE 7 coordinates, and `wildfly-maven-plugin` 4.0.0.Final should be checked against WildFly 41.  
  
## PrimeFaces human UI gate  
  
🛑 Every site below was **left unchanged for human UI review**. The build stays green because PrimeFaces 14.0.18-jakarta still ships `p:calendar`. The `pom.xml` line numbers are from the post-OpenRewrite POM; after Kiro's dependency changes the same entries are at `pom.xml:113` and `pom.xml:166`.  
  
- 🎨 `pom.xml:115` (`<artifactId>all-themes</artifactId>`, dependencyManagement): legacy `org.primefaces.themes:all-themes:1.1.0` ThemeRoller themes. PF 15 replacement: a built-in `saga-blue` / `arya-blue` / `vela-blue`, `primefaces-themes.jar` (Luna, Nova, Material, Bootstrap) or a Theme Designer theme, then drop `all-themes`. Left unchanged for human UI review.  
- 🎨 `pom.xml:204` (`<artifactId>all-themes</artifactId>`, dependency): the same legacy `all-themes` dependency with the same PF 15 replacement. Left unchanged for human UI review.  
- 🎨 `src/main/webapp/WEB-INF/web.xml:33` (`primefaces.THEME` = `bootstrap`): a legacy ThemeRoller theme, which no longer renders correctly on PF 15. PF 15 replacement: a built-in theme such as `saga-blue`, `primefaces-themes.jar`, or a Theme Designer theme. Left unchanged for human UI review.  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:86` (`<i class="fa fa-user">`): a FontAwesome icon. PF 15 replacement: PrimeIcons `pi pi-user`, or keep an app-supplied FontAwesome. Left unchanged for human UI review.  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:92` (`<i class="fa fa-sign-out">`): a FontAwesome icon. PF 15 replacement: PrimeIcons `pi pi-sign-out`, or keep an app-supplied FontAwesome. Left unchanged for human UI review.  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:100` (`<i class="fa fa-sign-in">`): a FontAwesome icon. PF 15 replacement: PrimeIcons `pi pi-sign-in`, or keep an app-supplied FontAwesome. Left unchanged for human UI review.  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:107` (`<i class="fa fa-shopping-cart">`): a FontAwesome icon. PF 15 replacement: PrimeIcons `pi pi-shopping-cart`, or keep an app-supplied FontAwesome. Left unchanged for human UI review.  
- 📅 `src/main/webapp/shopping/createaccount.xhtml:49` (`<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button" pattern="d/M/yyyy">`): deprecated since PF 10. PF 15 replacement: `<p:datePicker id="dateOfBirth" monthNavigator="true" yearNavigator="true" showIcon="true" pattern="d/M/yyyy">`. Left unchanged for human UI review.  
- 📅 `src/main/webapp/shopping/updateaccount.xhtml:53` (`<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button">`): deprecated since PF 10. PF 15 replacement: `<p:datePicker id="dateOfBirth" monthNavigator="true" yearNavigator="true" showIcon="true">`. Left unchanged for human UI review.

## Runtime verification checklist (silent breaks)

The build passing does not prove any of these. Each one fails only at deployment or run time, so verify it on the target server and database before accepting the upgrade (migration knowledge doc 36).

- Applies because: Hibernate 7: the Hibernate 7 step ran (hibernateTarget=7.1); the module uses JPA (src/main/resources/META-INF/persistence.xml)
- Applies because: CDI 4: jakartaEeTarget=11 (CDI 4.1); the module uses CDI (src/main/webapp/WEB-INF/beans.xml)

- [ ] **Hibernate 7: Native queries return java.time types** (doc 10b, §5.1). Run every native query that returns date or time columns against the target database: Hibernate 7 returns java.time.LocalDate / LocalTime / LocalDateTime instead of java.sql.Date / Time / Timestamp, so a cast or a formatter can fail with ClassCastException. Fix the reads, or set hibernate.query.native.prefer_jdbc_datetime_types=true for the persistence unit.
  - `src/main/java/org/agoncal/application/petstore/export/CatalogExportService.java:32`
- [ ] **Hibernate 7: StatelessSession now uses the second-level cache** (doc 10b, §5.2). Check every StatelessSession: in Hibernate 7 it reads from and writes to the L2 cache by default, so a batch job can read stale data with no error. Restore the old behavior with setCacheMode(CacheMode.IGNORE), or CacheMode.GET for bulk writers.
  - No site found by the static scan; check code that builds these calls at run time.
- [ ] **Hibernate 7: refresh() / lock() on a detached entity throws** (doc 10b, §5.3). Exercise every refresh() and lock() call whose entity may come from another transaction, an HTTP session, a JSF view-scoped bean or a cache: Hibernate 7 throws IllegalArgumentException on a detached entity. Work on a managed instance (find with a lock mode, or merge then refresh).
  - `src/main/java/org/agoncal/application/petstore/export/CatalogExportService.java:52`
- [ ] **CDI 4: beans.xml discovery mode default flipped to annotated** (doc 32, "What changed"). Deploy the app and open its pages: under CDI 4 an empty beans.xml, or one without bean-discovery-mode, means annotated, so classes without a bean-defining annotation stop being beans and fail only at deployment (WELD-001408) or page render. Keep bean-discovery-mode="all" or add scopes.
  - `src/main/webapp/WEB-INF/beans.xml: bean-discovery-mode="all", version 4.1: old behavior kept`
- [ ] **CDI 4: @Named classes without a bean-defining annotation** (doc 32, "Detection patterns"). Render every view that uses these @Named classes and inject them once: @Named alone is not a bean-defining annotation, so they are beans only while discovery is "all". With annotated discovery EL resolves them to null (Target Unreachable). Add a scope, or keep discovery "all".
  - `src/main/java/org/agoncal/application/petstore/security/SimpleCallbackHandler.java:21`

## Golden-master output checks (optional)

This module writes PDF, Excel or JasperReports output, and a green build does not show that the output is unchanged. Optionally, capture each output file before and after the upgrade and compare the captures structurally: PDFBox text per page and Apache POI cell values, never the bytes (PDF and XLSX embed timestamps). Migration knowledge doc 36 has the steps.

High-risk output paths found by the static scan:

- `src/main/java/org/agoncal/application/petstore/export/CatalogExcelExport.java` (Excel: Apache POI)
- `src/main/java/org/agoncal/application/petstore/export/CatalogPdfExport.java` (PDF: OpenPDF / iText 2 (com.lowagie))

No golden-master capture was made in this iteration (the scaffolding is optional).

## Kiro credit usage

💳 Kiro CLI credits used: 58.47  
