package com.baiyi.opscloud.datasource.manager.base;

import com.baiyi.opscloud.common.constant.DsInstanceTagConstants;
import com.baiyi.opscloud.common.constant.enums.DsTypeEnum;
import com.baiyi.opscloud.core.InstanceHelper;
import com.baiyi.opscloud.datasource.message.notice.NoticeHelper;
import com.baiyi.opscloud.domain.generator.opscloud.DatasourceInstance;
import com.baiyi.opscloud.domain.generator.opscloud.User;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author baiyi
 * @Date 2021/12/2 11:02 AM
 * @Version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeManager {

    private final InstanceHelper instanceHelper;

    public interface MsgKeys {
        String CREATE_USER = "CREATE_USER";
        String UPDATE_USER_PASSWORD = "UPDATE_USER_PASSWORD";
    }

    /**
     * 支持通知的实例类型
     */
    private static final DsTypeEnum[] FILTER_INSTANCE_TYPES = {DsTypeEnum.DINGTALK, DsTypeEnum.DINGTALK_APP};

    @Resource
    private NoticeHelper noticeHelper;

    public void sendMessage(User user, String msgKey) {
        List<DatasourceInstance> instances = instanceHelper.listInstance(FILTER_INSTANCE_TYPES,
                DsInstanceTagConstants.NOTICE.getTag());
        if (!CollectionUtils.isEmpty(instances)) {
            noticeHelper.sendMessage(user, msgKey, instances);
        }
    }
}
