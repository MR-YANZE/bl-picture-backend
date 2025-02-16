package com.baolong.blpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求
 */
@Data
public class PictureUploadRequest implements Serializable {

	/**
	 * 图片 id（用于修改）
	 */
	private Long id;

	/**
	 * 文件地址
	 */
	private String fileUrl;

	/**
	 * 图片名称
	 */
	private String picName;

	/**
	 * 分类
	 */
	private String category;

	/**
	 * 标签
	 */
	private String tags;

	private static final long serialVersionUID = 1L;
}
