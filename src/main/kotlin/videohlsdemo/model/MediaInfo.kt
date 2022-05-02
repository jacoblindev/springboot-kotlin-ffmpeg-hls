package videohlsdemo.model

import com.fasterxml.jackson.annotation.JsonProperty

data class MediaInfo(
    var streams: List<Stream>,
    var format: Format
) {
    companion object {
        data class Format(@JsonProperty("bit_rate")val bitRate: String)
        data class Stream(
            val index: Int,
            @JsonProperty("codec_name") val codecName: String,
            @JsonProperty("codec_long_name") val codecLongName: String,
            val profile: String,
        )
    }
}
