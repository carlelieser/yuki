package app.yuki.core.model

data class ParsedVersion(
    val release: List<Long>,
    val prerelease: List<String>?,
)

private val VERSION_PATTERN =
    Regex("""^[^0-9]*?(\d+(?:\.\d+)*)(?:[-_.]?([0-9a-z.+-]+))?$""", RegexOption.IGNORE_CASE)

private val NUMERIC_IDENTIFIER = Regex("""^\d+$""")

fun parseVersion(tag: String): ParsedVersion? {
    val match = VERSION_PATTERN.matchEntire(tag.trim()) ?: return null
    val release = match.groupValues[1].split(".").map { part -> part.toLongOrNull() ?: return null }

    val suffix = match.groups[2]?.value
    if (suffix.isNullOrEmpty()) return ParsedVersion(release, prerelease = null)

    val withoutBuild = suffix.substringBefore('+')
    if (withoutBuild.isEmpty()) return ParsedVersion(release, prerelease = null)

    return ParsedVersion(release, withoutBuild.lowercase().split("."))
}

private fun compareRelease(left: List<Long>, right: List<Long>): Int {
    val length = maxOf(left.size, right.size)

    for (index in 0 until length) {
        val difference = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
        if (difference != 0) return difference
    }

    return 0
}

private fun comparePrereleaseIdentifier(left: String, right: String): Int {
    val isLeftNumeric = NUMERIC_IDENTIFIER.matches(left)
    val isRightNumeric = NUMERIC_IDENTIFIER.matches(right)

    if (isLeftNumeric && isRightNumeric) return left.toLong().compareTo(right.toLong())
    if (isLeftNumeric) return -1
    if (isRightNumeric) return 1

    return left.compareTo(right)
}

private fun comparePrerelease(left: List<String>?, right: List<String>?): Int {
    if (left == null && right == null) return 0
    if (left == null) return 1
    if (right == null) return -1

    for (index in 0 until maxOf(left.size, right.size)) {
        val leftPart = left.getOrNull(index) ?: return -1
        val rightPart = right.getOrNull(index) ?: return 1

        val difference = comparePrereleaseIdentifier(leftPart, rightPart)
        if (difference != 0) return difference
    }

    return 0
}

fun compareVersions(left: ParsedVersion, right: ParsedVersion): Int {
    val release = compareRelease(left.release, right.release)
    return if (release == 0) comparePrerelease(left.prerelease, right.prerelease) else release
}

fun isNewerTag(candidate: String, installed: String): Boolean {
    if (candidate == installed) return false

    val parsedCandidate = parseVersion(candidate) ?: return false
    val parsedInstalled = parseVersion(installed) ?: return false

    return compareVersions(parsedCandidate, parsedInstalled) > 0
}
