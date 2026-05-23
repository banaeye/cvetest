package com.example.vulnerable.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import javax.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val username: String = "",
    val email: String = "",
    val role: String = "USER"
)

// CVE-2021-20190: @JsonTypeInfo(use = Id.CLASS) はクラス名を JSON に埋め込む。
// ObjectMapper の enableDefaultTyping と組み合わせると、
// 攻撃者が {"@class":"com.sun.rowset.JdbcRowSetImpl",...} のような
// ガジェットチェーンを送り込んで任意コード実行が可能になる。
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
data class TypedPayload(
    val data: Any? = null
)
