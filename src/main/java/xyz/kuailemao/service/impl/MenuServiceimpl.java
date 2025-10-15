package xyz.kuailemao.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import xyz.kuailemao.domain.dto.MenuDTO;
import xyz.kuailemao.domain.entity.*;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.MenuByIdVO;
import xyz.kuailemao.domain.vo.MenuVO;
import xyz.kuailemao.enums.RespEnum;
import xyz.kuailemao.mapper.*;
import xyz.kuailemao.service.MenuService;
import xyz.kuailemao.service.RoleMenuService;
import xyz.kuailemao.service.RoleService;
import xyz.kuailemao.utils.SecurityUtils;
import xyz.kuailemao.utils.ServiceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MenuServiceimpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {


    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleMenuService roleMenuService;

    /**
     * 添加菜单
     * @param menuDTO
     * @return
     */
    @Override
    public ResponseResult<Void> addMenu(MenuDTO menuDTO) {
        //判断是哪种组件类型：0:普通组件。1：跳转页面的组件。2:没有具体页面，点击后展示子类组件的组件
        if (menuDTO.getRouterType() == null) {
            menuDTO.setRouterType(0);
        }
        switch (menuDTO.getRouterType()) {
            // 普通组件
            case 0 -> {
                if (Objects.equals(menuDTO.getComponent(), "")) {
                    menuDTO.setComponent("RouteView");
                }
            }
            case 1 -> menuDTO.setComponent("Iframe");
            case 2 -> menuDTO.setComponent(null);
        }

        Menu menu = menuDTO.asViewObject(Menu.class);
        if (this.save(menu)) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Autowired
    private MenuMapper menuMapper;


    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private RoleMapper roleMapper;


    /**
     * 根据id查询菜单信息
     * @param id
     * @return
     */
    @Override
    public ResponseResult<MenuByIdVO> selectUpdateMenu(Long id) {
        Menu menu = this.getById(id);
        List<Long> roleIds = roleMenuMapper
                .selectList(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, menu.getId()))
                .stream().map(RoleMenu::getRoleId).toList();
        List<Long> roles;
        if (!roleIds.isEmpty()) roles = roleMapper.selectBatchIds(roleIds).stream().map(Role::getId).toList();
        else roles = null;
        MenuByIdVO vo = menu.asViewObject(MenuByIdVO.class, v -> {
            if (v.getComponent() == null) v.setRouterType(2L);
            else if (v.getComponent().equals("Iframe")) v.setRouterType(1L);
            else v.setRouterType(0L);
            v.setRoleId(roles);
        });
        return ResponseResult.success(vo);
    }


    /**
     * 修改菜单
     * @param menuDTO
     * @return
     */
    @Override
    public ResponseResult<Void> updateMenu(MenuDTO menuDTO) {
        LambdaUpdateWrapper<Menu> wrapper = new LambdaUpdateWrapper<>();
        if (menuDTO.getRouterType() == 3) {
            // 父菜单
            menuDTO.setComponent("RouteView");
        }
        if (menuDTO.getRouterType() == 0) wrapper.set(Menu::getRedirect,null);
        if (menuDTO.getRouterType() == 0 || menuDTO.getRouterType() == 3) {
            wrapper.set(Menu::getUrl, null);
        }
        if (menuDTO.getRouterType() == 1) {
            menuDTO.setComponent("Iframe");
            wrapper.set(Menu::getTarget, null);
        }
        if (menuDTO.getRouterType() == 2) {
            wrapper.set(Menu::getComponent, null);
            wrapper.set(Menu::getRedirect, null);
        }
        if (menuDTO.getParentId() == null) wrapper.set(Menu::getParentId,null);

        Menu menu = menuDTO.asViewObject(Menu.class);

        //先删除原来的全部角色
        roleMenuMapper.deleteById(menu.getId());
        //添加现在的全部角色
        List<RoleMenu> roleMenus = menuDTO.getRoleId().stream().map(roleId -> new RoleMenu(roleId, menu.getId())).collect(Collectors.toList());
        roleMenuService.saveBatch(roleMenus);
        //添加其他信息到菜单表
        if(menuMapper.updateById(menu)>0){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Autowired
    private PermissionMapper permissionMapper;

    /**
     * 根据id删除菜单
     * @param id
     * @return
     */
    @Override
    public ResponseResult<String> deleteMenu(Long id) {
        //是否还有未删除的子目录
        if(menuMapper.selectCount(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id)) > 0){
            return ResponseResult.failure(RespEnum.NO_DELETE_CHILD_MENU.getCode(),RespEnum.NO_DELETE_CHILD_MENU.getMsg());
        }
        //删除菜单表
        this.menuMapper.deleteById(id);
        //删除菜单-角色表
        roleMenuMapper.deleteById(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, id));
        // 删除菜单对应权限
        permissionMapper.delete(new LambdaQueryWrapper<Permission>().eq(Permission::getMenuId, id));
        return ResponseResult.success();
    }



    @Autowired
    private UserMapper userMapper;


    @Autowired
    private UserRoleMapper userRoleMapper;

    /**
     * 搜索管理菜单列表
     * @param typeId
     * @param username
     * @param status
     * @return
     */
    @Override
    public ResponseResult<List<MenuVO>> getMenuList(Integer typeId, String username, Integer status) {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Menu::getOrderNum);
        // 路由菜单
        if (typeId == 0) {
            // 1.使用用户id查询用户角色
            List<Long> roleIds = userRoleMapper
                    .selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, SecurityUtils.getUserId()))
                    .stream().map(UserRole::getRoleId).toList();
            // 2.使用角色id查询对应拥有的菜单
            List<Long> menuIds = new java.util.ArrayList<>(roleMenuMapper.selectBatchIds(roleIds).stream().map(RoleMenu::getMenuId).toList());
            // 关系表里面所有的菜单id
            List<Long> roleMenuAllIds = roleMenuService.list().stream().map(RoleMenu::getMenuId).toList();
            // 所有的菜单id
            List<Long> menuAllIds = this.list().stream().map(Menu::getId).toList();
            // 没有设置权限的菜单
            List<Long> noRoleByMenuIds = menuAllIds.stream().filter(menuId -> !roleMenuAllIds.contains(menuId)).toList();
            // 组合list
            menuIds.addAll(noRoleByMenuIds);

            // 显示的菜单必须是用户角色有的或者没有设置权限的菜单
            if (!menuIds.isEmpty()) {
                wrapper.in(Menu::getId, menuIds);
            }
            wrapper.eq(Menu::getIsDisable, 0);
        }
        if (typeId == 1 && (username != null || status != null)) {
            wrapper.eq(status!= null,Menu::getIsDisable, status).and(o -> o.like(Menu::getTitle, username));
        }
        List<Menu> menus = menuMapper.selectList(wrapper);
        List<MenuVO> list = menus.stream().map(menu -> MenuVO.builder()
                .id(menu.getId())
                .title(menu.getTitle())
                .icon(menu.getIcon())
                .path(menu.getPath())
                .component(menu.getComponent())
                .redirect(menu.getRedirect())
                .affix(ServiceUtil.isFalseOrTrue(menu.getAffix()))
                .parentId(menu.getParentId())
                .name(menu.getName())
                .hideInMenu(ServiceUtil.isFalseOrTrue(menu.getHideInMenu()))
                .url(menu.getUrl())
                .keepAlive(ServiceUtil.isFalseOrTrue(menu.getKeepAlive()))
                .target(menu.getTarget())
                .isDisable(!ServiceUtil.isFalseOrTrue(menu.getIsDisable()))
                .orderNum(menu.getOrderNum())
                .createTime(menu.getCreateTime())
                .build()).toList();

        return ResponseResult.success(list);
    }


}
