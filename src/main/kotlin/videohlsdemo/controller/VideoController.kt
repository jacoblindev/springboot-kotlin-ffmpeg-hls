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
import videohlsdemo.model.ResponseDTO
import videohlsdemo.model.TranscodeConfig
import videohlsdemo.model.VideoInfo
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
    private val videoFolder: String? = null
    private val tempDir: Path = Paths.get(System.getProperty("java.io.tmpdir"))

    @PostMapping
    fun upload(
        @RequestPart(name = "file", required = true) video: MultipartFile,
        @RequestPart(name = "config", required = true) transcodeConfig: TranscodeConfig,
    ): ResponseEntity<ResponseDTO> {
        LOG.info("檔案資訊：title={}, size={}", video.originalFilename, video.size)
        LOG.info("轉碼配置：{}", transcodeConfig)
        // 原始檔名 - 影片標題
        val title: String? = video.originalFilename
        val tempFile: Path? = title?.let { tempDir.resolve(it) }
        LOG.info("io到臨時檔案：{}", tempFile.toString())
        try {
            // 暫存影片到暫存資料夾
            tempFile?.let { video.transferTo(it) }
            // 刪除字尾
            val tempTitle: String? = title?.substring(0, title.lastIndexOf("."))
            LOG.info(tempTitle)
            // 按照日期生成子目錄 /日期/上傳檔名
            val today: String = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())
            val targetFolder: Path? = videoFolder?.let { Paths.get(it, today, tempTitle) }
            // 嘗試建立影片目錄
            if (targetFolder != null) {
                Files.createDirectories(targetFolder)
            }
            LOG.info("建立資料夾目錄：{}", targetFolder)
            // 執行轉碼操作
            LOG.info("開始轉碼")
            try {
                FFmpegUtils.transcodeToM3U8(tempFile.toString(), targetFolder.toString(), transcodeConfig)
            } catch (e: Exception) {
                LOG.error("轉碼異常：{}", e.message)
                val rs = ResponseDTO(success = false)
                rs.msg = e.message
                return ResponseEntity(rs, HttpStatus.INTERNAL_SERVER_ERROR)
            }

            val videoInfo: VideoInfo? =
                title?.let { VideoInfo(it, "/${today}/${tempTitle}/index.m3u8", "/${today}/${tempTitle}/poster.jpg") }

            LOG.info(videoInfo.toString())

            val rs = ResponseDTO(success = true)
            rs.msg = "轉碼成功"
            rs.data = videoInfo.toString()
            return ResponseEntity(rs, HttpStatus.OK)
        } catch (e: IOException) {
            throw IOException("存檔時發生異常", e)
        } finally {
            // 最終刪除暫存檔案
            if (tempFile != null) {
                Files.delete(tempFile)
            }
        }
    }
}