val colorTruncationRegex = Regex(
    listOf("black", "white", "green", "grey", "gray", "brown", "red", "cream", "blue", "yellow", "pink", "purple", "orange")
        .joinToString("|", prefix = "(?i)\\b(", postfix = ")\\b\\s*[)\\]]?")
)

val raw = "MEFABZ Shoe Stand Cover 4 Layer (Cloth)(30D x 60W x 74H cm) (Red Color)"
var cleaned = raw.replace(Regex("\\s+"), " ").trim()
val colorMatch = colorTruncationRegex.find(cleaned)
if (colorMatch != null) {
    cleaned = cleaned.substring(0, colorMatch.range.last + 1).trim()
}
println("Cleaned: >>>\$cleaned<<<")
