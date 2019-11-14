package com.atguigu.scw.user.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.atguigu.scw.common.consts.AppConsts;
import com.atguigu.scw.common.templates.SmsTemplate;
import com.atguigu.scw.common.utils.AppResponse;
import com.atguigu.scw.common.utils.ScwUtils;
import com.atguigu.scw.user.bean.TMember;
import com.atguigu.scw.user.service.MemberService;
import com.atguigu.scw.user.vo.request.MemberRequestVo;
import com.atguigu.scw.user.vo.response.MemberResponseVo;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
@Api(tags="处理用户注册验证码登录请求的controller")
@RestController
@Slf4j
public class UserController {
	@Autowired
	StringRedisTemplate stringRedisTemplate;
	@Autowired
	SmsTemplate smsTemplate;
	@Autowired
	MemberService memberService;
	//3、处理登录请求
	@ApiOperation("处理登录请求")
	@PostMapping("/user/doLogin")
	public AppResponse<Object> doLogin(@RequestParam("loginacct")String loginacct , @RequestParam("userpswd")String userpswd) {
		//3.1 调用service的方法查询用户信息
		TMember member =  memberService.getMember(loginacct , userpswd);
		log.info("登录查询的member对象："+member+" ,请求参数："+ loginacct +" , "+userpswd);
		if(member==null) {
			return AppResponse.fail(null, "登录失败！账号或密码错误");
		}
		//3.2 查询成功，创建存储信息的键,将用户信息存到redis中
		String memberToken = UUID.randomUUID().toString().replace("-", "");
		//将对象转为json字符串
		String memberJson = JSON.toJSONString(member);
		stringRedisTemplate.opsForValue().set(memberToken,memberJson , 7 , TimeUnit.DAYS );
		//3.3 返回token给前台系统
		//响应用户信息给前台项目，可以封装对应的vo
		MemberResponseVo responseVo = new MemberResponseVo();
		BeanUtils.copyProperties(member, responseVo);
		responseVo.setAccessToken(memberToken);
		return AppResponse.ok(responseVo, "登录成功");
		//以后前台系统访问后台系统时，只要携带token，能够在redis中获取用户信息，就代表已登录
	}
	
	//2、处理注册请求的方法
	@ApiOperation("处理注册请求的方法")
	@PostMapping("/user/doRegist")
	public AppResponse<Object> doRegist(MemberRequestVo vo) {//注册信息包含TMember中没有的字段[DAO： TMember 对应数据库t_member表]
		//controller的方法接受的参数是和视图层对应的：VO :view object
		//2.1 检查验证码是否正确
		//查询手机号对应的验证码
		String loginacct = vo.getLoginacct();
		String redisCode = stringRedisTemplate.opsForValue().get(AppConsts.CODE_PREFIX+loginacct+AppConsts.CODE_CODE_SUFFIX);
		if(StringUtils.isEmpty(redisCode)) {
			return AppResponse.fail(null, "验证码过期");
		}
		if(!redisCode.equals(vo.getCode())) {
			return AppResponse.fail(null, "验证码错误");
		}
		//2.2 注册
		memberService.saveMember(vo);
		
		//2.3  删除redis中的验证码
		stringRedisTemplate.delete(AppConsts.CODE_PREFIX+loginacct+AppConsts.CODE_CODE_SUFFIX);
		return AppResponse.ok(null, "注册成功");
	}
	
	
	
	
	//1、给手机号码发送短信验证码的方法
	@ApiOperation("发送验证码的方法")
	@ApiImplicitParams(value= @ApiImplicitParam(name="phoneNum" , required=true , value="手机号码") )
	@PostMapping("/user/sendSms")
	public AppResponse<Object> sendSms(@RequestParam("phoneNum")String phoneNum) {
		//验证手机号码格式
		boolean b = ScwUtils.isMobilePhone(phoneNum);
		if(!b) {
			// new AppResponse(code=10001 , message="手机号码格式错误" , data=sa);
			// new AppResponse(0 , "success" , list);
			// {code:10001  , message:"aasdsa" , data:[{},{}]}
			return AppResponse.fail(null, "手机号码格式错误");
		}
		//验证redis中存储的当前手机号码获取验证码的次数[第一次获取没有、或者没有超过指定次数可以继续获取验证码]
		//一个手机号码一天内最多只能获取3次验证码:   code:1812312332:count
		String countStr = stringRedisTemplate.opsForValue().get(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_COUNT_SUFFIX);
		int count = 0;
		if(!StringUtils.isEmpty(countStr)) {
			//如果数量字符串不为空，转为数字
			count = Integer.parseInt(countStr);
		}
		if(count>=3) {
			return AppResponse.fail(null, "验证码次数超出范围");
		}
		//验证redis中当前手机号码是否存在未过期验证码[看需求是否编写]
		//获取当前手机号码在redis中的验证码：如果为空，代表没有：    code:2423423423:code
		// 键在值在 ，键亡值亡
		Boolean hasKey = stringRedisTemplate.hasKey(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_CODE_SUFFIX);
		if(hasKey) {
			return AppResponse.fail(null, "请勿频繁获取验证码");
		}
		//发送验证码
		//随机6位生成验证码  
		String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
		//封装发送短信验证码请求参数的集合
		Map<String, String> querys = new HashMap<String, String>();
	    querys.put("mobile", phoneNum);
	    querys.put("param", AppConsts.CODE_PREFIX+code);
	    querys.put("tpl_id", "TP1711063");
//		boolean sendSms = smsTemplate.sendSms(querys);
//		if(!sendSms) {
//			return "短信验证码发送失败";
//		}
		//将验证码存到redis中5分钟
		stringRedisTemplate.opsForValue().set(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_CODE_SUFFIX, code, 5, TimeUnit.MINUTES);
		//修改该手机号码发送验证码的次数
		Long expire = stringRedisTemplate.getExpire(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_COUNT_SUFFIX , TimeUnit.MINUTES);//获取次数的过期时间
		log.info("查詢到的過期時間:{}", expire);// -2代表已過期(未注册)
		if(expire==null  || expire<=0 ) {
			expire = (long) (24*60);
		}
		count++;
		stringRedisTemplate.opsForValue().set(AppConsts.CODE_PREFIX+phoneNum+AppConsts.CODE_COUNT_SUFFIX, count+"", expire, TimeUnit.MINUTES);
		//响应成功
		return AppResponse.ok(null, "发送验证码成功");
	}
	
	
	
}
