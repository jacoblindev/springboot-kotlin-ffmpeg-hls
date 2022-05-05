package videohlsdemo.model

data class MediaInfo(
    var streams: List<Stream>,
    var format: Format
)
data class Format(val bit_rate: String)
data class Stream(
    val index: Int,
    val codec_name: String,
    val codec_long_name: String,
    val profile: String,
)
