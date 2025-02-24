package com.baolong.blpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.baolong.blpicturebackend.config.CosClientConfig;
import com.baolong.blpicturebackend.exception.BusinessException;
import com.baolong.blpicturebackend.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 腾讯云对象存储服务
 * <p>
 * 叫做 XXXManager 主要是做区分, 表示这个类是可以单独抽取出去的
 */
@Slf4j
@Component
public class CosManager {

	@Resource
	private CosClientConfig cosClientConfig;

	@Resource
	private COSClient cosClient;

	/**
	 * 上传对象
	 *
	 * @param key  唯一键
	 * @param file 文件
	 */
	public PutObjectResult putObject(String key, File file) {
		PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
		return cosClient.putObject(putObjectRequest);
	}

	/**
	 * 下载对象
	 *
	 * @param key 唯一键
	 */
	public COSObject getObject(String key) {
		GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
		return cosClient.getObject(getObjectRequest);
	}

	/**
	 * 上传对象（附带图片信息）
	 * <p>
	 * <a href="https://cloud.tencent.com/document/product/436/55377">这里用到了 腾讯 COS 的数据万象</a>
	 *
	 * @param key  唯一键
	 * @param file 文件
	 */
	public PutObjectResult putPictureObject(String key, File file) {
		PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
		// 对图片进行处理（获取基本信息也被视作为一种处理）
		PicOperations picOperations = new PicOperations();
		// 1 表示返回原图信息
		picOperations.setIsPicInfo(1);

		// 规则处理: 处理图片压缩转成 webp 格式
		List<PicOperations.Rule> rules = new ArrayList<>();
		String webpKey = FileUtil.mainName(key) + ".webp";
		PicOperations.Rule compressRule = new PicOperations.Rule();
		compressRule.setFileId(webpKey);
		compressRule.setBucket(cosClientConfig.getBucket());
		compressRule.setRule("imageMogr2/format/webp");
		rules.add(compressRule);

		// 针对大于 20KB 的图片处理
		if (file.length() > 2 * 1024) {
			// 规则处理: 处理缩略图
			PicOperations.Rule thumbnailRule = new PicOperations.Rule();
			// 这里用的是原图的后缀, 不是 图片压缩转换格式的 webp 后缀, 因为这里是针对原图处理的
			thumbnailRule.setFileId(FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key));
			thumbnailRule.setBucket(cosClientConfig.getBucket());
			thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 128, 128));
			rules.add(thumbnailRule);
		}

		// 把规则参数传入构造
		picOperations.setRules(rules);

		// 构造处理参数
		putObjectRequest.setPicOperations(picOperations);
		return cosClient.putObject(putObjectRequest);
	}

	/**
	 * 删除对象
	 *
	 * @param key 文件 key
	 */
	public void deleteObject(String key) {
		cosClient.deleteObject(cosClientConfig.getBucket(), key);
	}


	/**
	 * 获取图片主色调
	 *
	 * @param imageKey 图片 key
	 * @return 图片主色调
	 */
	public String getImageAve(String imageKey) {
		GetObjectRequest objectRequest = new GetObjectRequest(cosClientConfig.getBucket(), imageKey);
		// 设置图片处理规则为获取主色调
		String rule = "imageAve";
		objectRequest.putCustomQueryParameter(rule, null);
		// 获取对象
		COSObject cosObject = cosClient.getObject(objectRequest);
		try (
				COSObjectInputStream cosIp = cosObject.getObjectContent();
				ByteArrayOutputStream op = new ByteArrayOutputStream()
		) {
			// 读取流的内容
			byte[] bytes = new byte[1024];
			int len;
			while ((len = cosIp.read(bytes)) != -1) {
				op.write(bytes, 0, len);
			}
			// 将字节数组转换为字符串
			String aveColor = op.toString(StandardCharsets.UTF_8);
			System.out.println("数据: "+ JSONUtil.parse(aveColor));
			return JSONUtil.parseObj(aveColor).getStr("RGB");
		} catch (Exception e) {
			log.error("获取图片主色调失败", e);
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取图片主色调失败");
		}
	}
}
