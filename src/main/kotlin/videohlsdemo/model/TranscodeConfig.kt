package videohlsdemo.model

data class TranscodeConfig(
    val poster: String,      // 擷取封面的時間 HH:mm:ss.[SSS]
    val tsSeconds: String,   // ts分片大小，單位是秒
    val cutStart: String,    // 影片裁剪，開始時間	HH:mm:ss.[SSS]
    val cutEnd: String,      // 影片裁剪，結束時間	HH:mm:ss.[SSS]
)
