package com.baolong.blpicturebackend.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间用户上传分析响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUserAnalyzeResponse implements Serializable {

	/**
	 * 时间区间
	 */
	private String period;

	/**
	 * 上传数量
	 */
	private Long count;

	private static final long serialVersionUID = 1L;
}
