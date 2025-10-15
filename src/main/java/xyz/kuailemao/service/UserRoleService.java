package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import xyz.kuailemao.domain.dto.RoleUserDTO;
import xyz.kuailemao.domain.dto.UserRoleDTO;
import xyz.kuailemao.domain.entity.UserRole;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.RoleAllVO;
import xyz.kuailemao.domain.vo.RoleUserVO;

import java.util.List;

public interface UserRoleService extends IService<UserRole> {

    ResponseResult<Void> addRoleUser(RoleUserDTO roleUserDTO);

    ResponseResult<Void> addUserRole(@Valid UserRoleDTO userRoleDTO);

    ResponseResult<Void> deleteRoleUser(@Valid RoleUserDTO roleUserDTO);

    ResponseResult<Void> deleteUserRole(@Valid UserRoleDTO userRoleDTO);

    List<RoleAllVO> selectRoleByUserId(@NotNull(message = "用户id不能为空") Long userId, String roleName, String roleKey, Integer type);

    List<RoleUserVO> selectRoleUser(@NotNull(message = "角色id不能为空") Long roleId, String username, String email, Integer type);
}
