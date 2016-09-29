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
import org.mitre.cougr.dao.model.Group;
import org.mitre.cougr.dao.model.User;
import org.mitre.cougr.exception.CougrGroupException;
import org.mitre.cougr.exception.CougrUnknownUser;
import org.mitre.cougr.exception.InvalidPermissionException;
import org.mitre.cougr.service.GroupService;
import org.mitre.cougr.service.UserService;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;


@RestController
@RequestMapping("/local/groups")
@PreAuthorize("hasRole('ROLE_USER')")
public class GroupLocalController {

		    private static final Logger LOG = LoggerFactory.getLogger(IndexController.class);
	
    //TODO: THIS WHOLE FILE NEEDS GOOD UNIT TESTING FOR ADDING/REMOVING MEMBER LOGIC AND WHAT TO DISPLAY ETC.
    //TODO: Switch from using User objects for logic to just their subIssuer string for performance?
    //TODO: have some elevated privileges for cougrAdmins so they dont always act as admins and dont get all private groups etc all the time?

    @Autowired
    private GroupService gs;

    @Autowired
    private UserService us;

    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Group> getAllGroups(Principal p) throws CougrUnknownUser {
        final User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken) p);
        List<Group> groups = gs.getAllGroups();
        if(loggedInUser.isCougrAdmin()){  //admin can see all the groups.
            return groups;
        }else { //everyone else just gets public and private groups they're apart of.
            List<Group> newGroups = new LinkedList<>();
            groups.stream().forEach(g -> {
                if (g.isPrivateGroup()) {
                    if(g.getMemberSubIssuers().contains(loggedInUser.getSubIssuer()) ||
                       g.getAdminSubIssuers().contains(loggedInUser.getSubIssuer())){
                        newGroups.add(g);
                    }
                }else{
                    newGroups.add(g);
                }
            });
            return newGroups;
        }
    }
	
   @RequestMapping(value = "/app", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Group> getAllGroups() throws CougrUnknownUser {

        List<Group> groups = gs.getAllGroups();
        return groups;
        
    }

	

	@RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Group addGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException{
		JsonObject object = Json.parse(json).asObject();

		String name = object.get("name").asString();
		String ownerName = object.get("owner").asString();
		String description = object.get("description").asString();
		Boolean isPublic = object.getBoolean("isPublic", true);
		Boolean isOpen = object.getBoolean("isOpen", true);

		Long parent =  Long.valueOf(0);
		try{
			parent = object.getLong("parent", 0);
			System.out.println("PARENT ID =======" + parent);
		}catch(Exception e){
			System.out.println("NO PARENT");
		}
		
		User owner = us.getUserByUsername(ownerName);
		if(owner != null){
			owner = us.getLoggedInUser((OIDCAuthenticationToken)p);
		}

        Group group = gs.createGroup(name, description, owner, isPublic, isOpen);
		if(parent > 0){
			Group parentGroup = gs.getGroupById(parent);
			group = gs.addSubgroup(parentGroup, group);
		}
        return group;
	}
	
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Group getGroupById(Principal p, @PathVariable(value = "id") Long id) throws CougrGroupException, CougrUnknownUser {

        User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);
        Group group =  gs.getGroupById(id);

        if(group.equals(group.EMPTY_GROUP)){ //group id doesn't exist.
            throw new CougrGroupException("No group with id "+id);
        }
        if(group.isPublicGroup()){ //public, just return it.
            return group;
        }
        if(loggedInUser.isCougrAdmin()){ //cougr admins can see all groups.
            return group;
        }

        //members/admins of the private group can get it's details.
        if(group.getMemberSubIssuers().contains(loggedInUser.getSubIssuer()) ||
            group.getAdminSubIssuers().contains(loggedInUser.getSubIssuer())){
            return group;
        }else { //group is private, and they dont have permissions to view it.
            throw new CougrGroupException("No group with id " + id);
        }
    }


    @RequestMapping(value = "/owner/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Group> getGroupsByOwner(@PathVariable Long id, Principal p) throws CougrUnknownUser {
        LOG.error("owner/id");
		User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken) p);
        User user = us.getUserById(id);

        if(user.equals(User.EMPTY_USER)){
            throw new CougrUnknownUser("No user for id "+id);
        }

        List<Group> groups = gs.getGroupsByOwner(user);

        //cougr admins, and the user himself can see all the groups.
        if(loggedInUser.isCougrAdmin() || loggedInUser.getSubIssuer().equals(user.getSubIssuer())){
            return groups;
        }else{ //only see public groups.
            List<Group> newGroups = new LinkedList<>();
            groups.stream().forEach(g -> { //filter private groups, unless user is a member of those groups.
                if (g.isPrivateGroup()) {
                    if(g.getMemberSubIssuers().contains(loggedInUser.getSubIssuer()) ||
                        g.getAdminSubIssuers().contains(loggedInUser.getSubIssuer())){
                        newGroups.add(g);
                    }
                }else{
                    newGroups.add(g);
                }
            });
            return newGroups;
        }
    }
	

	/**
	 * Returns the groups a specified user is a member of
	 * 
	 * @param username - user we are finding membership info on
	 * @param p
	 * @return
	 * @throws CougrUnknownUser 
	 */
	@RequestMapping(value = "/member/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Group> getGroupsByMember(@PathVariable Long id, Principal p) throws CougrUnknownUser {
        LOG.error("member/id");
		User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken) p);
        //User user = us.getUserByUsername(username); //TODO: move this into the logic so not loading useless data from db if not needed?
		User user = us.getUserById(id);
        if(user.equals(User.EMPTY_USER)){
            throw new CougrUnknownUser("No user for username "+id);
        }

		List<Group> groups = gs.getGroupsByMembers(user);

        //cougr admins, and the user himself can see all the groups.
        if(loggedInUser.isCougrAdmin() || loggedInUser.getSubIssuer().equals(user.getSubIssuer())){
            return groups;
        }else{ //only see public groups.
            List<Group> newGroups = new LinkedList<>();
            groups.stream().forEach(g -> { //filter private groups, unless user is a member of those groups.
                if (g.isPrivateGroup()) {
                    if(g.getMemberSubIssuers().contains(loggedInUser.getSubIssuer()) ||
                        g.getAdminSubIssuers().contains(loggedInUser.getSubIssuer())){
                        newGroups.add(g);
                    }
                }else{
                    newGroups.add(g);
                }
            });
            return newGroups;
        }
    }

	/**
	 * 
	 * @param p
	 * @param json {group_id: Long, username: String}
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 * @throws InvalidPermissionException 
	 */
    @RequestMapping(value = "/member/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Group addMemberToGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException{
		
        User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);
		
		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();
		
        Group group = gs.getGroupById(groupId);
		User user;
		try{
			user = us.getUserByUsername(username);
		}catch(Exception e){
			throw new CougrGroupException("No user found with username: " + username);
		}
	
		
        if(group.isOpenGroup()){ //if it's an open group, add the user no problem.
            return gs.addMember(group, user);
        }else if(loggedInUser.isCougrAdmin() || group.getAdmins().contains(loggedInUser)) {
            return gs.addMember(group, user);
        }else if(!group.isOpenGroup()){
			return gs.addPendingMember(group, user);
		}else{
            throw new InvalidPermissionException(loggedInUser.getUsername()+" cannot add "+ username+" to "+group.getName());
        }
    }

	/**
	 * 
	 * @param p
	 * @param json {group_id: Long, username: String}
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 * @throws InvalidPermissionException 
	 */
    @RequestMapping(value = "/member/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Group removeMemberFromGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException{
        User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);

		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();
		
        Group group = gs.getGroupById(groupId);
		User user;
		try{
			user = us.getUserByUsername(username);
		}catch(Exception e){
			throw new CougrGroupException("No user found with username: " + username);
		}
		
        if(loggedInUser.equals(user)){
			//user can remove themselves from their own groups.
			return gs.removeMember(group, user);
        }else if(loggedInUser.isCougrAdmin() || group.getAdmins().contains(loggedInUser)) {
                return gs.removeMember(group, user);
        }else{
            throw new InvalidPermissionException(loggedInUser.getUsername()+" cannot remove "+username+" from "+group.getName()+".");
        }
    }
	
	/**
	 * 
	 * @param p
	 * @param json {group_id: Long, username: String}
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 * @throws InvalidPermissionException 
	 */
    @RequestMapping(value = "/member/pending/approve", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group approvePendingMember(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException{
		User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);

		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();
		
		Group group = gs.getGroupById(groupId);
        User user = us.getUserByUsername(username);

		if(loggedInUser.isCougrAdmin() || group.getAdmins().contains(loggedInUser)) { 
                return gs.approvePendingMember(group, user);
        }else{
            throw new InvalidPermissionException(loggedInUser.getUsername()+" cannot approve "+username+" for "+group.getName()+".");
        }
	}
	
	/**
	 * 
	 * @param p
	 * @param json {group_id: Long, username: String}
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 * @throws InvalidPermissionException 
	 */
	@RequestMapping(value = "/member/pending/decline", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group declinePendingMember(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException{
		User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);
		
		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();
		
		Group group = gs.getGroupById(groupId);
        User user = us.getUserByUsername(username);

		if(loggedInUser.isCougrAdmin() || group.getAdmins().contains(loggedInUser) || loggedInUser.equals(user)) { 
                return gs.declinePendingMember(group, user);
        }else{
            throw new InvalidPermissionException(loggedInUser.getUsername()+" cannot decline "+username+" for "+group.getName()+".");
        }
	}

	/**
	 * 
	 * @param p
	 * @param json {group_id: Long, username: String}
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 * @throws InvalidPermissionException 
	 */
	@RequestMapping(value = "/owner/promote", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group promoteUserToGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {
		
		User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);
		
		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();
		
		Group group = gs.getGroupById(groupId);
        User user = us.getUserByUsername(username);
		
		//Only a COUGr admin or the groups owner can promote a user to group owner
		if(loggedInUser.isCougrAdmin() || group.getOwner().equals(loggedInUser)){ 
			return gs.setOwner(group, user);
		}else{
			 throw new InvalidPermissionException(loggedInUser.getUsername()+" cannot promote "+username+" to owner of "+group.getName()+".");
		}
		
	}
	
	
    /**
	 * 
	 * @param p
	 * @param json {group_id: Long, username: String}
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 * @throws InvalidPermissionException 
	 */
    @RequestMapping(value = "/admin/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Group addAdminToGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {

        User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);
		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();
		
        Group group = gs.getGroupById(groupId);
        User user = us.getUserByUsername(username);

        //only cougr admins and group admins can add a new admin to the group.
        if(loggedInUser.isCougrAdmin() || group.getAdmins().contains(loggedInUser)) {
            return gs.addAdmin(group, user);
        }else{
            throw new InvalidPermissionException(loggedInUser.getUsername()+" cannot add "+username+" as an admin to "+group.getName()+".");
        }
    }

	/**
	 * 
	 * @param p
	 * @param json {group_id: Long, username: String}
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 * @throws InvalidPermissionException 
	 */
    @RequestMapping(value = "/admin/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Group removeAdminFromGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException{

        User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);
		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();
		
        Group group = gs.getGroupById(groupId);
        User user = us.getUserByUsername(username);


        if(group.getOwner().equals(user)){
            throw new InvalidPermissionException("Cannot remove group owner from the group admin list.  Change the group owner first.");
        }

        if(loggedInUser.isCougrAdmin() || group.getAdmins().contains(loggedInUser)) {
            return gs.removeAdmin(group, user);
        }else{
            throw new InvalidPermissionException(loggedInUser.getUsername()+" cannot remove "+username+" as an admin from "+group.getName()+".");
        }
    }

	/**
	 * 
	 * @param p
	 * @param json {parent_id: Long, child_id: Long}
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 * @throws InvalidPermissionException 
	 */
    @RequestMapping(value = "/subgroup/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Group addSubGroupToGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException{

        User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);
		
		JsonObject object = Json.parse(json).asObject();
		Long parentId = object.get("parent_id").asLong();
		Long childId = object.get("child_id").asLong();
		
        Group parent = gs.getGroupById(parentId);
        Group child = gs.getGroupById(childId);

        //only cougr admin or group admins can add subgroups to this group.
        if(loggedInUser.isCougrAdmin() || parent.getAdmins().contains(loggedInUser)){
            return gs.addSubgroup(parent, child);
        }else{
            throw new InvalidPermissionException(loggedInUser.getUsername()+" add "+child.getName()+" as a subgroup to "+parent.getName()+".");
        }
    }
	
	/**
	 * 
	 * @param p
	 * @param json {group_id: Long}
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 * @throws InvalidPermissionException 
	 */
	@RequestMapping(value = "/subgroup/remove", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Group removeSubgroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException{

        User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken)p);
		JsonObject object = Json.parse(json).asObject();
        Group group = gs.getGroupById(object.get("group_id").asLong());

        //only cougr admin or group admins can add subgroups to this group.
        if(loggedInUser.isCougrAdmin() || group.getAdmins().contains(loggedInUser)){
            return gs.removeSubgroup(group);
        }else{
            throw new InvalidPermissionException(loggedInUser.getUsername()+" prome "+group.getName()+" to a top level group.");
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
//
//    @ExceptionHandler(NullPointerException.class)
//    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
//    @ResponseBody
//    public CougrError handleGroupExceptions(NullPointerException npe) {
//        CougrError error = new CougrError(npe);
//        return error;
//    }

}



