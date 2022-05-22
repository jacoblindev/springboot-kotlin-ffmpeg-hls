package videohlsdemo.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where
import java.sql.Timestamp
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "music_list")
@Where(clause = "del_flag = 0")
@SQLDelete(sql = "UPDATE music_list SET del_flag = 1 WHERE music_id =?")
data class MusicEntity(
    @Id
    @GeneratedValue(generator = "jpa-uuid")
    @GenericGenerator(name = "jpa-uuid", strategy = "uuid2")
    val musicId: String? = null,
    val musicName: String,
    val musicAuthor: String,
    val musicType: String,
    val musicDetail: String,
    val musicUrl: String,
    val delFlag: Int = 0,
    val createdDate: Timestamp = Timestamp(System.currentTimeMillis()),
    val modifiedDate: Timestamp = Timestamp(System.currentTimeMillis()),
)