package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.constraints.NotNull;
import xyz.kuailemao.domain.dto.PermissionDTO;
import xyz.kuailemao.domain.entity.Permission;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.PermissionVO;
import xyz.kuailemao.mapper.PermissionMapper;

import java.util.List;

public interface  PermissionService  extends IService<Permission> {


    List<PermissionVO> selectPermission(String permissionDesc, String permissionKey, Long permissionMenuId);

    ResponseResult<Void> updateOrInsertPermission(PermissionDTO permissionDTO);

    PermissionDTO getPermission(@NotNull(message = "权限id不能为空") Long id);

    ResponseResult<Void> deletePermission(@NotNull(message = "权限id不能为空") Long id);
}
