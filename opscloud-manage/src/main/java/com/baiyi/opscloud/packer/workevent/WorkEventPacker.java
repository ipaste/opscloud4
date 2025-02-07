package com.baiyi.opscloud.packer.workevent;

import com.baiyi.opscloud.common.annotation.AgoWrapper;
import com.baiyi.opscloud.common.annotation.TagsWrapper;
import com.baiyi.opscloud.domain.generator.opscloud.WorkEventProperty;
import com.baiyi.opscloud.domain.generator.opscloud.WorkItem;
import com.baiyi.opscloud.domain.param.IExtend;
import com.baiyi.opscloud.domain.vo.workevent.WorkEventVO;
import com.baiyi.opscloud.packer.IWrapper;
import com.baiyi.opscloud.service.user.UserService;
import com.baiyi.opscloud.service.workevent.WorkEventPropertyService;
import com.baiyi.opscloud.service.workevent.WorkItemService;
import com.baiyi.opscloud.service.workevent.WorkRoleService;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author 修远
 * @Date 2022/8/15 11:11 AM
 * @Since 1.0
 */

@Component
@RequiredArgsConstructor
public class WorkEventPacker implements IWrapper<WorkEventVO.WorkEvent> {

    private final WorkItemService workItemService;

    private final WorkRoleService workRoleService;

    private final UserService userService;

    private final WorkEventPropertyService workEventPropertyService;

    private final static Integer ROOT_PARENT_ID = 0;


    @Override
    @AgoWrapper(extend = true)
    @TagsWrapper
    public void wrap(WorkEventVO.WorkEvent vo, IExtend iExtend) {
        if (!iExtend.getExtend()) return;
        vo.setWorkItem(workItemService.getById(vo.getWorkItemId()));
        List<WorkItem> workItemList = Lists.newArrayList();
        WorkItem workItem = workItemService.getById(vo.getWorkItemId());
        while (!workItem.getParentId().equals(ROOT_PARENT_ID)) {
            workItemList.add(workItem);
            workItem = workItemService.getById(workItem.getParentId());
        }
        workItemList.add(workItem);
        List<String> list = workItemList.stream().map(WorkItem::getWorkItemName).collect(Collectors.toList());
        Collections.reverse(list);
        String workItemTree = Joiner.on("/").join(list);
        vo.setWorkItemTree(workItemTree);
        vo.setWorkRole(workRoleService.getById(vo.getWorkRoleId()));
        vo.setUser(userService.getByUsername(vo.getUsername()));
        List<WorkEventProperty> propertyList = workEventPropertyService.listByWorkEventId(vo.getId());
        if (!CollectionUtils.isEmpty(propertyList)) {
            vo.setPropertyList(propertyList);
            Map<String, String> map = propertyList.stream()
                    .collect(Collectors.toMap(WorkEventProperty::getName, WorkEventProperty::getValue));
            vo.setProperty(map);
        }
    }
}
