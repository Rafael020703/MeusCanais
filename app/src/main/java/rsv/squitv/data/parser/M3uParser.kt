package rsv.squitv.data.parser

import rsv.squitv.data.model.M3uEntry
import java.io.BufferedReader
import java.io.Reader

object M3uParser {
    fun parse(reader: Reader): List<M3uEntry> {
        val entries = mutableListOf<M3uEntry>()
        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)
        var currentEntry: M3uEntry? = null

        bufferedReader.forEachLine { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEachLine
            
            if (trimmedLine.startsWith("#EXTINF:")) {
                val name = trimmedLine.substringAfterLast(",").trim()
                val id = getValue(trimmedLine, "tvg-id")
                val logo = getValue(trimmedLine, "tvg-logo")
                val group = getValue(trimmedLine, "group-title")
                val epgId = getValue(trimmedLine, "tvg-name")

                currentEntry = M3uEntry(
                    name = name,
                    url = "",
                    id = id,
                    logo = logo,
                    group = group,
                    epgId = epgId
                )
            } else if (trimmedLine.startsWith("http") && currentEntry != null) {
                entries.add(currentEntry!!.copy(url = trimmedLine))
                currentEntry = null
            }
        }
        return entries
    }

    fun parse(content: String): List<M3uEntry> {
        return parse(content.reader())
    }

    private val valueRegexMap = mutableMapOf<String, Regex>()

    private fun getValue(line: String, key: String): String? {
        val regex = valueRegexMap.getOrPut(key) { "$key=\"(.*?)\"".toRegex() }
        return regex.find(line)?.groupValues?.get(1)
    }
}
