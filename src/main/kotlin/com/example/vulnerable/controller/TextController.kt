package com.example.vulnerable.controller

import org.apache.commons.text.StringSubstitutor
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/text")
class TextController {

    // CVE-2022-42889 (Text4Shell / commons-text 1.9, CVSS 9.8):
    // StringSubstitutor.createInterpolator() は script: / dns: / url: の
    // ルックアップを有効にする。ユーザー入力をそのまま replace() に渡すと
    // 任意コード実行・情報漏洩が発生する。
    //
    // 攻撃例 (Content-Type: application/json):
    //   {"template": "${script:javascript:java.lang.Runtime.getRuntime().exec('id')}"}
    //   {"template": "${dns:attacker.com}"}
    //
    // 修正: commons-text 1.10.0 以上にアップグレード (script:/dns: が無効化される)、
    //       または StringSubstitutor の通常コンストラクタを使用し
    //       interpolator を使わない。
    @PostMapping("/process")
    fun processText(@RequestBody request: Map<String, String>): Map<String, String> {
        val template = request["template"] ?: ""
        val substitutor = StringSubstitutor.createInterpolator()
        substitutor.isEnableSubstitutionInVariables = true
        val result = substitutor.replace(template)
        return mapOf("result" to result)
    }

    @GetMapping("/render")
    fun renderTemplate(@RequestParam template: String): Map<String, String> {
        val substitutor = StringSubstitutor.createInterpolator()
        return mapOf("result" to substitutor.replace(template))
    }
}
