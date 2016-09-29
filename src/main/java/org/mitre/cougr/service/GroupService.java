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

import com.google.common.base.Strings;
import org.mitre.cougr.dao.GroupDAO;
import org.mitre.cougr.dao.UserDAO;
import org.mitre.cougr.dao.model.Group;
import org.mitre.cougr.dao.model.User;
import org.mitre.cougr.exception.CougrGroupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@Service
public class GroupService {

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private UserDAO userDAO;

    public Group createGroup(String name, String description, String ownerUsername, Boolean isPublic, Boolean isOpen) throws CougrGroupException{
        User owner = userDAO.findByUsername(ownerUsername);
        return this.createGroup(name, description, owner, isPublic, isOpen);
    }
	
    @Transactional
    public Group createGroup(String name, String description, User owner, Boolean isPublic, Boolean isOpen) throws CougrGroupException{

        if(Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Name must be defined.");
        }
        if(Strings.isNullOrEmpty(description)) {
            throw new IllegalArgumentException("Description must be defined");
        }

        if (owner == null || owner.equals(User.EMPTY_USER)) {
            throw new IllegalArgumentException("Owner must be set and cannot be null or empty");
        }
        if(isPublic == null){
            throw new IllegalArgumentException("isPublic cannot be null");
        }
		if(isOpen == null){
            throw new IllegalArgumentException("isOpen cannot be null");
        }

        Group a = groupDAO.findByName(name);
        if(a != null){
            throw new CougrGroupException("Group "+name+" already exists.");
        }

        Group group = new Group();
        group.setName(name);
        group.setOwner(owner);
        group.addAdmin(owner);
        group.addMember(owner);
        group.setDescription(description);
        group.setPublicGroup(isPublic);
        group.setOpenGroup(isOpen);
        return groupDAO.save(group);
    }

    public Group createSubgroup(String name, String description, Group parent) throws CougrGroupException{
        return this.createSubgroup(name, description, parent.getPublicGroup(), parent.getOpenGroup(), parent);
    }

    @Transactional
    public Group createSubgroup(String name, String description, Boolean isPublic, Boolean isOpen, Group parent) throws CougrGroupException{

        if(parent.isPrivateGroup() && isPublic.equals(Boolean.TRUE)) {
            throw new IllegalArgumentException("Subgroup cannot be public if parent group is private.");
        }

        Group group = this.createGroup(name, description, parent.getOwner(), isPublic, isOpen);
        group.setParent(parent);
        return this.addSubgroup(parent, group);
    }

    @Transactional
    public Group setOwner(Group group, User user) throws CougrGroupException{
        group.setOwner(user);
        group.addAdmin(user); //make sure new owner is an admin.
        group.addMember(user); //make sure owner is also a member
        return groupDAO.save(group);
    }

    @Transactional
    public Group addAdmin(Group group, User user) throws CougrGroupException {
        group.addAdmin(user);
        return groupDAO.save(group);
    }

    @Transactional
    public Group addAdmins(Group group, List<User> users) throws CougrGroupException{
        group.addAdmins(users);
        return groupDAO.save(group);
    }

    public Group addAdminsByUsernames(Group group, List<String> usernames) throws CougrGroupException{
        List<User> users = userDAO.findByUsernameIn(usernames);
        return this.addAdmins(group, users);
    }

    @Transactional
    public Group removeAdmin(Group group, User user) throws CougrGroupException{
        if(user.equals(group.getOwner())){
            throw new IllegalArgumentException("Cannot remove owner from Admin list.");
        }
        group.removeAdmin(user);
        return groupDAO.save(group);
    }

    @Transactional
    public Group removeAdmins(Group group, List<User> users) throws CougrGroupException{
        group.removeAdmins(users);
        return groupDAO.save(group);
    }

    @Transactional
    public Group removeAdminsByUsernames(Group group, List<String> usernames) throws CougrGroupException{
        List<User> users = userDAO.findByUsernameIn(usernames);
        return this.removeAdmins(group, users);
    }

    @Transactional
    public Group addMember(Group group, User user) throws CougrGroupException{
        group.addMember(user);
        return groupDAO.save(group);
    }

    @Transactional
    public Group addMembers(Group group, List<User> users) throws CougrGroupException{
        group.addMembers(users);
        return groupDAO.save(group);
    }

    @Transactional
    public Group addMembersByUsernames(Group group, List<String> usernames) throws CougrGroupException{
        List<User> users = userDAO.findByUsernameIn(usernames);
        return this.addMembers(group, users);
    }

    @Transactional
    public Group removeMember(Group group, User user) throws CougrGroupException{
        if(user.equals(group.getOwner())){
            throw new CougrGroupException("Cannot remove owner from Member list.");
        }
        group.removeMember(user);
        return groupDAO.save(group);
    }

    @Transactional
    public Group removeMembers(Group group, List<User> users) throws CougrGroupException{
        group.removeMembers(users);
        return groupDAO.save(group);
    }

