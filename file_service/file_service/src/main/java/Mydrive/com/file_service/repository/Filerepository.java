package Mydrive.com.file_service.repository;

import Mydrive.com.file_service.model.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Filerepository extends JpaRepository<Files,Long> {
    List<Files> findByUseridAndIsDeletedFalse(Long userid);
    Optional<Files> findByIdAndUseridAndIsDeletedFalse(Long id, Long userid);
    boolean existsByUseridAndFilenameAndIsDeletedFalse(Long userid, String filename);
}
