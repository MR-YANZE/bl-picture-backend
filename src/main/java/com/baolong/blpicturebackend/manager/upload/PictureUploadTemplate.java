package com.baolong.blpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.baolong.blpicturebackend.config.CosClientConfig;
import com.baolong.blpicturebackend.exception.BusinessException;
import com.baolong.blpicturebackend.exception.ErrorCode;
import com.baolong.blpicturebackend.manager.CosManager;
import com.baolong.blpicturebackend.model.dto.picture.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板抽象类
 *
 * @author Baolong 2025年02月15 20:51
 * @version 1.0
 * @since 1.8
 */
@Slf4j
public abstract class PictureUploadTemplate {

	@Resource
	protected CosManager cosManager;

	@Resource
	protected CosClientConfig cosClientConfig;

	/**
	 * 模板方法，定义上传流程
	 */
	public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
		// 1. 校验图片
		validPicture(inputSource);

		// 2. 图片上传地址
		String uuid = RandomUtil.randomString(16);
		String originFilename = getOriginFilename(inputSource);
		String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
				FileUtil.getSuffix(originFilename));
		String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

		File file = null;
		try {
			// 3. 创建临时文件
			file = File.createTempFile(uploadPath, null);
			// 处理文件来源（本地或 URL）
			processFile(inputSource, file);

			// 4. 上传图片到对象存储
			PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
			// 图片原图信息
			ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
			// 获取处理后的结果信息
			ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
			List<CIObject> objectList = processResults.getObjectList();
			if (CollUtil.isNotEmpty(objectList)) {
				// 根据图片处理规则的顺序获取即可
				CIObject compressedCiObject = objectList.get(0);

				// 让缩略图默认为压缩后的图片
				CIObject thumbnailCiObject = compressedCiObject;
				// 有生成缩略图，才得到缩略图
				if (objectList.size() > 1) {
					thumbnailCiObject = objectList.get(1);
				}
				// 封装压缩图返回结果
				return buildResult(originFilename, file, uploadPath, compressedCiObject, thumbnailCiObject, imageInfo);
			}

			// 5. 封装返回结果
			return buildResult(originFilename, file, uploadPath, imageInfo);
		} catch (Exception e) {
			log.error("图片上传到对象存储失败", e);
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
		} finally {
			// 6. 清理临时文件
			deleteTempFile(file);
		}
	}

	/**
	 * 校验输入源（本地文件或 URL）
	 *
	 * @param inputSource 文件输入源
	 */
	protected abstract void validPicture(Object inputSource);

	/**
	 * 获取输入源的原始文件名
	 *
	 * @param inputSource 文件输入源
	 * @return 原始文件名
	 */
	protected abstract String getOriginFilename(Object inputSource);

	/**
	 * 处理输入源并生成本地临时文件
	 *
	 * @param inputSource 文件输入源
	 * @param file        文件对象
	 * @throws Exception e
	 */
	protected abstract void processFile(Object inputSource, File file) throws Exception;

	/**
	 * 封装返回结果
	 */
	private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, CIObject compressedCiObject, CIObject thumbnailCiObject, ImageInfo imageInfo) {
		UploadPictureResult uploadPictureResult = new UploadPictureResult();
		int picWidth = compressedCiObject.getWidth();
		int picHeight = compressedCiObject.getHeight();
		double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
		uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
		uploadPictureResult.setPicWidth(picWidth);
		uploadPictureResult.setPicHeight(picHeight);
		uploadPictureResult.setPicScale(picScale);
		uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
		uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
		uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
		// 设置缩略图
		uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
		// 原图大小/原图 url/缩略图 url
		uploadPictureResult.setOriginSize(FileUtil.size(file));
		uploadPictureResult.setOriginUrl(cosClientConfig.getHost() + "/" + uploadPath);
		// 存储图片主色调
		// uploadPictureResult.setPicColor(imageInfo.getAve());
		uploadPictureResult.setPicColor(cosManager.getImageAve(uploadPath));
		return uploadPictureResult;
	}

	/**
	 * 封装返回结果
	 */
	private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
		UploadPictureResult uploadPictureResult = new UploadPictureResult();
		int picWidth = imageInfo.getWidth();
		int picHeight = imageInfo.getHeight();
		double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
		uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
		uploadPictureResult.setPicWidth(picWidth);
		uploadPictureResult.setPicHeight(picHeight);
		uploadPictureResult.setPicScale(picScale);
		uploadPictureResult.setPicFormat(imageInfo.getFormat());
		uploadPictureResult.setPicSize(FileUtil.size(file));
		uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
		// 原图大小/原图 url/缩略图 url
		uploadPictureResult.setOriginSize(FileUtil.size(file));
		uploadPictureResult.setOriginUrl(cosClientConfig.getHost() + "/" + uploadPath);
		uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + uploadPath);
		// 存储图片主色调
		// uploadPictureResult.setPicColor(imageInfo.getAve());
		uploadPictureResult.setPicColor(cosManager.getImageAve(uploadPath));
		return uploadPictureResult;
	}

	/**
	 * 删除临时文件
	 */
	public void deleteTempFile(File file) {
		if (file == null) {
			return;
		}
		boolean deleteResult = file.delete();
		if (!deleteResult) {
			log.error("file delete error, filepath = {}", file.getAbsolutePath());
		}
	}
}