    @Transactional
    public Group removeMembersByUsernames(Group group, List<String> usernames) throws CougrGroupException{
        List<User> users = userDAO.findByUsernameIn(usernames);
        return this.removeMembers(group, users);
    }

    @Transactional
    public Group setGroupName(Group group, String name) throws CougrGroupException{
        group.setName(name);
        return groupDAO.save(group);
    }
	@Transactional
	public Group addPendingMember(Group group, User user) throws CougrGroupException{
		group.addPendingMember(user);
		return groupDAO.save(group);
	}
	
	@Transactional
	public Group approvePendingMember(Group group, User user) throws CougrGroupException{
		group.approvePendingMember(user);
		return groupDAO.save(group);
	}
	
	@Transactional
	public Group approvePendingMembers(Group group, List<User> users) throws CougrGroupException{
		group.approvePendingMembers(users);
		return groupDAO.save(group);
	}
	
	@Transactional
	public Group declinePendingMember(Group group, User user) throws CougrGroupException{
		group.removePendingMember(user);
		return groupDAO.save(group);
	}

	@Transactional
	public Group declinePendingMembers(Group group, List<User> users) throws CougrGroupException{
		group.removePendingMembers(users);
		return groupDAO.save(group);
	}
	
    @Transactional
    public void deleteGroup(Group group) throws CougrGroupException{
        //check if it's a subgroup, if it is, remove it from it's parent's subgroup list.  not totally sure if this is needed.
        if(!group.parent.equals(Group.EMPTY_GROUP)){
            group.parent.removeSubgroup(group);
        }
        groupDAO.delete(group);
    }

    @Transactional
    public Group setPublicState(Group group, Boolean isPublic) throws CougrGroupException{
        group.setPublicGroup(isPublic);
        return groupDAO.save(group);
    }


    @Transactional
    public Group setPrivate(Group group) throws CougrGroupException {
        return this.setPublicState(group, Boolean.FALSE);
    }

    @Transactional
    public Group setPublic(Group group) throws CougrGroupException {
        return this.setPublicState(group, Boolean.TRUE);
    }

    @Transactional
    public Group setOpenState(Group group, Boolean isOpen) throws CougrGroupException{
        group.setPublicGroup(isOpen);
        return groupDAO.save(group);
    }

    @Transactional
    public Group setOpen(Group group) throws CougrGroupException {
        return this.setOpenState(group, Boolean.TRUE);
    }

    @Transactional
    public Group setClosed(Group group) throws CougrGroupException {
        return this.setOpenState(group, Boolean.FALSE);
    }

    public List<Group> getAllGroups(){
        return groupDAO.findAll();
    }

    public Group getGroupByName(String name) {
        return groupDAO.findByName(name);
    }

    public List<Group> getGroupsByOwner(User owner) {
        return groupDAO.findByOwner(owner);
    }

    public List<Group> getGroupsByOwner(String ownerUsername) {
        User user = userDAO.findByUsername(ownerUsername);
        return groupDAO.findByOwner(user);
    }
	
    public List<Group> getGroupsByMembers(User owner) {
        return groupDAO.findByMembers(owner);
    }

    public List<Group> getGroupsByMembers(String ownerUsername) {
        User user = userDAO.findByUsername(ownerUsername);
        return groupDAO.findByMembers(user);
    }

    public Group getGroupById(Long id) {
        Group group = groupDAO.findOne(id);
        if(group == null){
            return group.EMPTY_GROUP;
        }else{
            return group;
        }
    }
	
	public Group addGroup(Group group) {
        return groupDAO.save(group);
    }

    public List<Group> getGroupsById(List<Long> ids) {
        Iterable<Group> groups = groupDAO.findAll(ids);
        List<Group> x = new ArrayList<>();

        Stream<Group> stream =  StreamSupport.stream(groups.spliterator(), false);
        stream.forEach(g -> {
            if(g == null) {
                x.add(Group.EMPTY_GROUP);
            }else{
                x.add(g);
            }
        });
        return x;
    }

    /**
     *
     * @param parent
     * @param subGroup
     * @return Subgroup that was added
     */
    @Transactional
    public Group addSubgroup(Group parent, Group subGroup) throws CougrGroupException{
        parent.addSubgroup(subGroup);
        subGroup.setParent(parent);
        subGroup = groupDAO.save(subGroup);
        parent = groupDAO.save(parent);
        return subGroup;
    }

	@Transactional
    public Group removeSubgroup(Group group) throws CougrGroupException{
		Group parent = group.parent;
		parent.removeSubgroup(group);
		groupDAO.save(parent);
		group.parent = null;
        group = groupDAO.save(group);
        
        return group;
    }
	
    public boolean isSubgroup(Group group) {
        return !group.parent.equals(Group.EMPTY_GROUP);
    }

    public List<Group> getPublicGroups(){
        List<Group> groups = groupDAO.findByPublicGroup(true);
        return groups;
    }

    public List<Group> getPrivateGroups() {
        List<Group> groups = groupDAO.findByPublicGroup(false);
        return groups;
    }

}
