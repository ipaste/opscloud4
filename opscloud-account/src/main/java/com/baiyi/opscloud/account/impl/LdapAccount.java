package com.baiyi.opscloud.account.impl;


import com.baiyi.opscloud.account.Account;
import com.baiyi.opscloud.account.convert.LdapPersonConvert;
import com.baiyi.opscloud.common.util.PasswordUtils;
import com.baiyi.opscloud.domain.generator.OcUser;
import com.baiyi.opscloud.ldap.entry.Person;
import com.baiyi.opscloud.ldap.repo.PersonRepo;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author baiyi
 * @Date 2020/1/3 5:14 下午
 * @Version 1.0
 */
@Component("LdapAccount")
public class LdapAccount extends BaseAccount implements Account {

    @Resource
    private StringEncryptor stringEncryptor;

    @Resource
    private PersonRepo personRepo;

    @Override
    protected List<OcUser> getUserList() {
        return personRepo.getPersonList().stream().map(e -> {
            return LdapPersonConvert.convertPerson(e);
        }).collect(Collectors.toList());
    }

    /**
     * 创建
     *
     * @return
     */
    @Override
    public Boolean create(OcUser user) {
        user.setIsActive(true);
        user.setSource("ldap");
        // 若密码为空生成初始密码
        String password = (StringUtils.isEmpty(user.getPassword()) ? PasswordUtils.getPW(PASSWORD_LENGTH) : user.getPassword());
        user.setPassword(stringEncryptor.encrypt(password)); // 加密
        ocUserService.addOcUser(user);
        return personRepo.create(LdapPersonConvert.convertOcUser(user, password));
    }

    /**
     * 移除
     *
     * @return
     */
    @Override
    public Boolean delete(OcUser user) {
        try {
            if (personRepo.delete(user.getUsername())) {
                ocUserService.delOcUserByUsername(user.getUsername());
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public  Boolean active(OcUser user, boolean active){
        return Boolean.TRUE;
    }

    @Override
    public Boolean update(OcUser user) {
        // 校验用户
        if (StringUtils.isEmpty(user.getUsername()))
            return Boolean.FALSE;
        OcUser ocUser = ocUserService.queryOcUserByUsername(user.getUsername());
        if (ocUser == null) return Boolean.FALSE;

        Person person = new Person();
        person.setUsername(user.getUsername());
        if (!StringUtils.isEmpty(user.getDisplayName())) {
            ocUser.setDisplayName(user.getDisplayName());
            person.setDisplayName(user.getDisplayName());
        }
        if (!StringUtils.isEmpty(user.getEmail())) {
            ocUser.setEmail(user.getEmail());
            person.setEmail(user.getEmail());
        }
        if (!StringUtils.isEmpty(user.getPhone())) {
            ocUser.setPhone(user.getPhone());
            person.setMobile(user.getPhone());
        }

        if (!StringUtils.isEmpty(user.getPassword())) {
            ocUser.setPassword(stringEncryptor.encrypt(user.getPassword())); // 加密
            person.setUserPassword(user.getPassword());
        }
        try {
            ocUserService.updateOcUser(ocUser);
            personRepo.update(person);
            return Boolean.TRUE;
        } catch (Exception e) {
        }
        return Boolean.FALSE;
    }

}
