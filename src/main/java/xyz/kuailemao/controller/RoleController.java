package xyz.kuailemao.controller;

import jakarta.validation.constraints.PositiveOrZero;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.domain.dto.*;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.RoleAllVO;
import xyz.kuailemao.domain.vo.RoleByIdVO;
import xyz.kuailemao.service.RoleService;
import xyz.kuailemao.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/role")
public class RoleController {



    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;


    /**
     * 添加角色信息
     * @param roleDTO
     * @return
     */
    @PutMapping("/add")
    public ResponseResult<Void> addRole(@RequestBody RoleDTO roleDTO){
        return roleService.addRole(roleDTO);
    }

    /**
     * 修改角色信息
     * @param roleDTO
     * @return
     */
    @PutMapping("/update")
    public ResponseResult<Void> updateRole(@RequestBody RoleDTO roleDTO){
        return roleService.updateRole(roleDTO);
    }

    /**
     * 更新角色状态
     * @param roleDTO
     * @return
     */
    @PostMapping("/update/status")
    public ResponseResult<Void> updateRoleStatus(@RequestBody RoleDTO roleDTO){
        return roleService.updateRoleStatus(roleDTO);
    }


    /**
     * 根据条件搜索角色
     * @param roleSearchDTO
     * @return
     */
    @PostMapping("/search")
    public ResponseResult<List<RoleAllVO>> searchRole(@RequestBody RoleSearchDTO roleSearchDTO){
        return roleService.searchRole(roleSearchDTO);
    }

    /**
     * 获取角色列表
     * @param
     * @return
     */
    @GetMapping("/list")
    public ResponseResult<List<RoleAllVO>> getRoleList(){
        return roleService.getRoleList();
    }

    /**
     * 根据id获取角色信息
     * @param id
     * @return
     */
    @GetMapping("/get/{id}")
    public ResponseResult<RoleByIdVO> getRole(@PathVariable("id") Integer id) {
        return roleService.getRole(id);
    }

    /**
     * 根据ids删除角色
     * @param roleDeleteDTO
     * @return
     */
    @DeleteMapping("delete")
    public ResponseResult<Void> deleteRole(@RequestBody RoleDeleteDTO roleDeleteDTO) {
         roleService.removeByIds(roleDeleteDTO.getIds());
         return ResponseResult.success();
    }




}
