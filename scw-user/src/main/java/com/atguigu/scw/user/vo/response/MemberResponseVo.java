package com.atguigu.scw.user.vo.response;

import com.atguigu.scw.common.vo.BaseVo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MemberResponseVo extends BaseVo{ 
	private String loginacct; //手机号码
	private String username; //用户姓名
	//private String accessToken; //用户访问控制令牌
	private String email; //邮箱
	private String usertype; //用户的类型    0- 个人  1-企业
}
