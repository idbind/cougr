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
@RequestMapping("/rest/groups")
public class GroupController {

	private static final Logger LOG = LoggerFactory.getLogger(IndexController.class);

	@Autowired
	private GroupService gs;

	@Autowired
	private UserService us;

	/**
	 * *
	 *
	 * @param p
	 * @param actorString
	 * @return
	 * @throws CougrUnknownUser
	 */
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Group> getAllGroups(Principal p, @RequestParam(value = "actor", required = true) String actorString) throws CougrUnknownUser {
		User actor = us.getUserByUsername(actorString);

		List<Group> groups = gs.getAllGroups();
		if (actor.isCougrAdmin()) {  //admin can see all the groups.
			return groups;
		} else { //everyone else just gets public and private groups they're apart of.
			List<Group> newGroups = new LinkedList<>();
			groups.stream().forEach(g -> {
				if (g.isPrivateGroup()) {
					if (g.getMemberSubIssuers().contains(actor.getSubIssuer())
							|| g.getAdminSubIssuers().contains(actor.getSubIssuer())) {
						newGroups.add(g);
					}
				} else {
					newGroups.add(g);
				}
			});
			return newGroups;
		}
	}

	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group getGroupById(Principal p, @PathVariable(value = "id") Long id, @RequestParam(value = "actor", required = true) String actorString) throws CougrGroupException, CougrUnknownUser {

		User actor = us.getUserByUsername(actorString);
		Group group = gs.getGroupById(id);

		if (group.equals(group.EMPTY_GROUP)) { //group id doesn't exist.
			throw new CougrGroupException("No group with id " + id);
		}
		if (group.isPublicGroup()) { //public, just return it.
			return group;
		}
		if (actor.isCougrAdmin()) { //cougr admins can see all groups.
			return group;
		}

		//members/admins of the private group can get it's details.
		if (group.getMemberSubIssuers().contains(actor.getSubIssuer())
				|| group.getAdminSubIssuers().contains(actor.getSubIssuer())) {
			return group;
		} else { //group is private, and they dont have permissions to view it.
			throw new CougrGroupException("No group with id " + id);
		}
	}

	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/owner/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Group> getGroupsByOwner(@PathVariable Long id, Principal p, @RequestParam(value = "actor", required = true) String actorString) throws CougrUnknownUser {
		User actor = us.getUserByUsername(actorString);

		User user = us.getUserById(id);

		if (user.equals(User.EMPTY_USER)) {
			throw new CougrUnknownUser("No user for id " + id);
		}

		List<Group> groups = gs.getGroupsByOwner(user);

		//cougr admins, and the user himself can see all the groups.
		if (actor.isCougrAdmin() || actor.getSubIssuer().equals(user.getSubIssuer())) {
			return groups;
		} else { //only see public groups.
			List<Group> newGroups = new LinkedList<>();
			groups.stream().forEach(g -> { //filter private groups, unless user is a member of those groups.
				if (g.isPrivateGroup()) {
					if (g.getMemberSubIssuers().contains(actor.getSubIssuer())
							|| g.getAdminSubIssuers().contains(actor.getSubIssuer())) {
						newGroups.add(g);
					}
				} else {
					newGroups.add(g);
				}
			});
			return newGroups;
		}
	}

	/**
	 * *
	 *
	 * Returns the groups a specified user is a member of
	 *
	 * @param id
	 * @param p
	 * @return
	 * @throws CougrUnknownUser
	 */
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/member/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Group> getGroupsByMember(@PathVariable Long id, Principal p, @RequestParam(value = "actor", required = true) String actorString) throws CougrUnknownUser {
		User actor = us.getUserByUsername(actorString);

		User user = us.getUserById(id);
		if (user.equals(User.EMPTY_USER)) {
			throw new CougrUnknownUser("No user for username " + id);
		}

		List<Group> groups = gs.getGroupsByMembers(user);

		//cougr admins, and the user himself can see all the groups.
		if (actor.isCougrAdmin() || actor.getSubIssuer().equals(user.getSubIssuer())) {
			return groups;
		} else { //only see public groups.
			List<Group> newGroups = new LinkedList<>();
			groups.stream().forEach(g -> { //filter private groups, unless user is a member of those groups.
				if (g.isPrivateGroup()) {
					if (g.getMemberSubIssuers().contains(actor.getSubIssuer())
							|| g.getAdminSubIssuers().contains(actor.getSubIssuer())) {
						newGroups.add(g);
					}
				} else {
					newGroups.add(g);
				}
			});
			return newGroups;
		}
	}

