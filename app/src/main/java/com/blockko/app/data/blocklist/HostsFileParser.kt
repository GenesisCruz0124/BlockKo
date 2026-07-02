package com.blockko.app.data.blocklist

import java.io.BufferedReader

/**
 * Parses hosts-file formatted blocklists (StevenBlack-style):
 * "0.0.0.0 ads.example.com" or "127.0.0.1 ads.example.com" per line.
 * Loopback/localhost bookkeeping entries are skipped.
 */
object HostsFileParser {

    private val IGNORED_HOSTS = setOf(
        "localhost", "localhost.localdomain", "local", "broadcasthost",
        "ip6-localhost", "ip6-loopback", "ip6-localnet", "ip6-mcastprefix",
        "ip6-allnodes", "ip6-allrouters", "ip6-allhosts", "0.0.0.0"
    )

    private val BLOCKING_PREFIXES = listOf("0.0.0.0 ", "0.0.0.0\t", "127.0.0.1 ", "127.0.0.1\t")

    fun parseLine(rawLine: String): String? {
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return null

        val withoutPrefix = BLOCKING_PREFIXES.firstOrNull { line.startsWith(it) }
            ?.let { line.removePrefix(it).trim() }
            ?: return null

        val domain = withoutPrefix.substringBefore('#').trim()
            .substringBefore(' ')
            .substringBefore('\t')
            .lowercase()

        if (domain.isEmpty() || domain in IGNORED_HOSTS) return null
        if (!domain.contains('.')) return null
        return domain
    }

    fun parse(reader: BufferedReader): Set<String> {
        val domains = HashSet<String>()
        reader.forEachLine { line ->
            parseLine(line)?.let { domains.add(it) }
        }
        return domains
    }
}
