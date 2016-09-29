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

import java.util.List;
import org.mitre.cougr.dao.UserDAO;
import org.mitre.cougr.dao.model.User;
import org.mitre.cougr.exception.CougrUnknownUser;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserDAO userDAO;

    public User getUserById(Long id){
        User user =  userDAO.findOne(id);
        if(user == null){
            return User.EMPTY_USER;
        }else{
            return user;
        }
    }

    public User getUserByUsername(String username){
        return userDAO.findByUsername(username);
    }

    public List<User> getAllUsers(){
        return userDAO.findAll();
    }

    public User addUser(User user) {
        return userDAO.save(user);
    }

    public User getUserBySubAndIssuer(String sub, String issuer) {
        User user =  userDAO.findBySubAndIssuer(sub, issuer);
        if(user == null){
            return user.EMPTY_USER;
        }else{
            return user;
        }
    }

    public User getLoggedInUser(OIDCAuthenticationToken token) throws CougrUnknownUser {
        User user =  this.getUserBySubAndIssuer(token.getSub(), token.getIssuer());
        if(user == null || user.equals(User.EMPTY_USER)){
            throw new CougrUnknownUser("No user for "+token.getSub()+"@"+token.getIssuer());
        }
        return user;
    }

    public User setCougrAdmin(User user, boolean cougrAdmin) {
        user.setCougrAdmin(cougrAdmin);
        return userDAO.save(user);
    }

}
