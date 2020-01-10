package com.baiyi.opscloud.domain.bo;

import com.baiyi.opscloud.domain.generator.OcEnv;
import com.baiyi.opscloud.domain.generator.OcServer;
import lombok.Builder;
import lombok.Data;

/**
 * @Author baiyi
 * @Date 2020/1/10 1:45 下午
 * @Version 1.0
 */
@Builder
@Data
public class OcServerBO {
    private OcServer ocServer;
    private OcEnv ocEnv;
}
