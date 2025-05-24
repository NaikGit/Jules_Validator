package com.example.validation_service.repository;

import com.example.validation_service.model.document.ValidationFailureLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidationFailureLogRepository extends MongoRepository<ValidationFailureLog, String> {
    // Custom query methods can be defined here if needed
}
