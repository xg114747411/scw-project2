package com.atguigu.scw.user.vo.request;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MemberRequestVo {
	private String loginacct; //手机号码
	private String userpswd;//密码
	private String code; //验证码
	private String email; //邮箱
	private String usertype; //用户的类型    0- 个人  1-企业
	
}
