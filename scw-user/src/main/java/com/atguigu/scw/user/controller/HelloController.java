package com.atguigu.scw.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(tags="测试Swagger的Controller")
@RestController
public class HelloController {

	@ApiOperation(value="hello方法")
	@GetMapping("/hello")
	public String hello(MultipartFile file) {
		return "xxxxx";
	}
	/*@ApiOperation("登录方法")
	@ApiImplicitParams(value= {
			@ApiImplicitParam(name="username" , required=true , dataTypeClass=String.class),
			@ApiImplicitParam(name="password" , required=false  ,defaultValue="123456")
	})
	@PostMapping("/login")
	public User login(String username , String password) {
		
		return new User(100, username, password);
	}*/
	
}
