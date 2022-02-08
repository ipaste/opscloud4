package com.baiyi.opscloud.packer.user.am;

import com.baiyi.opscloud.core.factory.DsConfigHelper;
import com.baiyi.opscloud.domain.constants.DsAssetTypeConstants;
import com.baiyi.opscloud.domain.generator.opscloud.DatasourceInstanceAsset;
import com.baiyi.opscloud.domain.param.SimpleExtend;
import com.baiyi.opscloud.domain.param.SimpleRelation;
import com.baiyi.opscloud.domain.vo.datasource.DsAssetVO;
import com.baiyi.opscloud.domain.vo.user.AMVO;
import com.baiyi.opscloud.domain.vo.user.UserVO;
import com.baiyi.opscloud.packer.datasource.DsAssetPacker;
import com.baiyi.opscloud.service.datasource.DsConfigService;
import com.baiyi.opscloud.service.datasource.DsInstanceAssetService;
import com.baiyi.opscloud.service.datasource.DsInstanceService;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author baiyi
 * @Date 2022/2/8 1:13 PM
 * @Version 1.0
 */
@Component
@RequiredArgsConstructor
public class AmPacker {

    private final DsInstanceAssetService dsInstanceAssetService;

    private final DsInstanceService dsInstanceService;

    private final DsAssetPacker dsAssetPacker;

    private final DsConfigService dsConfigService;

    private final DsConfigHelper dsConfigHelper;

    public static final Map<String, Function<DsAssetVO.Asset, AMVO.XAM>> context = new ConcurrentHashMap<>();

    private static final DsAssetTypeConstants[] xamAssetTypes = {DsAssetTypeConstants.RAM_USER, DsAssetTypeConstants.IAM_USER};

    public void wrap(UserVO.User user) {
        Map<String, List<AMVO.XAM>> amMap = Maps.newHashMap();
        for (DsAssetTypeConstants xamAssetType : xamAssetTypes) {
            List<AMVO.XAM> xams = toAms(user, xamAssetType.name());
            if (CollectionUtils.isEmpty(xams)) continue;
            if (amMap.containsKey(xamAssetType.name())) {
                amMap.get(xamAssetType.name()).addAll(xams);
            } else {
                amMap.put(xamAssetType.name(), xams);
            }
        }
        user.setAmMap(amMap);
    }

    public void wrap(UserVO.User user, String xamType) {
        List<AMVO.XAM> xams = toAms(user, xamType);
        if (!CollectionUtils.isEmpty(xams))
            user.setAms(xams);
    }

    private List<AMVO.XAM> toAms(UserVO.User user, String xamType) {
        if (!context.containsKey(xamType)) return Collections.emptyList();
        DatasourceInstanceAsset param = DatasourceInstanceAsset.builder()
                .assetType(xamType)
                .assetKey(user.getUsername())
                .isActive(true)
                .build();
        List<DatasourceInstanceAsset> data = dsInstanceAssetService.queryAssetByAssetParam(param);
        if (CollectionUtils.isEmpty(data)) return Collections.emptyList();
        List<DsAssetVO.Asset> assets = dsAssetPacker.wrapVOList(data, SimpleExtend.EXTEND, SimpleRelation.RELATION);
        Function<DsAssetVO.Asset, AMVO.XAM> converter = context.get(xamType);
        return assets.stream().map(converter).collect(Collectors.toList());
    }

}
