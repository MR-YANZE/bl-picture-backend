package com.baolong.pictures.interfaces.assembler;

import com.baolong.pictures.domain.user.entity.User;
import com.baolong.pictures.interfaces.dto.user.UserAddRequest;
import com.baolong.pictures.interfaces.dto.user.UserEditRequest;
import com.baolong.pictures.interfaces.dto.user.UserUpdateRequest;
import com.baolong.pictures.interfaces.vo.user.LoginUserVO;
import com.baolong.pictures.interfaces.vo.user.UserVO;
import org.springframework.beans.BeanUtils;

/**
 * 用户转换类
 *
 * @author Baolong 2025年03月04 23:37
 * @version 1.0
 * @since 1.8
 */
public class UserAssembler {

	/**
	 * 用户添加请求 转为 用户实体
	 */
	public static User toUserEntity(UserAddRequest userAddRequest) {
		User user = new User();
		BeanUtils.copyProperties(userAddRequest, user);
		return user;
	}

	/**
	 * 用户更新请求 转为 用户实体
	 */
	public static User toUserEntity(UserUpdateRequest userUpdateRequest) {
		User user = new User();
		BeanUtils.copyProperties(userUpdateRequest, user);
		return user;
	}

	/**
	 * 用户编辑请求 转为 用户实体
	 */
	public static User toUserEntity(UserEditRequest userEditRequest) {
		User user = new User();
		BeanUtils.copyProperties(userEditRequest, user);
		return user;
	}

	/**
	 * 用户实体 转为 登录用户 VO
	 */
	public static LoginUserVO toLoginUserVO(User user) {
		LoginUserVO loginUserVO = new LoginUserVO();
		BeanUtils.copyProperties(user, loginUserVO);
		return loginUserVO;
	}

	/**
	 * 用户实体 转为 用户 VO
	 */
	public static UserVO toUserVO(User user) {
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(user, userVO);
		return userVO;
	}
}
