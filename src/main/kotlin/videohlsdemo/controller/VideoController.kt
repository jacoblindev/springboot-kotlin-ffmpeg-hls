package videohlsdemo.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import videohlsdemo.utility.FFmpegUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/video")
class VideoController {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(VideoController::class.java)
    }

    @Value("\${app.video-folder}")
    lateinit var videoFolder: String
    private val tempDir: Path = Paths.get(System.getProperty("java.io.tmpdir"))

    @PostMapping
    fun upload(
        @RequestPart(name = "file", required = true) video: MultipartFile
    ): ResponseEntity<MutableMap<String, Any>> {
        LOG.info("檔案資訊：title={}, size={}", video.originalFilename, video.size)
//        LOG.info("轉碼配置：{}", transcodeConfig)
        // 原始檔名 - 影片標題
        val title: String = video.originalFilename ?: "TempFile"
        LOG.info("原始檔名：{}", title)
        val tempFile: Path = tempDir.resolve(title)
        LOG.info("臨時檔案夾：{}", tempFile.toString())
        try {
            // 暫存影片到暫存資料夾
            video.transferTo(tempFile)
            // 刪除字尾
            val tempTitle: String = title.substring(0, title.lastIndexOf("."))
            LOG.info(tempTitle)
            // 按照日期生成子目錄 /日期/上傳檔名
            val today: String = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())
            val targetFolder: Path = Paths.get(videoFolder, today, tempTitle)
            // 嘗試建立影片目錄 TODO: do I need to consider exists folder to be overwrites???
            Files.createDirectories(targetFolder)
            LOG.info("建立資料夾目錄：{}", targetFolder)
            // 執行轉碼操作
            LOG.info("開始轉碼")
            try {
                FFmpegUtils.transcodeToM3U8(tempFile.toString(), targetFolder.toString())
            } catch (e: Exception) {
                LOG.error("轉碼異常：{}", e.message)
//                val rs = ResponseDTO(success = false)
//                rs.msg = e.message
                val result: MutableMap<String, Any> = HashMap()
                result["success"] = true
                result["msg"] = e.message.toString()
                return ResponseEntity(result, HttpStatus.INTERNAL_SERVER_ERROR)
            }

            // 封裝結果
            val videoInfo: MutableMap<String, Any> = HashMap()
            videoInfo["title"] = title
            videoInfo["m3u8"] = java.lang.String.join("/", "", today, tempTitle, "index.m3u8")
            videoInfo["poster"] = java.lang.String.join("/", "", today, tempTitle, "poster.jpg")

            LOG.info(videoInfo.toString())

            val result: MutableMap<String, Any> = HashMap()
            result["success"] = true
            result["data"] = videoInfo

            LOG.info(result.toString())
            return ResponseEntity(result, HttpStatus.OK)
        } catch (e: IOException) {
            throw IOException("存檔時發生異常", e)
        } finally {
            // 最終刪除暫存檔案
            Files.delete(tempFile)
        }
    }
}