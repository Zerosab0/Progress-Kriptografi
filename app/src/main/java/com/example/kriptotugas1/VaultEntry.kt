package com.example.kriptotugas1

data class VaultEntry(
    val id: String = "",
    val serviceName: String = "",
    val username: String = "",
    val strengthLevel: String = "",
    val fingerprintSha256: String = "",
    val createdAt: Long = 0L
)
