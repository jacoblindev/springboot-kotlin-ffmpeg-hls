package videohlsdemo.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import videohlsdemo.model.Video

@Repository
interface VideoRepository : JpaRepository<Video, String>