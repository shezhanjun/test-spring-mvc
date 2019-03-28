package com.jd.web.test.service;

import com.jd.web.test.annotation.JDService;

@JDService(name = "testService")
public class TestServiceImpl implements  TestService {


    @Override
    public String getUserById(String id) {

        System.out.println("TestServiceImpl userId = [" + id + "]");

        return "search id=" + id;
    }
}
