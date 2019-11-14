package com.atguigu.scw.user.service.impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.atguigu.scw.user.bean.TMember;
import com.atguigu.scw.user.bean.TMemberExample;
import com.atguigu.scw.user.mapper.TMemberMapper;
import com.atguigu.scw.user.service.MemberService;
import com.atguigu.scw.user.vo.request.MemberRequestVo;
@Service
public class MemberServiceImpl implements MemberService {

	@Autowired
	TMemberMapper memberMapper;
	@Autowired
	PasswordEncoder passwordEncoder;
	
	@Override
	public void saveMember(MemberRequestVo vo) {
		//先将vo和member一样的属性的值设置给member
		TMember member = new TMember();
		BeanUtils.copyProperties(vo, member);//将vo和member一样的属性(属性名和数据类型一样)的值拷贝给member
		//密码加密
		member.setUserpswd(passwordEncoder.encode(member.getUserpswd()));
		//设置其他的默认属性值
		member.setUsername(member.getLoginacct());
		member.setAuthstatus("0");// 0 未认证 ， 1认证中 ， 2已认证
		
		memberMapper.insertSelective(member);
	}

	@Override
	public TMember getMember(String loginacct, String userpswd) {
		TMemberExample example = new TMemberExample();
		//检查账号是否正确
		//BCryptPasswordEncoder 同一个密码加密之后的字符串每次都不一样
		example.createCriteria().andLoginacctEqualTo(loginacct);
		List<TMember> list = memberMapper.selectByExample(example );
		
		if(CollectionUtils.isEmpty(list) || list.size()>1) {
			return null;
		}
		TMember member = list.get(0);
		//检查密码是否正确
		boolean b = passwordEncoder.matches(userpswd, member.getUserpswd());//参数1：未加密的字符串， 参数2：数据库中读取的加密的密码
		if(!b) {
			return null;
		}
		//擦除重要信息
		member.setUserpswd("[PROTECTED]");
		return member;
	}

}
