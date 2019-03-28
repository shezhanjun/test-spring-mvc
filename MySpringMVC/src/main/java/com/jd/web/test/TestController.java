package com.jd.web.test;

import com.jd.web.test.annotation.*;
import com.jd.web.test.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@JDController
@JDRequestMapping(value="/test")
public class TestController {

    @JDAutowired("testService")
    private TestService testService;

    @JDRequestMapping(value = "/query")
    public void query(HttpServletRequest request, HttpServletResponse response, @JDRequestParam(name="userId") String userId) {
        String result = testService.getUserById(userId);
        System.out.println("TestController userId = [" + userId + "]");
        try {
            response.getWriter().write("params:" + userId + ";reuslt=" + result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
