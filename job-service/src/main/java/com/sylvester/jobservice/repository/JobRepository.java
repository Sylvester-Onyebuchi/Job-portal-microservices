package com.sylvester.jobservice.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.sylvester.jobservice.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    Optional<Job> findJobById(String id);

    Optional<Job> findJobByCompanyId(String companyId);

    Optional<Job> findJobByTitle(String title);

    boolean existsByTitle(String title);

    List<Job> findJobsByCompanyId(String companyId);


    boolean existsByCompanyIdAndTitleIgnoreCase(String companyId, String title);

    Page<Job> findByTitleContainingIgnoreCase(String title, Pageable sortedPageable);
}
