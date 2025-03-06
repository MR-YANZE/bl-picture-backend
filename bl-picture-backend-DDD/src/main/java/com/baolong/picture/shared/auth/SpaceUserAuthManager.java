package com.baolong.picture.shared.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baolong.picture.shared.auth.model.SpaceUserAuthConfig;
import com.baolong.picture.shared.auth.model.SpaceUserRole;
import com.baolong.picture.domain.space.entity.Space;
import com.baolong.picture.domain.space.entity.SpaceUser;
import com.baolong.picture.domain.space.enums.SpaceRoleEnum;
import com.baolong.picture.domain.space.enums.SpaceTypeEnum;
import com.baolong.picture.application.service.SpaceUserApplicationService;
import com.baolong.picture.application.service.UserApplicationService;
import com.baolong.picture.domain.user.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 空间用户权限管理
 */
@Component
public class SpaceUserAuthManager {

	@Resource
	private UserApplicationService userApplicationService;

	@Resource
	private SpaceUserApplicationService spaceUserApplicationService;

	public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

	static {
		String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
		SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
	}

	/**
	 * 根据角色获取权限列表
	 */
	public List<String> getPermissionsByRole(String spaceUserRole) {
		if (StrUtil.isBlank(spaceUserRole)) {
			return new ArrayList<>();
		}
		// 找到匹配的角色
		SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
				.filter(r -> spaceUserRole.equals(r.getKey()))
				.findFirst()
				.orElse(null);
		if (role == null) {
			return new ArrayList<>();
		}
		return role.getPermissions();
	}

	public List<String> getPermissionList(Space space, User loginUser) {
		if (loginUser == null) {
			return new ArrayList<>();
		}
		// 管理员权限
		List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
		// 公共图库
		if (space == null) {
			if (loginUser.isAdmin()) {
				return ADMIN_PERMISSIONS;
			}
			return new ArrayList<>();
		}
		SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
		if (spaceTypeEnum == null) {
			return new ArrayList<>();
		}
		// 根据空间获取对应的权限
		switch (spaceTypeEnum) {
			case PRIVATE:
				// 私有空间，仅本人或管理员有所有权限
				if (space.getUserId().equals(loginUser.getId()) || loginUser.isAdmin()) {
					return ADMIN_PERMISSIONS;
				} else {
					return new ArrayList<>();
				}
			case TEAM:
				// 团队空间，查询 SpaceUser 并获取角色和权限
				QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
				queryWrapper.eq("spaceId", space.getId());
				queryWrapper.eq("userId", loginUser.getId());
				SpaceUser spaceUser = spaceUserApplicationService.getSpaceUser(queryWrapper);
				if (spaceUser == null) {
					return new ArrayList<>();
				} else {
					return getPermissionsByRole(spaceUser.getSpaceRole());
				}
		}
		return new ArrayList<>();
	}

}
