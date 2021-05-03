package com.logicgate.farm.repository;

import com.logicgate.farm.domain.Animal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

//  Below is an example of queries that could be used to better organize the code. Unfortunately, this would
//  require a spring database connection, which would require changing the yml, which I wasn't sure if the project constraints allowed it

//  @Query(value = "select a from Animal a where a.color = :color")
//  List<Animal> findByColor(String color);

}
