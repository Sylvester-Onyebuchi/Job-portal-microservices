package com.sylvester.applicationservice.repository;


import com.sylvester.applicationservice.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, String> {

    boolean existsByJobIdAndCandidateUserId(String jobId, String candidateUserId);

    Optional<Application> findApplicationByCandidateUserId(String candidateUserId);

    Optional<Application> findApplicationById(String id);


    List<Application> findAllByCandidateUserId(String candidateUserId);
}
