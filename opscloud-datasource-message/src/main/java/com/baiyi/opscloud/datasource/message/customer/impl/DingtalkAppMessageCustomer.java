package com.baiyi.opscloud.datasource.message.customer.impl;

import com.baiyi.opscloud.common.constant.enums.DsTypeEnum;
import com.baiyi.opscloud.common.datasource.DingtalkConfig;
import com.baiyi.opscloud.common.exception.common.CommonRuntimeException;
import com.baiyi.opscloud.datasource.dingtalk.drive.DingtalkMessageDrive;
import com.baiyi.opscloud.datasource.dingtalk.entity.DingtalkMessage;
import com.baiyi.opscloud.datasource.dingtalk.param.DingtalkMessageParam;
import com.baiyi.opscloud.datasource.message.customer.base.AbstractMessageCustomer;
import com.baiyi.opscloud.domain.generator.opscloud.*;
import com.baiyi.opscloud.domain.types.BusinessTypeEnum;
import com.baiyi.opscloud.domain.types.DsAssetTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author baiyi
 * @Date 2021/12/2 2:56 PM
 * @Version 1.0
 */
@Slf4j
@Component
public class DingtalkAppMessageCustomer extends AbstractMessageCustomer<DingtalkConfig.Dingtalk> {

    @Resource
    private DingtalkMessageDrive dingtalkMessageDrive;

    private DatasourceInstanceAsset findAssetUser(DatasourceInstance instance, User user) {
        List<BusinessAssetRelation> relations =
                businessAssetRelationService.queryBusinessRelations(BusinessTypeEnum.USER.getType(), user.getId());
        for (BusinessAssetRelation relation : relations) {
            DatasourceInstanceAsset asset = dsInstanceAssetService.getById(relation.getDatasourceInstanceAssetId());
            if (asset.getInstanceUuid().equals(instance.getUuid()) && asset.getAssetType().equals(DsAssetTypeEnum.DINGTALK_USER.name()))
                return asset;
        }
        throw new CommonRuntimeException("发送消息失败: 用户未绑定钉钉用户，无法查找对应userid！");
    }

    @Override
    public void send(DatasourceInstance instance, User user, MessageTemplate mt, String text) {
        DatasourceInstanceAsset asset = findAssetUser(instance, user);
        DingtalkMessageParam.Markdown markdown =
                DingtalkMessageParam.Markdown.builder()
                        .title(mt.getTitle())
                        .text(text)
                        .build();
        DingtalkMessageParam.Msg msg = DingtalkMessageParam.Msg.builder()
                .markdown(markdown)
                .build();
        DingtalkMessageParam.AsyncSendMessage message =
                DingtalkMessageParam.AsyncSendMessage.builder()
                        .msg(msg)
                        .useridList(asset.getAssetId())
                        .build();
        // log.info("发送通知 : message = {}",JSONUtil.writeValueAsString(message));
        DingtalkMessage.MessageResponse messageResponse = dingtalkMessageDrive.asyncSend(buildConfig(instance),
                message);
    }

    @Override
    protected DingtalkConfig.Dingtalk buildConfig(DatasourceInstance instance) {
        DatasourceConfig config = getConfig(instance);
        return dsConfigHelper.build(config, DingtalkConfig.class).getDingtalk();
    }

    @Override
    public String getInstanceType() {
        return DsTypeEnum.DINGTALK_APP.getName();
    }

}
