package com.blockko.app.vpn

/**
 * Hand-rolled IPv4 + UDP (de)serialization. We only need to recognize
 * outbound UDP/53 packets and to synthesize replies from the tunnel back
 * to the querying app, so this intentionally does not implement IP options,
 * fragmentation, or IPv6.
 */
object PacketUtils {

    const val PROTOCOL_UDP = 17
    private const val IPV4_HEADER_MIN_LEN = 20
    private const val UDP_HEADER_LEN = 8

    data class Udp4Packet(
        val sourceIp: ByteArray,
        val destIp: ByteArray,
        val sourcePort: Int,
        val destPort: Int,
        val payloadOffset: Int,
        val payloadLength: Int
    )

    /** Returns the parsed IPv4/UDP envelope if [buffer] is a well-formed UDP-over-IPv4 packet, else null. */
    fun parseUdp4(buffer: ByteArray, length: Int): Udp4Packet? {
        if (length < IPV4_HEADER_MIN_LEN) return null
        val versionAndIhl = buffer[0].toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return null
        val ihl = (versionAndIhl and 0x0F) * 4
        if (ihl < IPV4_HEADER_MIN_LEN || length < ihl + UDP_HEADER_LEN) return null

        val protocol = buffer[9].toInt() and 0xFF
        if (protocol != PROTOCOL_UDP) return null

        val sourceIp = buffer.copyOfRange(12, 16)
        val destIp = buffer.copyOfRange(16, 20)

        val udpOffset = ihl
        val sourcePort = ((buffer[udpOffset].toInt() and 0xFF) shl 8) or (buffer[udpOffset + 1].toInt() and 0xFF)
        val destPort = ((buffer[udpOffset + 2].toInt() and 0xFF) shl 8) or (buffer[udpOffset + 3].toInt() and 0xFF)
        val udpLength = ((buffer[udpOffset + 4].toInt() and 0xFF) shl 8) or (buffer[udpOffset + 5].toInt() and 0xFF)

        val payloadOffset = udpOffset + UDP_HEADER_LEN
        val payloadLength = (udpLength - UDP_HEADER_LEN).coerceAtLeast(0)
        if (payloadOffset + payloadLength > length) return null

        return Udp4Packet(sourceIp, destIp, sourcePort, destPort, payloadOffset, payloadLength)
    }

    /**
     * Builds a raw IPv4/UDP packet carrying [payload], addressed from
     * [sourceIp]:[sourcePort] to [destIp]:[destPort] (i.e. a reply travels
     * from the "DNS server" back to the querying app).
     */
    fun buildUdp4Packet(
        sourceIp: ByteArray,
        sourcePort: Int,
        destIp: ByteArray,
        destPort: Int,
        payload: ByteArray
    ): ByteArray {
        val totalLength = IPV4_HEADER_MIN_LEN + UDP_HEADER_LEN + payload.size
        val packet = ByteArray(totalLength)

        // IPv4 header
        packet[0] = ((4 shl 4) or 5).toByte() // version=4, IHL=5 (20 bytes, no options)
        packet[1] = 0 // DSCP/ECN
        packet[2] = (totalLength shr 8).toByte()
        packet[3] = (totalLength and 0xFF).toByte()
        packet[4] = 0; packet[5] = 0 // identification
        packet[6] = 0x40.toByte(); packet[7] = 0 // flags: don't fragment
        packet[8] = 64 // TTL
        packet[9] = PROTOCOL_UDP.toByte()
        packet[10] = 0; packet[11] = 0 // header checksum, filled below
        System.arraycopy(sourceIp, 0, packet, 12, 4)
        System.arraycopy(destIp, 0, packet, 16, 4)

        val ipChecksum = checksum(packet, 0, IPV4_HEADER_MIN_LEN)
        packet[10] = (ipChecksum shr 8).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // UDP header
        val udpOffset = IPV4_HEADER_MIN_LEN
        val udpLength = UDP_HEADER_LEN + payload.size
        packet[udpOffset] = (sourcePort shr 8).toByte()
        packet[udpOffset + 1] = (sourcePort and 0xFF).toByte()
        packet[udpOffset + 2] = (destPort shr 8).toByte()
        packet[udpOffset + 3] = (destPort and 0xFF).toByte()
        packet[udpOffset + 4] = (udpLength shr 8).toByte()
        packet[udpOffset + 5] = (udpLength and 0xFF).toByte()
        packet[udpOffset + 6] = 0 // checksum optional for IPv4, 0 = unused
        packet[udpOffset + 7] = 0

        System.arraycopy(payload, 0, packet, udpOffset + UDP_HEADER_LEN, payload.size)
        return packet
    }

    /** Standard one's-complement checksum over [length] bytes starting at [offset]. */
    private fun checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < end) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv().toInt() and 0xFFFF
    }
}
