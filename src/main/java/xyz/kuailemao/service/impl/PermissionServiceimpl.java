package xyz.kuailemao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.kuailemao.domain.dto.PermissionDTO;
import xyz.kuailemao.domain.entity.Menu;
import xyz.kuailemao.domain.entity.Permission;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.PermissionVO;
import xyz.kuailemao.mapper.MenuMapper;
import xyz.kuailemao.mapper.PermissionMapper;
import xyz.kuailemao.service.MenuService;
import xyz.kuailemao.service.PermissionService;
import xyz.kuailemao.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class PermissionServiceimpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {


    private final MenuMapper menuMapper;

    public PermissionServiceimpl(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Override
    public List<PermissionVO> selectPermission(String permissionDesc, String permissionKey, Long permissionMenuId) {
        //先查权限列表
        LambdaQueryWrapper<Permission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Objects.nonNull(permissionDesc),Permission::getPermissionDesc, permissionDesc)
                .like(Objects.nonNull(permissionKey),Permission::getPermissionKey, permissionKey)
                .eq(Objects.nonNull(permissionMenuId),Permission::getMenuId, permissionMenuId);
        List<Permission> permissions = baseMapper.selectList(queryWrapper);
        if(StringUtils.isNotEmpty(permissions)){
            List<PermissionVO> permissionVOS = permissions.stream().map(permission -> {
                PermissionVO permissionVO = BeanUtil.copyProperties(permission, PermissionVO.class);
                permissionVO.setMenuName(menuMapper.selectOne(new LambdaQueryWrapper<Menu>().eq(Menu::getId,permission.getMenuId())).getName());
                return permissionVO;
            }).collect(Collectors.toList());
            return permissionVOS;
        }
        return null;

    }


    @Autowired
    private PermissionMapper permissionMapper;

    /**
     * 添加权限
     * @param permissionDTO
     * @return
     */
    @Override
    public ResponseResult<Void> updateOrInsertPermission(PermissionDTO permissionDTO) {
        // 权限字符是否重复
        Permission isPermission = permissionMapper.selectOne(new LambdaQueryWrapper<Permission>().eq(Permission::getPermissionKey, permissionDTO.getPermissionKey().trim()));
        if (StringUtils.isNotNull(isPermission) && !isPermission.getId().equals(permissionDTO.getId())) {
            return ResponseResult.failure("权限字符不可重复");
        }
        if(this.save(BeanUtil.copyProperties(permissionDTO,Permission.class))) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


    /**
     * 获取要修改的权限信息
     * @param id
     * @return
     */
    @Override
    public PermissionDTO getPermission(Long id) {
        Permission permission = getById(id);
        return permission.asViewObject(PermissionDTO.class, v -> v.setPermissionMenuId(permission.getMenuId()));
    }


    /**
     * 删除权限关系
     * @param id
     * @return
     */
    @Override
    public ResponseResult<Void> deletePermission(Long id) {
        if(permissionMapper.deleteById(id) > 0){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }
}
