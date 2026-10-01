package com.meuscanais.data.update

object VersionComparator {

    /**
     * Compares two semantic version strings (e.g. "1.10.0" and "1.9.0" or "v1.2.0").
     * Returns positive if version1 > version2, negative if version1 < version2, zero if equal.
     */
    fun compareVersions(version1: String, version2: String): Int {
        val clean1 = sanitizeVersion(version1)
        val clean2 = sanitizeVersion(version2)

        val parts1 = clean1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = clean2.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }

    fun isUpdateAvailable(currentVersion: String, remoteVersion: String): Boolean {
        return compareVersions(remoteVersion, currentVersion) > 0
    }

    private fun sanitizeVersion(version: String): String {
        return version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .takeWhile { it.isDigit() || it == '.' }
    }
}
