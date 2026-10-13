# 📊 Executive Report: Petstore Java EE 7 → Java 25 / Jakarta EE 11 Upgrade (Iteration 3)  
  
## 1. 📝 Executive Summary  
  
- 🎯 **Scope:** `petstoreee7` (WAR, Maven) moved from **Java 8 / Java EE 7 (JSF 2.2, PrimeFaces 6.1)** to **Java 25 / Jakarta EE 11 (Jakarta Faces 4.1, PrimeFaces 14.0.18 `jakarta`)**.  
- 🤖 **How:** OpenRewrite ran `UpgradeToJakartaEE11` → `UpgradeToJakartaFaces41` → `UpgradeToHibernate71` → `UpgradeToJava25` (BUILD SUCCESS, 1:43 min), then a separate FreeMarker template pass (BUILD SUCCESS, 45 s). After that, Kiro CLI fixed the remaining build breaks in about 6 minutes.  
- ✅ **Outcome:** `mvn clean package` is **green on JDK 25 (Corretto 25.0.4)**, with 0 compilation errors and 9/9 unit tests passing. The WAR no longer bundles any EE API jars.  
- 🌱 **Spring:** not in scope. The log says `Skipping Spring Boot upgrade: not requested (springBootTargetVersion=none)`. The project has no Spring dependencies, and no Spring recipe or "effective Spring Framework version" check ran, so this report claims no Spring upgrade.  
- 🗄️ **Hibernate 7.1:** the recipe ran but changed nothing (0 rows in SourcesFileResults). The app has no `org.hibernate` dependency and uses the Hibernate that WildFly provides.  
- ⚠️ **Not yet verified:** deployment to WildFly 40/41, integration tests (`skipITs=true`), and the 9 PrimeFaces UI sites that were left for human review.  
  
## 2. 🏗️ Application Changes  
  
- ☕ **Java level:** `maven.compiler.source/target` 1.8 → **25**.  
- 🔁 **Namespace:** Java EE `javax.*` → `jakarta.*` (Persistence, CDI, Inject, EJB, Validation, JAX-RS, JAXB, Interceptor, Decorator, Annotation, Faces).  
- 🛡️ **Kept on javax on purpose:** jsr305 `javax.annotation.Nonnull/Nullable` (re-applied by the `PreserveJsr305Annotations` guard) and the JDK packages `javax.security.auth.*`, `javax.naming` and `javax.sql`.  
- 🖥️ **Web tier:** 35 XHTML views now use the `jakarta.faces.*`, `jakarta.tags.*` and `primefaces` URNs. `faces-config.xml` is 4.1. JSF managed beans are now CDI.  
- 📄 **Descriptors (Jakarta EE 11 level):**  
  - `web.xml` 3.1 → 6.1  
  - `beans.xml` 1.1 → 4.1, still `bean-discovery-mode="all"`  
  - `persistence.xml` 2.x → 3.2  
  - `validation.xml` → 3.0  
  - `forge.taglib.xml` → 4.1 schema  
- 📦 **Dependencies:** PrimeFaces 6.1 → 14.0.18 (`jakarta` classifier). The EE 7 umbrella API was replaced with individual Jakarta EE 11 APIs in `provided` scope.  
- 🧩 **Code generator template:** `entity.ftl` moved to `jakarta.persistence`. `datasource.ftl` (JDK `javax.naming`/`javax.sql`) was correctly left alone.  
  
## 3. 🛠️ Tools Used  
  
- ☕ **Java SDK:** Amazon Corretto **25.0.4.1** (25.0.4.10.1 LTS) for the build and tests.  
- 🔧 **OpenRewrite:**  
  - `rewrite-maven-plugin` 6.42.0, `rewrite-migrate-java` 3.38.0, `rewrite-hibernate` 2.22.0  
  - Recipes: `com.amazonaws.java.migrate.UpgradeToJakartaEE11`, `UpgradeToJakartaFaces41` (Faces 2x→3x→4x→4.1x chain plus `JakartaFacesConfigXml41` and `PreserveJdkJavaxInFacesFiles`), `UpgradeToHibernate71`, `UpgradeToJava25` (official Java 25 chain without `IfElseIfConstructToSwitch`), `MigrateJakartaEeTemplates`  
  - 📈 SourcesFileResults: **144 files** changed in the main run (102 `.java`, 35 `.xhtml`, 7 `.xml`) and **1 file** (`entity.ftl`) in the template run  
