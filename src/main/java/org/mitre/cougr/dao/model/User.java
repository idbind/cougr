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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Created by bkeyes on 1/13/15.
 */

@Entity
@Table(name="CougrUser")
@JsonIgnoreProperties({"new"})
@JsonRootName("user")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME , property = "class")
public class User extends AbstractPersistable<Long> {
    private static final long serialVersionUID = 1832554782172361837L;

    @Transient
    @JsonIgnore
    public static final User EMPTY_USER = new User();

    @Column(unique = true, length = 255)
    private String username;


    @Column(unique = false, length = 255)
    private String email;


    @Column(unique = false, length = 255)
    private String firstname;


    @Column(unique = false, length = 255)
    private String lastname;


    @Column(unique = false, length = 255)
    private String sub;


    @Column(unique = false, length = 255)
    private String issuer;

    @Column(unique = true, length = 512)
    private String subIssuer;

    @Column(unique = false)
    private Boolean cougrAdmin;

    public User(){
        this(null);
    }

    public User(Long id) {
        this.setId(id);
        this.username = "None";
        this.email = "None";
        this.firstname = "None";
        this.lastname = "None";
        this.sub = "None";
        this.issuer = "None";
        this.subIssuer = this.sub+"@"+this.issuer;
        this.cougrAdmin = false;
    }

    public User(String username, String email, String firstname, String lastname, String sub, String issuer) {
        this(null);
        this.username = username;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.sub = sub;
        this.issuer = issuer;
        this.subIssuer = this.sub+"@"+this.issuer;
        this.cougrAdmin = false;
    }



    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubIssuer(){
        return this.subIssuer;
    }

    public void setCougrAdmin(Boolean isAdmin){
        this.cougrAdmin = isAdmin;
    }

    public Boolean isCougrAdmin(){
        return this.cougrAdmin;
    }

    @Override
    public String toString() {
//        ObjectMapper mapper = new ObjectMapper();
//        ByteArrayOutputStream bao = new ByteArrayOutputStream();
//        try {
//            mapper.writeValue(bao, this);
//            return bao.toString();
//        }catch(IOException ex) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("User ['id': ");
//            sb.append(this.getId());
//            sb.append(", 'username': '");
//            sb.append(this.getUsername());
//            sb.append("']");
//            return sb.toString();
//        }

        StringBuilder sb = new StringBuilder();
        sb.append("User ['id': ");
        sb.append(this.getId());
        sb.append(", 'username': '");
        sb.append(this.getUsername());
        sb.append("']");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        if (!super.equals(o)) return false;

        User user = (User) o;

        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        if (firstname != null ? !firstname.equals(user.firstname) : user.firstname != null) return false;
        if (issuer != null ? !issuer.equals(user.issuer) : user.issuer != null) return false;
        if (lastname != null ? !lastname.equals(user.lastname) : user.lastname != null) return false;
        if (sub != null ? !sub.equals(user.sub) : user.sub != null) return false;
        if (username != null ? !username.equals(user.username) : user.username != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (firstname != null ? firstname.hashCode() : 0);
        result = 31 * result + (lastname != null ? lastname.hashCode() : 0);
        result = 31 * result + (sub != null ? sub.hashCode() : 0);
        result = 31 * result + (issuer != null ? issuer.hashCode() : 0);
        return result;
    }
}
