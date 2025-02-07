package com.baiyi.opscloud.domain.param.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;


public class LoginParam {

    @Builder
    @Data
    @NoArgsConstructor
    @ApiModel
    @AllArgsConstructor
    public static class Login {

        @NotBlank(message = "用户名不能为空")
        @ApiModelProperty(value = "用户名", required = true)
        private String username;

        @ApiModelProperty(value = "密码")
        private String password;

        @ApiModelProperty(value = "一次性密码(OTP)")
        private String otp;

        public boolean isEmptyPassword() {
            return StringUtils.isEmpty(password);
        }
    }

    @Data
    @NoArgsConstructor
    @ApiModel
    @AllArgsConstructor
    public static class PlatformLogin extends Login implements IAuthPlatform {

        @NotBlank(message = "平台名称不能为空")
        @ApiModelProperty(value = "平台名称(用于审计)", required = true)
        public String platform;

        @NotBlank(message = "平台令牌不能为空")
        @ApiModelProperty(value = "平台令牌用于鉴权", required = true)
        public String platformToken;

    }

    @Data
    @NoArgsConstructor
    @ApiModel
    public static class Logout {

        @NotBlank(message = "用户名不能为空")
        @ApiModelProperty(value = "用户名", required = true)
        private String username;

        @NotBlank(message = "令牌不能为空")
        @ApiModelProperty(value = "令牌", required = true)
        private String token;

    }
}
