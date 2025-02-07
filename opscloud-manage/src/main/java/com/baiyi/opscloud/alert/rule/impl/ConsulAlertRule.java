package com.baiyi.opscloud.alert.rule.impl;

import com.baiyi.opscloud.alert.strategy.AlertStrategyFactory;
import com.baiyi.opscloud.alert.strategy.IAlertStrategy;
import com.baiyi.opscloud.common.alert.AlertContext;
import com.baiyi.opscloud.common.alert.AlertNotifyMedia;
import com.baiyi.opscloud.common.alert.AlertRuleMatchExpression;
import com.baiyi.opscloud.common.alert.Metadata;
import com.baiyi.opscloud.common.constants.enums.DsTypeEnum;
import com.baiyi.opscloud.common.datasource.ConsulConfig;
import com.baiyi.opscloud.common.util.IdUtil;
import com.baiyi.opscloud.datasource.consul.driver.ConsulServiceDriver;
import com.baiyi.opscloud.datasource.consul.entity.ConsulHealth;
import com.baiyi.opscloud.domain.annotation.InstanceHealth;
import com.baiyi.opscloud.domain.constants.BusinessTypeEnum;
import com.baiyi.opscloud.domain.constants.DsAssetTypeConstants;
import com.baiyi.opscloud.domain.generator.opscloud.*;
import com.baiyi.opscloud.domain.param.datasource.DsAssetParam;
import com.baiyi.opscloud.domain.vo.datasource.DsAssetVO;
import com.baiyi.opscloud.service.application.ApplicationService;
import com.baiyi.opscloud.service.user.UserPermissionService;
import com.baiyi.opscloud.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import static com.baiyi.opscloud.common.base.Global.ENV_PROD;

/**
 * @Author 修远
 * @Date 2022/7/21 5:14 PM
 * @Since 1.0
 */

@Slf4j
@Component
public class ConsulAlertRule extends AbstractAlertRule {

    @Resource
    private ApplicationService applicationService;

    @Resource
    private UserPermissionService userPermissionService;

    @Resource
    private UserService userService;

    @Resource
    private ConsulServiceDriver consulServiceDriver;

    private static final String DATA_CENTER = "dc1";

    private static final String HEALTHY_STATUS = "passing";

    @InstanceHealth
    @Scheduled(cron = "10 */1 * * * ?")
    @SchedulerLock(name = "consul_alert_rule_evaluate_task", lockAtMostFor = "30s", lockAtLeastFor = "30s")
    public void ruleEvaluate() {
        // 非生产环境不执行任务
        if (ENV_PROD.equals(env)) {
            log.info("consul 告警规则评估");
            preData();
            List<DatasourceInstance> datasourceInstances = dsInstanceService.listByInstanceType(getInstanceType());
            datasourceInstances.forEach(dsInstance -> {
                DsAssetParam.AssetPageQuery pageQuery = DsAssetParam.AssetPageQuery.builder()
                        .assetType(DsAssetTypeConstants.CONSUL_SERVICE.name())
                        .extend(true)
                        .instanceId(dsInstance.getId())
                        .length(1000)
                        .page(1)
                        .queryName("")
                        .relation(false)
                        .build();
                List<DsAssetVO.Asset> assetList = dsInstanceAssetFacade.queryAssetPage(pageQuery).getData();
                assetList.forEach(asset ->
                        evaluate(asset, getConfig(dsInstance.getUuid()).getStrategyMatchExpressions())
                );
            });
            log.info("consul 告警规则结束");
        }
    }

    private ConsulConfig.Consul getConfig(String instanceUuid) {
        DatasourceConfig dsConfig = DS_CONFIG_MAP.get(instanceUuid);
        return dsConfigHelper.build(dsConfig, ConsulConfig.class).getConsul();
    }

    @Override
    public Boolean evaluate(DsAssetVO.Asset asset, AlertRuleMatchExpression matchExpression) {
        if ("0".equals(asset.getProperties().get("checksCritical")))
            return false;
        List<ConsulHealth.Health> healthList = consulServiceDriver.listHealthService(getConfig(asset.getInstanceUuid()), asset.getName(), DATA_CENTER);
        List<String> warningNode = getWarningNode(healthList);
        if (CollectionUtils.isEmpty(warningNode))
            return false;
        double warningNum = warningNode.size();
        double totalNum = healthList.size();
        if (NumberUtils.isDigits(matchExpression.getValues()))
            return warningNum >= Integer.parseInt(matchExpression.getValues());
        try {
            double percent = NumberFormat.getPercentInstance().parse(matchExpression.getValues()).doubleValue();
            return warningNum / totalNum >= percent;
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    public List<String> getWarningNode(List<ConsulHealth.Health> healthList) {
        return healthList.stream()
                .filter(health ->
                        health.getChecks().stream()
                                .anyMatch(check -> !HEALTHY_STATUS.equals(check.getStatus()))
                ).map(health -> health.getService().getAddress())
                .collect(Collectors.toList());
    }

    @Override
    protected AlertContext converterContext(DsAssetVO.Asset asset, AlertRuleMatchExpression matchExpression) {
        DatasourceInstance datasourceInstance = dsInstanceService.getByUuid(asset.getInstanceUuid());
        List<ConsulHealth.Health> healthList = consulServiceDriver.listHealthService(getConfig(asset.getInstanceUuid()), asset.getName(), DATA_CENTER);
        List<String> warningNode = getWarningNode(healthList);
        if (CollectionUtils.isEmpty(warningNode)) return null;
        List<Metadata> metadata = warningNode.stream()
                .map(node -> Metadata.builder()
                        .name(node)
                        .build()
                ).collect(Collectors.toList());
        return AlertContext.builder()
                .eventUuid(IdUtil.buildUUID())
                .alertName("Consul 节点异常告警")
                .severity(matchExpression.getSeverity())
                .message("Consul 不可用节点大于 " + matchExpression.getValues())
                .value(String.valueOf(warningNode.size()))
                .check(datasourceInstance.getInstanceName())
                .source(datasourceInstance.getUuid())
                .alertType(asset.getKind())
                .service(asset.getName())
                .alertTime(System.currentTimeMillis())
                .metadata(metadata)
                .build();
    }

    @Override
    public void execute(AlertContext context) {
        if (ObjectUtils.isEmpty(context)) return;
        IAlertStrategy alertStrategy = AlertStrategyFactory.getAlertActivity(context.getSeverity());
        Application application = applicationService.getByName(context.getService());
        UserPermission userPermission = UserPermission.builder()
                .businessId(application.getId())
                .businessType(BusinessTypeEnum.APPLICATION.getType())
                .build();
        List<UserPermission> userPermissions = userPermissionService.queryByBusiness(userPermission).stream()
                .filter(x -> "admin".equals(x.getPermissionRole()))
                .collect(Collectors.toList());
        List<User> users = userPermissions.stream()
                .map(permission -> userService.getById(permission.getUserId()))
                .filter(User::getIsActive)
                .collect(Collectors.toList());
        ConsulConfig.Consul consul = getConfig(context.getSource());
        AlertNotifyMedia media = AlertNotifyMedia.builder()
                .users(users)
                .dingtalkToken(consul.getDingtalkToken())
                .ttsCode(consul.getTtsCode())
                .templateCode(consul.getTemplateCode())
                .build();
        alertStrategy.executeAlertStrategy(media, context);
    }

    @Override
    public String getInstanceType() {
        return DsTypeEnum.CONSUL.name();
    }
}
