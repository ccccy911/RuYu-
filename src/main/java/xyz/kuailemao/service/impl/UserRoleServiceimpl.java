package xyz.kuailemao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.kuailemao.domain.dto.RoleUserDTO;
import xyz.kuailemao.domain.entity.UserRole;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.mapper.UserRoleMapper;
import xyz.kuailemao.service.UserRoleService;
import xyz.kuailemao.utils.SecurityUtils;

import java.security.Security;
import java.util.List;

@Service
public class UserRoleServiceimpl extends ServiceImpl<UserRoleMapper, UserRole>  implements UserRoleService {

    private final UserRoleMapper userRoleMapper;
    private final UserRoleService userRoleService;

    public UserRoleServiceimpl(UserRoleMapper userRoleMapper, UserRoleService userRoleService) {
        this.userRoleMapper = userRoleMapper;
        this.userRoleService = userRoleService;
    }

    /**
     * 添加用户——角色关系
     * @param roleUserDTO
     * @return
     */
    @Override
    public ResponseResult<Void> addUserRole(RoleUserDTO roleUserDTO) {
        List<Long> roleIds = roleUserDTO.getRoleId();
        List<UserRole> userRoleList = roleIds.stream().map(roleId->{
            UserRole userRole = new UserRole();
            userRole.setRoleId(roleId);
            userRole.setUserId(SecurityUtils.getUserId());
            return userRole;
        }).toList();
        if(userRoleService.saveBatch(userRoleList)){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


}
