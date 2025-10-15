package xyz.kuailemao.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.domain.dto.PermissionDTO;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.PermissionVO;
import xyz.kuailemao.mapper.PermissionMapper;
import xyz.kuailemao.service.PermissionService;
import xyz.kuailemao.utils.ControllerUtils;

import java.util.List;

@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    /**
     * 获取所有权限列表
     * @return
     */
    @GetMapping("/list")
    public ResponseResult<List<PermissionVO>> list() {
        return ResponseResult.success(permissionService.selectPermission(null, null, null));
    }


    /**
     * 权限管理
     * @param permissionDesc
     * @param permissionKey
     * @param permissionMenuId
     * @return
     */
    public ResponseResult<List<PermissionVO>> searchPermission(
            @RequestParam(value = "permissionDesc", required = false) String permissionDesc,
            @RequestParam(value = "permissionKey", required = false) String permissionKey,
            @RequestParam(value = "permissionMenuId", required = false) Long permissionMenuId
    ) {
        return ResponseResult.success(permissionService.selectPermission(permissionDesc, permissionKey, permissionMenuId));
    }

    /**
     * 添加权限字符
     * @param permissionDTO
     * @return
     */
    @PostMapping("/add")
    public ResponseResult<Void> addPermission(@RequestBody @Valid PermissionDTO permissionDTO) {
        return permissionService.updateOrInsertPermission(permissionDTO.setId(null));
    }

    /**
     * 修改权限字符
     * @param permissionDTO
     * @return
     */
    @PostMapping("/update")
    public ResponseResult<Void> updatePermission(@RequestBody @Valid PermissionDTO permissionDTO) {
        return permissionService.updateOrInsertPermission(permissionDTO);
    }

    /**
     * 获取要修改的权限信息
     * @param id
     * @return
     */
    @GetMapping("/get/{id}")
    public ResponseResult<PermissionDTO> getPermission(
            @PathVariable("id") @NotNull(message = "权限id不能为空") Long id
    ) {
        return ControllerUtils.messageHandler(() -> permissionService.getPermission(id));
    }


    /**
     * 删除权限信息
     * @param id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    public ResponseResult<Void> deletePermission(
            @PathVariable("id") @NotNull(message = "权限id不能为空") Long id
    ) {
        return permissionService.deletePermission(id);
    }









}
