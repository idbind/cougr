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
package org.mitre.cougr.controller;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.mitre.cougr.dao.model.User;
import org.mitre.cougr.exception.CougrGroupException;
import org.mitre.cougr.exception.CougrUnknownUser;
import org.mitre.cougr.exception.InvalidPermissionException;
import org.mitre.cougr.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;


@RestController
@RequestMapping("/rest/users")
public class UserController {

    @Autowired
    private UserService userService;

	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
    @RequestMapping(value = {"/", ""}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<User> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return users;
    }

	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public User getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        //return gson.toJson(users);
        return user;
    }
	
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
    @RequestMapping(value = "/cougr_admin", method = RequestMethod.POST)
    public User makeCougrAdmin(Principal p, @RequestBody String json) throws InvalidPermissionException, CougrUnknownUser {

		JsonObject object = Json.parse(json).asObject();
		User actor = userService.getUserByUsername(object.get("actor").asString());
        
		Long id = object.get("user_id").asLong();
        User user = userService.getUserById(id);
        if(user.equals(User.EMPTY_USER)){
            throw new CougrUnknownUser("No User for id "+id);
        }
        if(actor.isCougrAdmin()){
            return userService.setCougrAdmin(user, true);
        }else{
            throw new InvalidPermissionException(actor.getUsername()+" cannot perform this action.");
        }
    }
	
	
	@RequestMapping(value = "/cougr_admin/remove", method = RequestMethod.POST)
    public User removeCougrAdmin(Principal p, @RequestBody String json) throws InvalidPermissionException, CougrUnknownUser {

		JsonObject object = Json.parse(json).asObject();
		User actor = userService.getUserByUsername(object.get("actor").asString());
        Long id = object.get("user_id").asLong();
        User user = userService.getUserById(id);
        if(user.equals(User.EMPTY_USER)){
            throw new CougrUnknownUser("No User for id "+id);
        }
        if(actor.isCougrAdmin()){
            return userService.setCougrAdmin(user, false);
        }else{
            throw new InvalidPermissionException(actor.getUsername()+" cannot perform this action.");
        }
    }

    private String error(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("{'error': \"");
        sb.append(throwable.getMessage());
        sb.append("\"}");
        return sb.toString();
    }

    @ExceptionHandler(value = {Exception.class,
            CougrGroupException.class,
            InvalidPermissionException.class,
            CougrUnknownUser.class,
            NullPointerException.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public CougrError handleGroupExceptions(Exception ex) {
        CougrError error = new CougrError(ex);
        return error;
    }

}


