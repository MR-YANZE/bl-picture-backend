package com.baolong.blpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import com.baolong.picture.infrastructure.exception.BusinessException;
import com.baolong.picture.infrastructure.exception.ErrorCode;
import com.baolong.picture.infrastructure.exception.ThrowUtils;
import com.baolong.blpicturebackend.model.dto.analyze.SpaceAnalyzeRequest;
import com.baolong.blpicturebackend.model.dto.analyze.SpaceCategoryAnalyzeRequest;
import com.baolong.blpicturebackend.model.dto.analyze.SpaceRankAnalyzeRequest;
import com.baolong.blpicturebackend.model.dto.analyze.SpaceSizeAnalyzeRequest;
import com.baolong.blpicturebackend.model.dto.analyze.SpaceTagAnalyzeRequest;
import com.baolong.blpicturebackend.model.dto.analyze.SpaceUsageAnalyzeRequest;
import com.baolong.blpicturebackend.model.dto.analyze.SpaceUserAnalyzeRequest;
import com.baolong.blpicturebackend.model.entity.CategoryTag;
import com.baolong.blpicturebackend.model.entity.Picture;
import com.baolong.blpicturebackend.model.entity.Space;
import com.baolong.picture.domain.user.entity.User;
import com.baolong.blpicturebackend.model.vo.analyze.SpaceCategoryAnalyzeResponse;
import com.baolong.blpicturebackend.model.vo.analyze.SpaceSizeAnalyzeResponse;
import com.baolong.blpicturebackend.model.vo.analyze.SpaceTagAnalyzeResponse;
import com.baolong.blpicturebackend.model.vo.analyze.SpaceUsageAnalyzeResponse;
import com.baolong.blpicturebackend.model.vo.analyze.SpaceUserAnalyzeResponse;
import com.baolong.blpicturebackend.service.CategoryTagService;
import com.baolong.blpicturebackend.service.PictureService;
import com.baolong.blpicturebackend.service.SpaceAnalyzeService;
import com.baolong.blpicturebackend.service.SpaceService;
import com.baolong.picture.application.service.UserApplicationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 空间分析服务实现
 *
 * @author Baolong 2025年02月27 19:18
 * @version 1.0
 * @since 1.8
 */
