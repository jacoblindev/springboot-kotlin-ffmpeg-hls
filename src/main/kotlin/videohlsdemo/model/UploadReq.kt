package videohlsdemo.model

import org.springframework.web.multipart.MultipartFile

data class UploadReq(
    val musicName: String,
    val musicAuthor: String,
    val musicType: String,
    val musicDetail: String,
    val musicFile: MultipartFile,
)
