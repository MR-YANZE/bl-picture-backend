package com.baolong.blpicturebackend.comment;

import com.baolong.blpicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用相应类
 *
 * @param <T> T
 */
@Data
public class BaseResponse<T> implements Serializable {

	private int code;

	private T data;

	private String message;

	public BaseResponse(int code, T data, String message) {
		this.code = code;
		this.data = data;
		this.message = message;
	}

	public BaseResponse(int code, T data) {
		this(code, data, "");
	}

	public BaseResponse(ErrorCode errorCode) {
		this(errorCode.getCode(), null, errorCode.getMessage());
	}
}

