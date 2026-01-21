package com.lcs.lcspicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    /*
     * 图片id(用于修改)
     */
    Long id;

    private static final long serialVersionUID = 5866713111006966770L;
}
