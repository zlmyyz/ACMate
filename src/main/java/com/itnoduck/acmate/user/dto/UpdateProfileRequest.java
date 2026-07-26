package com.itnoduck.acmate.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateProfileRequest {

    @Size(min = 2, max = 32, message = "昵称长度为 2-32 个字符")
    private String nickname;

    @Size(max = 500, message = "个人简介不能超过 500 字")
    private String bio;
}