	/**
	 *
	 * Creates a new group
	 *
	 * Name: New Group's name Owner: username of the new groups owner
	 * Description: Short text describing the new group isPublic: Can this group
	 * be seen by the typical user? isClosed: True = this group can be joined by
	 * any member without approval. False = approval needed to join group
	 *
	 * @param p
	 * @param json
	 * @return
	 * @throws CougrUnknownUser
	 * @throws CougrGroupException
	 */
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group addGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException {
		JsonObject object = Json.parse(json).asObject();
		String name = object.get("name").asString();
		String ownerName = object.get("owner").asString();
		String description = object.get("description").asString();
		Boolean isPublic = object.getBoolean("isPublic", true);
		Boolean isOpen = object.getBoolean("isOpen", true);

		Long parent = Long.valueOf(0);
		try {
			parent = object.getLong("parent", 0);
		} catch (Exception e) {
			System.out.println("NO PARENT");
		}

		User owner = us.getUserByUsername(ownerName);
		if (owner != null) {
			owner = us.getLoggedInUser((OIDCAuthenticationToken) p);
		}

		Group group = gs.createGroup(name, description, owner, isPublic, isOpen);
		if (parent > 0) {
			Group parentGroup = gs.getGroupById(parent);
			group = gs.addSubgroup(parentGroup, group);
		}
		return group;
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
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/member/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group addMemberToGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {

		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();

		Group group = gs.getGroupById(groupId);
		User user;
		try {
			user = us.getUserByUsername(username);
		} catch (Exception e) {
			throw new CougrGroupException("No user found with username: " + username);
		}

		if (group.isOpenGroup()) { //if it's an open group, add the user no problem.
			return gs.addMember(group, user);
		} else if (!group.isOpenGroup()) {
			return gs.addPendingMember(group, user);
		} else {
			throw new InvalidPermissionException("Cannot add " + username + " to " + group.getName());
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
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/member/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group removeMemberFromGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {

		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();

		Group group = gs.getGroupById(groupId);
		User user;
		try {
			user = us.getUserByUsername(username);
		} catch (Exception e) {
			throw new CougrGroupException("No user found with username: " + username);
		}

		return gs.removeMember(group, user);
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
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/member/pending/approve", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group approvePendingMember(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {
		User loggedInUser = us.getLoggedInUser((OIDCAuthenticationToken) p);

		JsonObject object = Json.parse(json).asObject();
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();

		Group group = gs.getGroupById(groupId);
		User user = us.getUserByUsername(username);

		if (loggedInUser.isCougrAdmin() || group.getAdmins().contains(loggedInUser)) {
			return gs.approvePendingMember(group, user);
		} else {
			throw new InvalidPermissionException(loggedInUser.getUsername() + " cannot approve " + username + " for " + group.getName() + ".");
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
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/member/pending/decline", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group declinePendingMember(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {

		JsonObject object = Json.parse(json).asObject();
		User actor = us.getUserByUsername(object.get("actor").asString());
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();

		Group group = gs.getGroupById(groupId);
		User user = us.getUserByUsername(username);

		if (actor.isCougrAdmin() || group.getAdmins().contains(actor) || actor.equals(user)) {
			return gs.declinePendingMember(group, user);
		} else {
			throw new InvalidPermissionException(actor.getUsername() + " cannot decline " + username + " for " + group.getName() + ".");
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
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/owner/promote", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group promoteUserToGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {

		JsonObject object = Json.parse(json).asObject();
		User actor = us.getUserByUsername(object.get("actor").asString());
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();

		Group group = gs.getGroupById(groupId);
		User user = us.getUserByUsername(username);

		//Only a COUGr admin or the groups owner can promote a user to group owner
		if (actor.isCougrAdmin() || group.getOwner().equals(actor)) {
			return gs.setOwner(group, user);
		} else {
			throw new InvalidPermissionException(actor.getUsername() + " cannot promote " + username + " to owner of " + group.getName() + ".");
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
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/admin/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group addAdminToGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {

		JsonObject object = Json.parse(json).asObject();
		User actor = us.getUserByUsername(object.get("actor").asString());
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();

		Group group = gs.getGroupById(groupId);
		User user = us.getUserByUsername(username);

		//only cougr admins and group admins can add a new admin to the group.
		if (actor.isCougrAdmin() || group.getAdmins().contains(actor)) {
			return gs.addAdmin(group, user);
		} else {
			throw new InvalidPermissionException(actor.getUsername() + " cannot add " + username + " as an admin to " + group.getName() + ".");
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
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/admin/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group removeAdminFromGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {

		JsonObject object = Json.parse(json).asObject();
		User actor = us.getUserByUsername(object.get("actor").asString());
		Long groupId = object.get("group_id").asLong();
		String username = object.get("username").asString();

		Group group = gs.getGroupById(groupId);
		User user = us.getUserByUsername(username);

		if (group.getOwner().equals(user)) {
			throw new InvalidPermissionException("Cannot remove group owner from the group admin list.  Change the group owner first.");
		}

		if (actor.isCougrAdmin() || group.getAdmins().contains(actor)) {
			return gs.removeAdmin(group, user);
		} else {
			throw new InvalidPermissionException(actor.getUsername() + " cannot remove " + username + " as an admin from " + group.getName() + ".");
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
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/subgroup/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group addSubGroupToGroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {

		JsonObject object = Json.parse(json).asObject();
		User actor = us.getUserByUsername(object.get("actor").asString());
		Long parentId = object.get("parent_id").asLong();
		Long childId = object.get("child_id").asLong();

		Group parent = gs.getGroupById(parentId);
		Group child = gs.getGroupById(childId);

		//only cougr admin or group admins can add subgroups to this group.
		if (actor.isCougrAdmin() || parent.getAdmins().contains(actor)) {
			return gs.addSubgroup(parent, child);
		} else {
			throw new InvalidPermissionException(actor.getUsername() + " add " + child.getName() + " as a subgroup to " + parent.getName() + ".");
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
	@PreAuthorize("#oauth2.hasScope('org.mitre.cougr.rest')")
	@RequestMapping(value = "/subgroup/remove", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Group removeSubgroup(Principal p, @RequestBody String json) throws CougrUnknownUser, CougrGroupException, InvalidPermissionException {

		JsonObject object = Json.parse(json).asObject();
		User actor = us.getUserByUsername(object.get("actor").asString());
		Group group = gs.getGroupById(object.get("group_id").asLong());

		//only cougr admin or group admins can add subgroups to this group.
		if (actor.isCougrAdmin() || group.getAdmins().contains(actor)) {
			return gs.removeSubgroup(group);
		} else {
			throw new InvalidPermissionException(actor.getUsername() + " prome " + group.getName() + " to a top level group.");
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
