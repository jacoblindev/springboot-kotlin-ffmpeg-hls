package videohlsdemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class VideohlsdemoApplication

fun main(args: Array<String>) {
	runApplication<VideohlsdemoApplication>(*args)
}
