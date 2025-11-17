package au.edu.rmit.sept.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import au.edu.rmit.sept.webapp.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // findAll() method is inherited from JpaRepository
    // no additional custom methods needed for Task 1.2
}