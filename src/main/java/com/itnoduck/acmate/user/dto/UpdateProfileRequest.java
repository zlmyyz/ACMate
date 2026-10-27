package com.itnoduck.acmate.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 32, message = "昵称长度为 2-32 个字符")
    private String nickname;

    @Size(max = 500, message = "个人简介不能超过 500 字")
    private String bio;
    @Size(max = 128, message = "所在地不能超过 128 字")
    private String location;
    @jakarta.validation.constraints.Pattern(regexp = "|MALE|FEMALE|PRIVATE", message = "请选择有效的性别选项")
    private String gender;
}