@Slf4j
@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {
	@Resource
	private UserApplicationService userApplicationService;
	@Resource
	private SpaceService spaceService;
	@Resource
	private PictureService pictureService;
	@Resource
	private CategoryTagService categoryTagService;

	/**
	 * 获取空间使用分析数据
	 *
	 * @param spaceUsageAnalyzeRequest 空间资源分析请求
	 * @param loginUser                当前登录用户
	 * @return SpaceUsageAnalyzeResponse 分析结果
	 */
	@Override
	public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
		ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
		if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
			// 查询全部或公共图库逻辑
			// 仅管理员可以访问
			boolean isAdmin = loginUser.isAdmin();
			ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH_ERROR, "无权访问空间");
			// 统计公共图库的资源使用
			QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
			queryWrapper.select("picSize");
			if (!spaceUsageAnalyzeRequest.isQueryAll()) {
				queryWrapper.isNull("spaceId");
			}
			List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
			long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
			long usedCount = pictureObjList.size();
			// 封装返回结果
			SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
			spaceUsageAnalyzeResponse.setUsedSize(usedSize);
			spaceUsageAnalyzeResponse.setUsedCount(usedCount);
			// 公共图库无上限、无比例
			spaceUsageAnalyzeResponse.setMaxSize(null);
			spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
			spaceUsageAnalyzeResponse.setMaxCount(null);
			spaceUsageAnalyzeResponse.setCountUsageRatio(null);
			return spaceUsageAnalyzeResponse;
		} else {
			// 查询指定空间
			Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
			ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
			// 获取空间信息
			Space space = spaceService.getById(spaceId);
			ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

			// 权限校验：仅空间所有者或管理员可访问
			spaceService.checkSpaceAuth(loginUser, space);

			// 构造返回结果
			SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
			response.setUsedSize(space.getTotalSize());
			response.setMaxSize(space.getMaxSize());
			// 后端直接算好百分比，这样前端可以直接展示
			double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
			response.setSizeUsageRatio(sizeUsageRatio);
			response.setUsedCount(space.getTotalCount());
			response.setMaxCount(space.getMaxCount());
			double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
			response.setCountUsageRatio(countUsageRatio);
			return response;
		}
	}

	/**
	 * 获取空间分类分析数据
	 *
	 * @param spaceCategoryAnalyzeRequest 空间分类分析请求
	 * @param loginUser                   当前登录用户
	 * @return List<SpaceCategoryAnalyzeResponse>
	 */
	@Override
	public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
		ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

		// 检查权限
		checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);

		// 构造查询条件
		QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
		// 根据分析范围补充查询条件
		fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);

		// 使用 MyBatis-Plus 分组查询
		queryWrapper.select("category AS category",
						"COUNT(*) AS count",
						"SUM(picSize) AS totalSize")
				.groupBy("category");

		// 查询并转换结果
		return pictureService.getBaseMapper().selectMaps(queryWrapper)
				.stream()
				.map(result -> {
					String category = result.get("category") != null ? result.get("category").toString() : "未分类";
					Long count = ((Number) result.get("count")).longValue();
					Long totalSize = ((Number) result.get("totalSize")).longValue();
					return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
				})
				.collect(Collectors.toList());
	}

	/**
	 * 获取空间标签分析数据
	 *
	 * @param spaceTagAnalyzeRequest 空间标签分析请求
	 * @param loginUser              当前登录用户
	 * @return List<SpaceTagAnalyzeResponse>
	 */
	@Override
	public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
		ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

		// 检查权限
		checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);

		// 构造查询条件
		QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
		fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

		// 查询所有符合条件的标签
		queryWrapper.select("tags");
		List<String> tagsList = pictureService.getBaseMapper().selectObjs(queryWrapper)
				.stream()
				.filter(ObjUtil::isNotNull)
				.map(Object::toString)
				.collect(Collectors.toList());

		// 合并所有标签并统计使用次数
		Map<String, Long> tagCountMap = tagsList.stream()
				.flatMap(tag -> Arrays.stream(tag.split(",")))
				.collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

		// 查询所有标签对应的名字
		Map<String, String> tagMap = categoryTagService.list(new LambdaQueryWrapper<CategoryTag>().in(CategoryTag::getId, tagCountMap.keySet()))
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(tag -> tag.getId().toString(), CategoryTag::getName));

		// 转换为响应对象，按使用次数降序排序
		return tagCountMap.entrySet().stream()
				.sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())) // 降序排列
				.map(entry -> new SpaceTagAnalyzeResponse(tagMap.get(entry.getKey()), entry.getValue()))
				.collect(Collectors.toList());
	}

	/**
	 * 获取空间大小分析数据
	 *
	 * @param spaceSizeAnalyzeRequest 空间大小分析请求
	 * @param loginUser               当前登录用户
	 * @return List<SpaceSizeAnalyzeResponse>
	 */
	@Override
	public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
		ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

		// 检查权限
		checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

		// 构造查询条件
		QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
		fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

		// 查询所有符合条件的图片大小
		queryWrapper.select("picSize");
		List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper)
				.stream()
				.map(size -> ((Number) size).longValue())
				.collect(Collectors.toList());

		// 定义分段范围，注意使用有序 Map
		Map<String, Long> sizeRanges = new LinkedHashMap<>();
		sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
		sizeRanges.put("100KB-500KB", picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
		sizeRanges.put("500KB-1MB", picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
		sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1 * 1024 * 1024).count());

		// 转换为响应对象
		return sizeRanges.entrySet().stream()
				.map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	/**
	 * 获取空间用户上传分析数据
	 *
	 * @param spaceUserAnalyzeRequest 空间用户上传分析请求
	 * @param loginUser               当前登录用户
	 * @return List<SpaceUserAnalyzeResponse>
	 */
	@Override
	public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
		ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
		// 检查权限
		checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

		// 构造查询条件
		QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
		Long userId = spaceUserAnalyzeRequest.getUserId();
		queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
		fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

		// 分析维度：每日、每周、每月
		String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
		switch (timeDimension) {
			case "day":
				queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
				break;
			case "week":
				queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
				break;
			case "month":
				queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
				break;
			default:
				throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
		}

		// 分组和排序
		queryWrapper.groupBy("period").orderByAsc("period");

		// 查询结果并转换
		List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
		return queryResult.stream()
				.map(result -> {
					String period = result.get("period").toString();
					Long count = ((Number) result.get("count")).longValue();
					return new SpaceUserAnalyzeResponse(period, count);
				})
				.collect(Collectors.toList());
	}

	/**
	 * 获取空间排行分析数据
	 *
	 * @param spaceRankAnalyzeRequest 空间排名分析请求
	 * @param loginUser               当前登录用户
	 * @return List<Space>
	 */
	@Override
	public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
		ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

		// 仅管理员可查看空间排行
		ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");

		// 构造查询条件
		QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
		queryWrapper.select("id", "spaceName", "userId", "totalSize")
				.orderByDesc("totalSize")
				.last("LIMIT " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名

		// 查询结果
		return spaceService.list(queryWrapper);
	}

	/**
	 * 检查用户空间分析权限
	 *
	 * @param spaceAnalyzeRequest 空间分析请求
	 * @param loginUser           当前登录用户
	 */
	private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
		// 检查权限
		if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
			// 全空间分析或者公共图库权限校验：仅管理员可访问
			ThrowUtils.throwIf(!loginUser.isAdmin(), ErrorCode.NO_AUTH_ERROR, "无权访问公共图库");
		} else {
			// 私有空间权限校验
			Long spaceId = spaceAnalyzeRequest.getSpaceId();
			ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
			Space space = spaceService.getById(spaceId);
			ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
			spaceService.checkSpaceAuth(loginUser, space);
		}
	}

	/**
	 * 根据分析范围填充分析查询条件
	 *
	 * @param spaceAnalyzeRequest 空间分析请求
	 * @param queryWrapper        QueryWrapper
	 */
	private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
		if (spaceAnalyzeRequest.isQueryAll()) {
			return;
		}
		if (spaceAnalyzeRequest.isQueryPublic()) {
			queryWrapper.isNull("spaceId");
			return;
		}
		Long spaceId = spaceAnalyzeRequest.getSpaceId();
		if (spaceId != null) {
			queryWrapper.eq("spaceId", spaceId);
			return;
		}
		throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
	}

}
