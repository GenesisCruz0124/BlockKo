package com.blockko.app.vpn

/**
 * Minimal DNS message helpers: enough to read the queried name out of a
 * question section and to synthesize an NXDOMAIN reply that mirrors it.
 * Only the first question is inspected; that covers the overwhelming
 * majority of real-world resolver traffic.
 */
object DnsMessage {

    data class ParsedQuery(val id: Int, val name: String, val questionEndOffset: Int)

    /** Reads the transaction id and first question name. Returns null if malformed. */
    fun parseQuery(packet: ByteArray, length: Int): ParsedQuery? {
        if (length < 12) return null
        val id = ((packet[0].toInt() and 0xFF) shl 8) or (packet[1].toInt() and 0xFF)
        val qdCount = ((packet[4].toInt() and 0xFF) shl 8) or (packet[5].toInt() and 0xFF)
        if (qdCount < 1) return null

        val name = StringBuilder()
        var offset = 12
        var guard = 0
        while (offset < length && guard < 128) {
            guard++
            val labelLen = packet[offset].toInt() and 0xFF
            if (labelLen == 0) {
                offset += 1
                break
            }
            // Pointer compression should not appear in an outbound question, but guard anyway.
            if (labelLen and 0xC0 == 0xC0) return null
            offset += 1
            if (offset + labelLen > length) return null
            if (name.isNotEmpty()) name.append('.')
            name.append(String(packet, offset, labelLen, Charsets.US_ASCII))
            offset += labelLen
        }
        if (name.isEmpty()) return null
        // Skip QTYPE(2) + QCLASS(2)
        val questionEnd = offset + 4
        if (questionEnd > length) return null

        return ParsedQuery(id, name.toString().lowercase(), questionEnd)
    }

    /**
     * Builds an NXDOMAIN reply reusing the original question section verbatim,
     * as required so resolvers on the other end accept the response as matching.
     */
    fun buildNxDomainResponse(originalPacket: ByteArray, query: ParsedQuery): ByteArray {
        val questionBytes = originalPacket.copyOfRange(12, query.questionEndOffset)
        val response = ByteArray(12 + questionBytes.size)

        response[0] = (query.id shr 8).toByte()
        response[1] = (query.id and 0xFF).toByte()

        val requestFlagsByte1 = originalPacket[2]
        val rd = requestFlagsByte1.toInt() and 0x01
        response[2] = (0x80 or rd).toByte() // QR=1, Opcode=0, AA=0, TC=0, RD=echoed
        response[3] = (0x80 or 0x03).toByte() // RA=1, Z=0, RCODE=3 NXDOMAIN

        response[4] = 0x00; response[5] = 0x01 // QDCOUNT=1
        response[6] = 0x00; response[7] = 0x00 // ANCOUNT=0
        response[8] = 0x00; response[9] = 0x00 // NSCOUNT=0
        response[10] = 0x00; response[11] = 0x00 // ARCOUNT=0

        System.arraycopy(questionBytes, 0, response, 12, questionBytes.size)
        return response
    }
}
