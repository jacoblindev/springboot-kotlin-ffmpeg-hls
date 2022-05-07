package videohlsdemo.utility

import com.google.gson.Gson
import org.apache.commons.codec.binary.Hex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import videohlsdemo.model.MediaInfo
import videohlsdemo.model.TranscodeConfig
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.NoSuchAlgorithmException
import javax.crypto.KeyGenerator

class FFmpegUtils {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(FFmpegUtils::class.java)

        // 取得該系統平台換行符號
        private val LINE_SEPARATOR: String = System.getProperty("line.separator")

        /**
         * 隨機生成16位元的 AES KEY
         */
        private fun genAesKey(): ByteArray {
            return try {
                val keyGen: KeyGenerator = KeyGenerator.getInstance("AES")
                keyGen.init(128)
                keyGen.generateKey().encoded
            } catch (e: NoSuchAlgorithmException) {
                throw NoSuchAlgorithmException("No such algorithm found")
            }
        }

        /**
         * 在指定的目錄下生成 key_info, key 檔案，返回 key_info 檔案
         */
        private fun genKeyInfo(folder: String): Path {
            try {
                // AES key
                val aesKey: ByteArray = genAesKey()
                // AES initialization vector
                val iv: String = Hex.encodeHexString(genAesKey())
                // key 檔案寫入
                val keyFile = Paths.get(folder, "key")
                Files.write(keyFile, aesKey, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                // key_info 檔案寫入
                val strBuilder: StringBuilder = StringBuilder()
                strBuilder.append("key").append(LINE_SEPARATOR)                 // M3U8 載入key檔案路徑
                strBuilder.append(keyFile.toString()).append(LINE_SEPARATOR)    // FFmpeg 載入key_info檔案路徑
                strBuilder.append(iv)                                           // ASE 向量
                val keyInfo: Path = Paths.get(folder, "key_info")
                Files.write(
                    keyInfo,
                    strBuilder.toString().toByteArray(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                )
                return keyInfo
            } catch (e: IOException) {
                throw IOException("File path is invalid : [genKeyInfo]")
            }
        }

        /**
         * 指定的目錄下生成 master index.m3u8 檔案
         */
        private fun genIndex(file: String, indexPath: String, bandWidth: String) {
            LOG.info("GenIndex: file = {}, indexPath = {}, bandWidth = {}", file, indexPath, bandWidth)
            try {
                val strBuilder: StringBuilder = StringBuilder()
                strBuilder.append("#EXTM3U").append(LINE_SEPARATOR)
                strBuilder.append("#EXT-X-STREAM-INF:BANDWIDTH=$bandWidth").append(LINE_SEPARATOR)
                strBuilder.append(indexPath)
                Files.write(
                    Paths.get(file),
                    strBuilder.toString().toByteArray(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                )
            } catch (e: IOException) {
                throw IOException("File path is invalid : [genIndex]")
            }
        }

        /**
         *  Transcode video into M3U8 format with FFmpeg
         *  Official Docs: https://ffmpeg.org/
         *  Ref: https://alfg.dev/ffmpeg-commander/
         */
        fun transcodeToM3U8(source: String, destFolder: String) {
            LOG.info("M3U8轉檔開始： source = {}, destFolder = {}", source, destFolder)
            // 判斷源影片是否存在
            if (!Files.exists(Paths.get(source))) throw IllegalArgumentException("檔案不存在：$source")
            try {
                // 建立工作目錄
                val workDir: Path = Paths.get(destFolder, "ts")
                Files.createDirectory(workDir)
                // 在工作目錄生成KeyInfo檔案
                val keyInfo: Path = genKeyInfo(workDir.toString())
                // 建構命令
                val commands: List<String> = listOf(
                    "ffmpeg",
                    "-i",
                    source,                     // 原始檔
                    "-c:v",
                    "libx264",                  // 影片編碼為 H264
                    "-c:a",
                    "copy",                     // 音訊直接 copy
                    "-hls_key_info_file",
                    keyInfo.toString(),         // 指定金鑰檔案路徑
                    "-hls_time",
                    "60",           // ts 切片大小
                    "-hls_playlist_type",
                    "vod",                      // 點播模式
                    "-hls_segment_filename",
                    "%06d.ts",                  // ts 切片檔名稱
                    "index.m3u8",               // 生成 M3U8 檔案
                )

                /**
                 *  Create a subprocess to execute the shell command for Video transcode with FFmpeg.
                 *  Ref: https://www.baeldung.com/java-lang-processbuilder-api
                 */
                val process: Process =
                    ProcessBuilder().command(commands)
                        .directory(workDir.toFile())
                        .redirectErrorStream(true)
                        .inheritIO()
                        .start()

                // 阻塞直到任務結束
                if (process.waitFor() != 0) throw RuntimeException("影片切片異常")
                // 切出封面
                if (!screenShots(source, "/${destFolder}/poster.jpg")) throw RuntimeException("封面擷取異常")
                // 獲取影片資訊
                val mediaInfo: MediaInfo = getMediaInfo(source) ?: throw RuntimeException("獲取媒體資訊異常")
                // 生成 index.m3u8 檔案
                genIndex("/${destFolder}/index.m3u8", "ts/index.m3u8", mediaInfo.format.bit_rate)
                // 刪除 keyInfo 檔案
                Files.delete(keyInfo)
            } catch (e: IOException) {
                throw IOException("File path is invalid : [transcodeToM3U8]")
            }
        }

        /**
         * 獲取影片檔案的媒體資訊
         */
        private fun getMediaInfo(source: String): MediaInfo? {
            val commands: List<String> = listOf(
                "ffprobe",
                "-i",
                source,
                "-show_format",
                "-show_streams",
                "-print_format",
                "json",
            )
            val process: Process = ProcessBuilder(commands).start()
            var mediaInfo: MediaInfo? = null
            BufferedReader(InputStreamReader(process.inputStream)).use {
                mediaInfo = Gson().fromJson(it, MediaInfo::class.java)
            }

            if (process.waitFor() != 0) return null

            return mediaInfo
        }

        /**
         * 擷取影片的指定時間幀，生成圖片檔案
         */
        private fun screenShots(source: String, file: String): Boolean {
            val commands: List<String> = listOf(
                "ffmpeg",
                "-i",
                source,
                "-ss",
                "00:00:00.001", // 擷取第1毫秒作為封面
                "-y",
                "-q:v",
                "1",
                "-frames:v",
                "1",
                "-f",
                "image2",
                file,
            )
            val process: Process = ProcessBuilder(commands).redirectErrorStream(true).inheritIO().start()

            return process.waitFor() == 0
        }

    }
}