- 🤖 **Amazon Kiro CLI:** `kiro-cli` **2.14.2**, model **claude-opus-5.5**. It fixed the build, used the migration knowledge base (docs 30–33), and wrote the report. Session time was 5m 57s.  
  
## 4. 💻 Code Changes  
  
**🤖 By OpenRewrite (from the log):**  
- 🔤 Imports rewritten by `JavaxMigrationToJakarta` (214 results): Persistence (33), Inject (45), Validation (24), EJB (21), Enterprise (20), Annotation (13), WS (12) and Faces API (11).  
- 🧹 `FacesManagedBeansRemoved` turned `@ManagedBean` into CDI `@Named`, for example in `DebugBean`.  
- 🌐 `JakartaFacesXhtmlEE9/EE10` (111 results) rewrote the XHTML namespaces. `JavaxWebXmlToJakartaWebXml`, `JavaxBeansXmlToJakartaBeansXml`, `JavaxPersistenceXmlToJakartaPersistenceXml` and `JavaxFacesConfigXmlToJakartaFacesConfigXml` updated the descriptors.  
- 🏗️ `pom.xml` changes:  
  - Java 25 build level  
  - Byte Buddy pinned to 1.17.8 (needed for EqualsVerifier on Java 25)  
  - surefire/failsafe 3.5.6, war plugin 3.5.1  
  - individual Jakarta API dependencies added  
- 🧪 Integration test classes (`*IT.java`) migrated to jakarta.  
  
**🧠 By Kiro CLI (build fix):**  
- ❌➡️✅ **Root cause of every compile error:** `jakarta.faces.*` was missing from the classpath. Fix: added `jakarta.faces:jakarta.faces-api` **4.1.2** with `provided` scope.  
- 📦 Moved the EE APIs to `provided` scope and Jakarta EE 11 versions: CDI 4.1.0, Persistence 3.2.0, Validation 3.1.1, REST 4.0.0. EJB 4.0.1, Annotation 3.0.0 and JAXB 4.0.5 were already at that level.  
- 🗑️ Removed `org.jboss.spec:jboss-javaee-7.0` (javax EE 7 APIs, plus its unused properties) and `org.glassfish.jaxb:jaxb-runtime` 2.3.9 (javax JAXB, conflicts with `jakarta.xml.bind` 4).  
- 📜 `swagger-maven-plugin` 3.1.8 is now `skipSwaggerGeneration=true`.  
  - Why: it fails without javax JAXB, and it only reads `javax.ws.rs`, so it was overwriting `swagger.json` with an empty spec.  
  - The committed `swagger.json` is preserved.  
- 📄 Bumped `web.xml` to 6.1, `beans.xml` to 4.1 and `persistence.xml` to 3.2. Switched `forge.taglib.xml` from the Facelets 1.0 DOCTYPE to the 4.1 schema.  
- 🔍 Checked the Hibernate 7 silent-break patterns. None are present: no native queries, `StatelessSession`, `refresh()` or `lock()`.  
  
## 5. ⏱️ Time Savings Estimate  
  
- 🤖 **OpenRewrite's own estimate:** 31h 33m (main run) + 5m (templates) ≈ **31.6 hours** (113,880 s in SourcesFileResults).  
- 🧠 **Kiro build fix and dependency reconciliation:** done in about 6 minutes. I estimate it replaces **1–2 developer days** of manual work: diagnosing the classpath, aligning EE 11 APIs, researching the swagger plugin, bumping descriptors, and reviewing CDI and Hibernate behavior.  
- 💰 **Total, estimated:** about **5–6 developer days** of manual migration done in about **10 minutes** of automated run time. This is an estimate, not a measurement.  
  
## 6. 🚀 Next Steps  
  
- 🧪 **Deploy to WildFly 40 or 41** (Jakarta EE 11). WildFly 26 can no longer run this app.  
  - ⚖️ EE 11 on Java SE 25 is not certified yet; WildFly claims EE 11 compatibility on SE 17 and 21 only.  
  - Choose between SE 25 (recommended by WildFly, not certified) and SE 21 (certified).  
