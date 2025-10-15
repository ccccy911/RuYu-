package xyz.kuailemao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kuailemao.domain.dto.RoleUserDTO;
import xyz.kuailemao.domain.entity.UserRole;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.service.UserRoleService;

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
        return userRoleService.addUserRole(roleUserDTO);
    }


}
