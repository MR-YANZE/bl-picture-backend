package com.baolong.picture.interfaces.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baolong.picture.shared.auth.annotation.SaSpaceCheckPermission;
import com.baolong.picture.shared.auth.model.SpaceUserPermissionConstant;
import com.baolong.picture.infrastructure.common.BaseResponse;
import com.baolong.picture.infrastructure.common.DeleteRequest;
import com.baolong.picture.infrastructure.common.ResultUtils;
import com.baolong.picture.infrastructure.exception.BusinessException;
import com.baolong.picture.infrastructure.exception.ErrorCode;
import com.baolong.picture.infrastructure.exception.ThrowUtils;
import com.baolong.picture.interfaces.dto.space.spaceUser.SpaceUserAddRequest;
import com.baolong.picture.interfaces.dto.space.spaceUser.SpaceUserEditRequest;
import com.baolong.picture.interfaces.dto.space.spaceUser.SpaceUserQueryRequest;
import com.baolong.picture.domain.space.entity.SpaceUser;
import com.baolong.picture.domain.user.entity.User;
import com.baolong.picture.interfaces.vo.space.SpaceUserVO;
import com.baolong.picture.application.service.SpaceUserApplicationService;
import com.baolong.picture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

	@Resource
	private SpaceUserApplicationService spaceUserApplicationService;

	@Resource
	private UserApplicationService userApplicationService;

	// region 增删改

	/**
	 * 添加成员到空间
	 */
	@PostMapping("/add")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
		ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
		return ResultUtils.success(spaceUserApplicationService.addSpaceUser(spaceUserAddRequest));
	}

	/**
	 * 从空间移除成员
	 */
	@PostMapping("/delete")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
												 HttpServletRequest request) {
		if (deleteRequest == null || deleteRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		long id = deleteRequest.getId();
		// 判断是否存在
		SpaceUser oldSpaceUser = spaceUserApplicationService.getSpaceUserById(id);
		ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
		// 操作数据库
		boolean result = spaceUserApplicationService.deleteSpaceUser(id);
		ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
		return ResultUtils.success(true);
	}

	/**
	 * 查询某个成员在某个空间的信息
	 */
	@PostMapping("/get")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
		// 参数校验
		ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
		Long spaceId = spaceUserQueryRequest.getSpaceId();
		Long userId = spaceUserQueryRequest.getUserId();
		ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
		// 查询数据库
		SpaceUser spaceUser = spaceUserApplicationService.getSpaceUser(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
		ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
		return ResultUtils.success(spaceUser);
	}

	/**
	 * 查询成员信息列表
	 */
	@PostMapping("/list")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
														 HttpServletRequest request) {
		ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
		List<SpaceUser> spaceUserList = spaceUserApplicationService.getSpaceUserList(
				spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest)
		);
		return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(spaceUserList));
	}

	/**
	 * 编辑成员信息（设置权限）
	 */
	@PostMapping("/edit")
	@SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
	public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
											   HttpServletRequest request) {
		if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		// 将实体类和 DTO 进行转换
		SpaceUser spaceUser = new SpaceUser();
		BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
		// 数据校验
		spaceUserApplicationService.validSpaceUser(spaceUser, false);
		// 判断是否存在
		long id = spaceUserEditRequest.getId();
		SpaceUser oldSpaceUser = spaceUserApplicationService.getSpaceUserById(id);
		ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
		// 操作数据库
		boolean result = spaceUserApplicationService.updateSpaceUser(spaceUser);
		ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
		return ResultUtils.success(true);
	}

	/**
	 * 查询我加入的团队空间列表
	 */
	@PostMapping("/list/my")
	public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
		User loginUser = userApplicationService.getLoginUser(request);
		SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
		spaceUserQueryRequest.setUserId(loginUser.getId());
		List<SpaceUser> spaceUserList = spaceUserApplicationService.getSpaceUserList(
				spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest)
		);
		return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(spaceUserList));
	}
}
