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
package org.mitre.cougr.dao.model;

import com.fasterxml.jackson.annotation.*;
import com.google.common.collect.ImmutableSet;
import org.mitre.cougr.exception.CougrGroupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Created by bkeyes on 1/13/15.
 */
@Entity
@Table(name = "CougrGroup")
@JsonIgnoreProperties({"new"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME , property = "class")
public class Group extends AbstractPersistable<Long> {
    @Transient
    @JsonIgnore
    private static final Logger LOG = LoggerFactory.getLogger(Group.class);

    @JsonIgnore
    public static final Group EMPTY_GROUP = new Group();

    @Column(unique = false, length = 255)
    private String name;

    @Column(length = 10000)
    private String description;

    @JsonIgnoreProperties({"email", "firstname", "lastname", "sub","issuer"})
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToMany(fetch = FetchType.EAGER)
    //only be eager for now.  if it gets large, might need to change to lazy and fix the contextloaderlistener issues.
    @JsonIgnoreProperties({"email", "firstname", "lastname", "sub","issuer"})
    private Set<User> admins;


    @ManyToMany(fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"email", "firstname", "lastname", "sub","issuer"})
    private Set<User> members;

	@ManyToMany(fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"email", "firstname", "lastname", "sub","issuer"})
    private Set<User> pendingMembers;
	
	
    private Boolean publicGroup;
    private Boolean openGroup;


    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"description", "owner", "admins", "members", "isPublic", "subGroups", "parent", "pendingMembers"})
    public Set<Group> subGroups;

    @ManyToOne
    @JoinColumn(name="parent_id")
    @JsonIgnoreProperties({"description", "owner", "admins", "members", "isPublic", "subGroups", "parent", "pendingMembers"})
    public Group parent;

    public Group() {
        this(null);
    }

    public Group(Long id) {
        this.setId(id);
        this.name = "";
        this.description = "";
        this.admins = new HashSet<>();
        this.members = new HashSet<>();
		this.pendingMembers = new HashSet<>();
        this.subGroups = new HashSet<>();
        this.parent = null;
        this.owner = null;
        this.publicGroup = false;
        this.openGroup = false;
    }

	public Group(String name, String description, Boolean publicGroup, Boolean openGroup) {
        this(null);
        this.name = name;
        this.description = description;
        this.admins = new HashSet<>();
        this.members = new HashSet<>();
		this.pendingMembers = new HashSet<>();
        this.subGroups = new HashSet<>();
        this.parent = null;
        this.owner = null;
        this.publicGroup = publicGroup;
        this.openGroup = openGroup;
    }
	
	
    public String getName() {
        return name;
    }

    public void setName(String name) throws CougrGroupException{
        if (name == null) {
            throw new CougrGroupException("Group name cannot be null");
        }
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) throws CougrGroupException{
        if (description == null) {
            throw new CougrGroupException("Group description cannot be null");
        }
        this.description = description;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) throws CougrGroupException{
        if (owner == null || owner.equals(User.EMPTY_USER)) {
            throw new CougrGroupException("Owner cannot be null or EMPTY_USER");
        }
		//owners must be members
		if(!this.members.contains(owner)){
			this.members.add(owner);
		}
        this.owner = owner;
		//Owners should also be on the admin list
		this.addAdmin(owner);
		
    }

    public Set<User> getAdmins() {
        return ImmutableSet.copyOf(admins);
    }

    protected void setAdmins(Set<User> admins) throws CougrGroupException{
        if (admins == null) {
            throw new CougrGroupException("Admin Set cannot be null");
        }
        this.admins = admins;
    }

    public Set<User> getMembers() {
        return ImmutableSet.copyOf(members);
    }
	
	public Set<User> getPendingMembers() {
        return ImmutableSet.copyOf(pendingMembers);
    }

    @JsonIgnore
    public Set<String> getMemeberUsernames() {
        return this.members.stream().map(m -> m.getUsername()).collect(Collectors.toSet());
    }
    @JsonIgnore
    public Set<String> getMemberSubIssuers() {
        return this.members.stream().map(m -> m.getSubIssuer()).collect(Collectors.toSet());
    }

    @JsonIgnore
    public Set<String> getAdminUsernames() {
        return this.admins.stream().map(a -> a.getUsername()).collect(Collectors.toSet());
    }
    @JsonIgnore
    public Set<String> getAdminSubIssuers() {
        return this.admins.stream().map(a -> a.getSubIssuer()).collect(Collectors.toSet());
    }

    protected void setMembers(Set<User> members) throws CougrGroupException{
        if (members == null) {
            throw new CougrGroupException("Member Set cannot be null");
        }
        this.members = members;
    }
	
	protected void setPendingMembers(Set<User> pendingMembers) throws CougrGroupException{
        if (pendingMembers == null) {
            throw new CougrGroupException("Pending Member Set cannot be null");
        }
        this.pendingMembers = pendingMembers;
    }

    public boolean addMember(User user) throws CougrGroupException {
        if (user == null || user.equals(User.EMPTY_USER)) {
            throw new CougrGroupException("Cannot add null or empty User: "+user);
        }
        return this.members.add(user);
    }

    //TODO: assuming list of users contains no null or EMPTY_USERs
    public boolean addMembers(List<User> users) {
        if (users == null) {
            LOG.debug("Group::addMembers - Cannot add null list");
            return false;
        }
        return this.members.addAll(users);
    }
	
	public boolean addPendingMember(User user) throws CougrGroupException {
        if (user == null || user.equals(User.EMPTY_USER)) {
            throw new CougrGroupException("Cannot add null or empty User: "+user);
        }
        return this.pendingMembers.add(user);
    }

    //TODO: assuming list of users contains no null or EMPTY_USERs
    public boolean addPendingMembers(List<User> users) {
        if (users == null) {
            LOG.debug("Group::addMembers - Cannot add null list");
            return false;
        }
        return this.pendingMembers.addAll(users);
    }
	
	public boolean approvePendingMember(User user) throws CougrGroupException {
        if (user == null || user.equals(User.EMPTY_USER)) {
            throw new CougrGroupException("Cannot add null or empty User: "+user);
        }
		if(this.members.contains(user)){
			throw new CougrGroupException("Cannot approve a user who is not pending: "+user);
		}
		this.addMember(user);
		this.removePendingMember(user);
		return true;
	}
	
	public boolean approvePendingMembers(List<User> users) throws CougrGroupException {
        if (users == null ) {
            throw new CougrGroupException("Cannot add null or empty User: Cannot add null list");
        }
		
		this.addMembers(users);
		this.removePendingMembers(users);
		return true;
	}

    public boolean addAdmin(User user) throws CougrGroupException{
        if (user == null || user.equals(User.EMPTY_USER)) {
            throw new CougrGroupException("Cannot add null or empty User: "+user);
        }
        return this.admins.add(user);
    }

    public boolean addAdmins(List<User> users) {
        if (users == null) {
            LOG.debug("Group::addAdmins - Cannot add null list");
            return false;
        }
        return this.admins.addAll(users);
    }

    public boolean removeMember(User user) throws CougrGroupException {
        if (user == null || user.equals(User.EMPTY_USER)) {
            throw new CougrGroupException("Cannot remove null or empty User: "+user);
        }
        return this.members.remove(user);
    }

    public boolean removeMembers(List<User> users) {
        return this.members.removeAll(users);
    }
	
	public boolean removePendingMember(User user) throws CougrGroupException {
        if (user == null || user.equals(User.EMPTY_USER)) {
            throw new CougrGroupException("Cannot remove null or empty User: "+user);
        }
        return this.pendingMembers.remove(user);
    }

    public boolean removePendingMembers(List<User> users) {
        return this.pendingMembers.removeAll(users);
    }

    public boolean removeAdmin(User user) throws CougrGroupException{
        if (user == null || user.equals(User.EMPTY_USER)) {
            throw new CougrGroupException("Cannot remove null or empty User: "+user);
        }
        return this.admins.remove(user);
    }

    public boolean removeAdmins(List<User> users) {
        return this.admins.removeAll(users);
    }

    public void setPublicGroup(Boolean publicGroup) throws CougrGroupException{
        if(publicGroup == null) {
            throw new CougrGroupException("publicGroup cannot be null");
        }
        if(publicGroup.equals(Boolean.TRUE) && this.parent != null && this.parent.isPrivateGroup()){
            throw new CougrGroupException("Subgroup cannot be public is parent group is private.");
        }
        this.publicGroup = publicGroup;
    }

    @JsonProperty("isPublic")
    public Boolean isPublicGroup() {
        return this.publicGroup;
    }

    @JsonIgnore
    public Boolean isPrivateGroup() {
        return !this.publicGroup;
    }

    @JsonIgnore
    public Boolean getPublicGroup() {
        return publicGroup;
    }


    public void setOpenGroup(Boolean openGroup) throws CougrGroupException{
        if(openGroup == null) {
            throw new CougrGroupException("openGroup cannot be null");
        }
        if(openGroup.equals(Boolean.TRUE) && this.parent != null && this.parent.isClosedGroup()){
            throw new CougrGroupException("Subgroup cannot be open is parent group is closed.");
        }
        this.openGroup = openGroup;
    }

    @JsonProperty("isOpen")
    public Boolean isOpenGroup(){ return this.openGroup; }

    @JsonIgnore
    public Boolean isClosedGroup(){ return !this.openGroup; }

    @JsonIgnore
    public Boolean getOpenGroup(){ return this.openGroup; }

    public boolean addSubgroup(Group group) throws CougrGroupException {
        if(group == null || group.equals(Group.EMPTY_GROUP)){
            throw new CougrGroupException("Cannot add null or EMPTY_GROUP as subGroup: "+group);
        }
        return this.subGroups.add(group);
    }

    public void removeSubgroup(Group group) {
        if(group == null || group.equals(Group.EMPTY_GROUP)){
            LOG.debug("Group::removeSubgroup - Tried removing null or EMPTY_GROUP: "+group);
            return;
        }
        this.subGroups.remove(group);
    }

    @JsonIgnore
    public Set<String> getSubgroupNames() {
        return this.subGroups.stream().map(a -> a.getName()).collect(Collectors.toSet());
    }

    public Set<Group> getSubGroups() {
        return ImmutableSet.copyOf(subGroups);
    }

    protected void setSubGroups(Set<Group> subGroups) throws CougrGroupException{
        if (admins == null) {
            throw new CougrGroupException("Subgroups Set cannot be null");
        }
        this.subGroups = subGroups;
    }

    public Group getParent() {
        return parent;
    }

    public void setParent(Group parent) throws CougrGroupException{
        if(parent == null || parent.equals(Group.EMPTY_GROUP)) {
            throw new CougrGroupException("Cannot get parent to null or EMPTY_GROUP");
        }
        this.parent = parent;
    }

    @Override
    public String toString() {
//        ObjectMapper mapper = new ObjectMapper();
//        ByteArrayOutputStream bao = new ByteArrayOutputStream();
//        try {
//            mapper.writeValue(bao, this);
//            return bao.toString();
//        } catch (IOException ex) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("Group [");
//            sb.append("'id': ");
//            sb.append(this.getId());
//            sb.append(", 'name' : '");
//            sb.append(this.getName());
//            sb.append("']");
//            return sb.toString();
//        }

        StringBuilder sb = new StringBuilder();
        sb.append("Group [");
        sb.append("'id': ");
        sb.append(this.getId());
        sb.append(", 'name' : '");
        sb.append(this.getName());
        sb.append("']");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group)) return false;
        if (!super.equals(o)) return false;

        Group group = (Group) o;
        if (name != null ? !name.equals(group.name) : group.name != null) return false;
        if (description != null ? !description.equals(group.description) : group.description != null) return false;

        if (admins != null ? !admins.equals(group.admins) : group.admins != null) return false;
        if (members != null ? !members.equals(group.members) : group.members != null) return false;
		if (pendingMembers != null ? !pendingMembers.equals(group.pendingMembers) : group.pendingMembers != null) return false;

        if (owner != null ? !owner.equals(group.owner) : group.owner != null) return false;
        if (parent != null ? !parent.getId().equals(group.parent.getId()) : group.parent != null) return false; //can't use parent.equals becuare of infinite loop
        if (publicGroup != null ? !publicGroup.equals(group.publicGroup) : group.publicGroup != null) return false;
        if (subGroups != null ? !subGroups.equals(group.subGroups) : group.subGroups != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (admins != null ? admins.hashCode() : 0);
        result = 31 * result + (members != null ? members.hashCode() : 0);
		result = 31 * result + (pendingMembers != null ? pendingMembers.hashCode() : 0);
        result = 31 * result + (publicGroup != null ? publicGroup.hashCode() : 0);
        result = 31 * result + (subGroups != null ? subGroups.hashCode() : 0);
        result = 31 * result + (parent != null ? parent.toString().hashCode() : 0); //can't use parent.hashCode becuase of infinite loop.
        return result;
    }
}
