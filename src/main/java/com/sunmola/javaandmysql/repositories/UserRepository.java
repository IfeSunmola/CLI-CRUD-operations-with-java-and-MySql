package com.sunmola.javaandmysql.repositories;

import com.sunmola.javaandmysql.models.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

}
