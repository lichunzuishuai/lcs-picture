package com.lcs.lcspicture.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {
    /*
    要删除的id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