- 🫘 **CDI 4 deployment test:** `beans.xml` explicitly keeps `bean-discovery-mode="all"`. Check that injection and EL names still resolve for the classes that have no bean-defining annotation:  
  - the `@Named` entities `Category`, `Product`, `Customer`, `Item`, `PurchaseOrder`  
  - the `NumberProducer` producer  
- 🎨 **`PurchaseOrderDecorator`:** it has no `@Priority` and isn't listed in `<decorators>`, so it is disabled. It was disabled before the upgrade too. Confirm this is intended.  
- 🗄️ **Hibernate 7 smoke test** on the server: the `init_db.sql` load script and the `drop-and-create` schema action.  
- 🔬 **Arquillian ITs:** before running `-Parquillian-wildfly-*`:  
  - Move `version.wildfly` (26.1.3) and `wildfly-arquillian-container-*` (3.0.1) to the WildFly 40+ line.  
  - Move the REST test client (RESTEasy 3.15.3) to its jakarta line.  
  - Update `wildfly-maven-plugin` 4.0.0.Final.  
- 📜 **Swagger:** move to a Jakarta-aware OpenAPI 3 generator (`io.swagger.core.v3` `*-jakarta`), then remove `skipSwaggerGeneration`.  
- 🧹 **Optional:** update `log4j-core` 2.19.0, and fix the outdated `@ManagedBean` comment in `web.xml`.  
- 🎨 **Complete the PrimeFaces human UI review** below. PrimeFaces 15 is optional; legacy themes no longer render correctly on it.  
  
## PrimeFaces human UI gate  
  
Each site below was **left unchanged for human UI review** (migration knowledge doc 31). Line numbers are from the OpenRewrite UI-gate scan. After Kiro's POM cleanup, the `all-themes` entries are now at `pom.xml:102` and `pom.xml:199`.  
  
- 🎨 `pom.xml:112` (dependencyManagement): `org.primefaces.themes:all-themes` 1.1.0 (legacy ThemeRoller themes). PrimeFaces 15 replacement: built-in `saga-blue` / `arya-blue` / `vela-blue`, `primefaces-themes.jar` (Luna, Nova, Material, Bootstrap) or a Theme Designer theme; then drop `all-themes`. Left unchanged for human UI review.  
- 🎨 `pom.xml:196` (dependencies): `org.primefaces.themes:all-themes`. Replacement: same as above, remove the dependency once a PrimeFaces 15 theme is chosen. Left unchanged for human UI review.  
- 🎨 `src/main/webapp/WEB-INF/web.xml:33`: `primefaces.THEME` = `bootstrap` (legacy theme). Replacement: a PrimeFaces 15 theme such as `saga-blue`, or a theme from `primefaces-themes.jar` or Theme Designer. Left unchanged for human UI review.  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:86`: FontAwesome `fa fa-user`. Replacement: PrimeIcons `pi pi-user`, or keep the FontAwesome the app supplies itself. Left unchanged for human UI review.  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:92`: FontAwesome `fa fa-sign-out`. Replacement: PrimeIcons `pi pi-sign-out`. Left unchanged for human UI review.  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:100`: FontAwesome `fa fa-sign-in`. Replacement: PrimeIcons `pi pi-sign-in`. Left unchanged for human UI review.  
- 🔣 `src/main/webapp/resources/templates/template.xhtml:107`: FontAwesome `fa fa-shopping-cart`. Replacement: PrimeIcons `pi pi-shopping-cart`. Left unchanged for human UI review.  
- 📅 `src/main/webapp/shopping/createaccount.xhtml:49`: `<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button" pattern="d/M/yyyy">` (deprecated since PrimeFaces 10). Replacement: `<p:datePicker>` with `monthNavigator`/`yearNavigator` in place of `navigator`, and `showIcon="true"` in place of `showOn="button"`. Left unchanged for human UI review.  
- 📅 `src/main/webapp/shopping/updateaccount.xhtml:53`: `<p:calendar id="dateOfBirth" display="inline" navigator="true" showOn="button">`. Replacement: `<p:datePicker>`, same attribute mapping as above. Left unchanged for human UI review.  

## Kiro credit usage

💳 Kiro CLI credits used: 44.45  
