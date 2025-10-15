package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kuailemao.domain.dto.RoleUserDTO;
import xyz.kuailemao.domain.entity.UserRole;
import xyz.kuailemao.domain.response.ResponseResult;

public interface UserRoleService extends IService<UserRole> {

    ResponseResult<Void> addUserRole(RoleUserDTO roleUserDTO);
}
