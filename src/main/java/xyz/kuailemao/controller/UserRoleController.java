package xyz.kuailemao.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.domain.dto.RoleUserDTO;
import xyz.kuailemao.domain.dto.UserRoleDTO;
import xyz.kuailemao.domain.entity.UserRole;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.RoleAllVO;
import xyz.kuailemao.domain.vo.RoleUserVO;
import xyz.kuailemao.service.UserRoleService;
import xyz.kuailemao.utils.ControllerUtils;

import java.util.List;

@RequestMapping("/user/role")
@RestController
public class UserRoleController {

    @Autowired
    private UserRoleService userRoleService;

    /**
     * 添加角色用户关系
     */

    @GetMapping("/user/add")
    public ResponseResult<Void> userRoleAdd(@RequestBody RoleUserDTO roleUserDTO){
        return userRoleService.addRoleUser(roleUserDTO);
    }

    /**
     * 添加用户角色关系
     * @param userRoleDTO
     * @return
     */
    @PostMapping("/add")
    public ResponseResult<Void> addUserRole(@Valid @RequestBody UserRoleDTO userRoleDTO) {
        return userRoleService.addUserRole(userRoleDTO);
    }



    /**
     * 删除角色用户关系
     * @param roleUserDTO
     * @return
     */
    @DeleteMapping("/user/delete")
    public ResponseResult<Void> deleteRoleUser(@Valid @RequestBody RoleUserDTO roleUserDTO) {
        return userRoleService.deleteRoleUser(roleUserDTO);
    }


    /**
     * 删除用户角色关系
     * @param userRoleDTO
     * @return
     */
    @DeleteMapping("/delete")
    public ResponseResult<Void> deleteUserRole(@Valid @RequestBody UserRoleDTO userRoleDTO) {
        return userRoleService.deleteUserRole(userRoleDTO);
    }


    /**
     * 用户角色详情页（查询）
     * @param userId
     * @param roleName
     * @param roleKey
     * @return
     */
    @GetMapping("/role/list")
    public ResponseResult<List<RoleAllVO>> selectPermissionIdRole(
            @NotNull(message = "用户id不能为空") Long userId,
            @RequestParam(required = false, name = "roleName") String roleName,
            @RequestParam(required = false, name = "roleKey") String roleKey
    ) {
        return ControllerUtils.messageHandler(() -> userRoleService.selectRoleByUserId(userId,roleName,roleKey,0));
    }


    /**
     * 添加角色页面
     * @param userId
     * @param roleName
     * @param roleKey
     * @return
     */
    @GetMapping("/not/role/list")
    public ResponseResult<List<RoleAllVO>> selectUserNotRole(
            @NotNull(message = "用户id不能为空") Long userId,
            @RequestParam(required = false, name = "roleName") String roleName,
            @RequestParam(required = false, name = "roleKey") String roleKey
    ) {
        return ControllerUtils.messageHandler(() -> userRoleService.selectRoleByUserId(userId,roleName,roleKey,1));
    }

    /**
     * 角色用户详情页（查询）
     * @param roleId
     * @param username
     * @param email
     * @return
     */
    @GetMapping("/user/list")
    public ResponseResult<List<RoleUserVO>> selectUser(
            @NotNull(message = "角色id不能为空") Long roleId,
            @RequestParam(required = false,name = "username") String username,
            @RequestParam(required = false,name = "email") String email
    ) {
        return ControllerUtils.messageHandler(() -> userRoleService.selectRoleUser(roleId,username,email,0));
    }


    /**
     * 角色添加用户页面
     * @param roleId
     * @param username
     * @param email
     * @return
     */
    @GetMapping("/not/user/list")
    public ResponseResult<List<RoleUserVO>> selectNotUserByRole(
            @NotNull(message = "角色id不能为空") Long roleId,
            @RequestParam(required = false,name = "username") String username,
            @RequestParam(required = false,name = "email") String email
    ) {
        return ControllerUtils.messageHandler(() -> userRoleService.selectRoleUser(roleId,username,email,1));
    }







}
