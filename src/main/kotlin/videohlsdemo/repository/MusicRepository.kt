package videohlsdemo.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import videohlsdemo.model.MusicEntity

@Repository
interface MusicRepository : JpaRepository<MusicEntity, String> {
}