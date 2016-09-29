/*******************************************************************************
 * Copyright 2016 The MITRE Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.mitre.cougr.dao;

import org.mitre.cougr.dao.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by bkeyes on 1/13/15.
 */
@Repository("userDao")
public interface UserDAO extends CrudRepository<User, Long>{

    User findByUsername(String username);
    User findBySubAndIssuer(String sub, String issuer);

    List<User> findAll();
    List<User> findByLastname(String lastName);
    List<User> findByFirstname(String firstName);

    List<User> findByUsernameIn(List<String> usernameList);
    List<User> findByFirstnameIn(List<String> firstnameList);
    List<User> findByLastnameIn(List<String> lastnameList);

    //List<User> findByFirstnameOrLastname(@Param("name") String name);
    //List<User> findByLastnameContaining(String partialLastname);
}
