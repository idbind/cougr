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

import org.mitre.cougr.dao.model.Group;
import org.mitre.cougr.dao.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("groupDao")
public interface GroupDAO extends CrudRepository<Group, Long>{

    Group findByName(String name);
    List<Group> findAll();
    List<Group> findByOwner(User owner);
	List<Group> findByMembers(User members);
    List<Group> findByPublicGroup(Boolean isPublic);


    //List<Group> findByMember(User member);
    //List<Group> findByAdminr(User member);
    //List<User> findByFirstnameOrLastname(@Param("name") String name);
    //List<User> findByLastnameContaining(String partialLastname);
}
