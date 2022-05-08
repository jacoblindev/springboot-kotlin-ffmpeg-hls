package videohlsdemo.model

import org.hibernate.annotations.GenericGenerator
import java.util.UUID
import javax.persistence.*

@Entity
@Table(name = "videos")
data class Video(
    @Id
    @GeneratedValue(generator = "jpa-uuid")
    @GenericGenerator(name = "jpa-uuid", strategy = "uuid")
    val id: String? = null,
    val title: String,
    val poster: String,
    val playlist: String,
)