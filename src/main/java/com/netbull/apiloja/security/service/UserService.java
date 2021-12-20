package com.netbull.apiloja.security.service;

import com.netbull.apiloja.security.model.LoggedUser;
import com.netbull.apiloja.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private StoreService storeService;

    @Override
    public UserDetails loadUserByUsername(String username){
        LoggedUser user;
        try {
            user  = new LoggedUser(storeService.getStoreByEmail(username));
        } catch(Exception e) {
            user = new LoggedUser();
        }
        return user;
    }
}
