package com.sugus.service.impl;

import com.sugus.annotation.WService;
import com.sugus.model.UserInfo;
import com.sugus.service.UserService;

@WService("userService")
public class UserServiceImpl implements UserService {
    @Override
    public UserInfo getUser(String name, Integer age) {
        UserInfo userInfo = new UserInfo();
        userInfo.setName(name);
        userInfo.setAge(age);
        return userInfo;
    }
}
