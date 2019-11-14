package com.atguigu.scw.user.service;

import com.atguigu.scw.user.bean.TMember;
import com.atguigu.scw.user.vo.request.MemberRequestVo;

public interface MemberService {

	void saveMember(MemberRequestVo vo);

	TMember getMember(String loginacct, String userpswd);

}
