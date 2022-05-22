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
import videohlsdemo.model.UploadReq
import videohlsdemo.model.UploadRes
import videohlsdemo.utility.FFmpegUtils
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@RestController
@RequestMapping("/audio")
class MusicController {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(MusicController::class.java)
    }

    @Value("\${app.audio-folder}")
    lateinit var audioFolder: String

    @Value("\${server.port}")
    lateinit var serverPort: String

    private val tempDir: Path = Paths.get(System.getProperty("java.io.tmpdir"))

    @PostMapping("/upload")
    fun musicUpload(formData: UploadReq): ResponseEntity<UploadRes> {
        LOG.info("[Request]: ${formData.musicName}, ${formData.musicAuthor}, ${formData.musicType}, ${formData.musicDetail}")
        LOG.info("[檔案資訊]：title={}, size={}", formData.musicFile.originalFilename, formData.musicFile.size)
        val res = UploadRes("Music")
        return ResponseEntity.ok().body(res)
    }

    @PostMapping
    fun upload(
        @RequestPart(name = "file", required = true) audio: MultipartFile
    ): ResponseEntity<MutableMap<String, Any>> {
        LOG.info("檔案資訊：title={}, size={}", audio.originalFilename, audio.size)
        // 原始檔名 - 影片標題
        val title: String = audio.originalFilename ?: "TempFile"
        LOG.info("原始檔名：{}", title)
        val tempFile: Path = tempDir.resolve(title)
        LOG.info("臨時檔案夾：{}", tempFile.toString())
        try {
            // 暫存影片到暫存資料夾
            audio.transferTo(tempFile)
            // 刪除字尾 extension
            val tempTitle: String = title.substring(0, title.lastIndexOf("."))
            LOG.info(tempTitle)
            // 按照日期生成子目錄 /日期/上傳檔名
            val today: String = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())
            val targetFolder: Path = Paths.get(audioFolder, today, tempTitle)
            // 嘗試建立影片目錄 TODO: do I need to consider exists folder to be overwrites???
            // 檢查工作目錄是否已存在
            if (Files.exists(Paths.get(targetFolder.toUri()))) throw IllegalArgumentException("檔案已存在：${targetFolder}")
            Files.createDirectory(targetFolder)
            LOG.info("建立資料夾目錄：{}", targetFolder)
            // 執行轉碼操作
            LOG.info("開始轉碼")
            try {
                FFmpegUtils.audioTranscodeToM3U8(tempFile.toString(), targetFolder.toString())
            } catch (e: Exception) {
                LOG.error("轉碼異常：{}", e.message)
                val result: MutableMap<String, Any> = HashMap()
                result["success"] = true
                result["msg"] = e.message.toString()
                return ResponseEntity(result, HttpStatus.INTERNAL_SERVER_ERROR)
            }

            // 封裝結果
            val audioInfo: MutableMap<String, Any> = HashMap()
            audioInfo["title"] = title
            audioInfo["m3u8"] = java.lang.String.join("/", "", today, tempTitle, "index.m3u8")

            val result: MutableMap<String, Any> = HashMap()
            result["success"] = true
            result["data"] = audioInfo

            val ipAddr: String
            try {
                ipAddr = InetAddress.getLocalHost().hostAddress
                LOG.info("[HOSTNAME]: $ipAddr:$serverPort")
            } catch (e: UnknownHostException) {
                LOG.error("Unknown Host???")
            }

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