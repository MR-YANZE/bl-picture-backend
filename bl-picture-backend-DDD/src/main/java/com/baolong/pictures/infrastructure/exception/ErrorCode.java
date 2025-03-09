package com.baolong.pictures.infrastructure.exception;

import lombok.Getter;

/**
 * 错误码枚举类
 */
@Getter
public enum ErrorCode {

	SUCCESS(0, "ok"),
	FAILED(444, "err"),
	PARAMS_ERROR(40000, "请求参数错误"),
	NOT_LOGIN_ERROR(40100, "未登录"),
	NO_AUTH_ERROR(40101, "无权限"),
	NOT_FOUND_ERROR(40400, "请求数据不存在"),
	FORBIDDEN_ERROR(40300, "禁止访问"),
	SYSTEM_ERROR(50000, "系统内部异常"),
	REQUEST_LIMIT_ERROR(50005, "访问过于频繁，请稍候再试"),
	OPERATION_ERROR(50001, "操作失败");

	/**
	 * 状态码
	 */
	private final int code;

	/**
	 * 信息
	 */
	private final String message;

	ErrorCode(int code, String message) {
		this.code = code;
		this.message = message;
	}

}
