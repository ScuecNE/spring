package com.sugus.service.impl;

import com.sugus.annotation.WAutowired;
import com.sugus.annotation.WService;
import com.sugus.model.UserInfo;
import com.sugus.service.FinancialService;
import com.sugus.service.UserService;

@WService("financialService")
public class FinancialServiceImpl implements FinancialService {

    @WAutowired
    private UserService userService;

    @Override
    public void setSalary(String name, Integer age, Double salary) {
        UserInfo userInfo = userService.getUser(name, age);
        userInfo.setSalary(salary);
    }
}
