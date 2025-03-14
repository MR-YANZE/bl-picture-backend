package com.baolong.blpicturebackend.model.dto.spaceUser;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间用户编辑请求
 */
@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
