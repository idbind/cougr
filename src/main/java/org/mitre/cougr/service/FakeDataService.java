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
package org.mitre.cougr.service;

import com.google.common.collect.ImmutableList;
import org.mitre.cougr.dao.model.Group;
import org.mitre.cougr.dao.model.User;
import org.mitre.cougr.exception.CougrGroupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class FakeDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FakeDataService.class);

    @Autowired
    private GroupService gs;

    @Autowired
    private UserService us;

    @PostConstruct
    public void loadData(){
        LOGGER.debug("Loading fake data.");
        List<User> users = this.getUsers();

        //add user to db
        users.stream().forEach(user -> {
            try {
                us.addUser(user);
                LOGGER.warn("created user "+user.getUsername());
            }catch(Exception e){
                LOGGER.warn("error adding user: "+user.getUsername()+ " "+e.getMessage());
            }
        });

        User admin = us.getUserByUsername("admin");
        us.setCougrAdmin(admin, true);

		
		User user = us.getUserByUsername("user");
		
		List<Group> groups = this.getGroups();
		//add user to db
        groups.stream().forEach(group -> {
            try {
				
				group.setOwner(user);
                gs.addGroup(group);
                LOGGER.warn("created user "+group.getName());
            }catch(Exception e){
                LOGGER.warn("error adding group: "+group.getName()+ " "+e.getMessage());
            }
        });

        LOGGER.debug("Fake data loaded");
    }

    // hardcoded list of local COUGr user entities. 
    private List<User> getUsers(){
        return ImmutableList.of(
    		new User("user", "user@localhost", "user", "user", "90342.ASDFJWFA", "http://localhost:8080/openid-connect-server-webapp/"),
			new User("admin", "admin@localhost", "admin", "admin", "01921.FLANRJQW", "http://localhost:8080/openid-connect-server-webapp/")
        );
    }
	
	private List<Group> getGroups(){
        return ImmutableList.of(
            new Group("Department ABC", "Department members of ABC", true, true),
			new Group("OIDC devs", "developers using Open ID Connect technologies", true, true),
			new Group("Cat lovers", "people who love cats", true, true)
        );
    }
}
