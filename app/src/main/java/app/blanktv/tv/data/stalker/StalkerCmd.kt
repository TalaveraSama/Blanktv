package app.blanktv.tv.data.stalker

/**
 * Normalises a Ministra `cmd` field into a playable URL.
 *
 * Portals store the transport in front of the address (`ffmpeg http://…`, `ffrt http://…`, and on
 * newer firmware `libretro`/`dvb`/`radio`). Feeding the raw string to a player fails instantly, so
 * the prefix is stripped once, case-insensitively, and only at the start.
 */
object StalkerCmd {

    private val PREFIXES = listOf("ffmpeg ", "ffrt ", "libretro ", "dvb ", "radio ", "auto ")

    fun normalize(cmd: String): String {
        var value = cmd.trim()
        for (prefix in PREFIXES) {
            if (value.startsWith(prefix, ignoreCase = true)) {
                value = value.substring(prefix.length)
                break
            }
        }
        return value.trim()
    }
}
