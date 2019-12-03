package com.sugus.service;

import com.sugus.model.UserInfo;

public interface UserService {

    UserInfo getUser(String name, Integer age);

}
