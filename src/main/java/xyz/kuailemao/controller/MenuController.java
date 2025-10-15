package xyz.kuailemao.controller;

import jakarta.validation.constraints.NotNull;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.domain.dto.MenuDTO;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.MenuByIdVO;
import xyz.kuailemao.domain.vo.MenuVO;
import xyz.kuailemao.domain.vo.RoleVO;
import xyz.kuailemao.service.MenuService;
import xyz.kuailemao.service.RoleService;

import java.util.List;

@RequestMapping("menu")
@RestController
public class MenuController {


    @Autowired
    private MenuService menuService;

    /**
     * 添加菜单
     * @param menuDTO
     * @return
     */
    @PostMapping
    public ResponseResult<Void> add(@RequestBody MenuDTO menuDTO) {
        return menuService.addMenu(menuDTO);
    }


    /**
     * 根据id查询菜单信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseResult<MenuByIdVO> getById(@PathVariable("id") @NotNull Long id) {
        return menuService.selectUpdateMenu(id);
    }

    /**
     * 修改菜单
     * @param menuDTO
     * @return
     */
    @PutMapping
    public ResponseResult<Void> update(@RequestBody MenuDTO menuDTO) {
        return menuService.updateMenu(menuDTO);
    }

    /**
     * 根据id删除菜单
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseResult<String> delete(@PathVariable("id") @NotNull Long id) {
        return menuService.deleteMenu(id);
    }


    /**
     * 搜索管理菜单列表
     * @param typeId
     * @param username
     * @param status
     * @return
     */
    @GetMapping("/search/list/{typeId}")
    public ResponseResult<List<MenuVO>> searchMenu(@PathVariable("typeId") Integer typeId, String username, Integer status) {
        return menuService.getMenuList(typeId,username,status);
    }


    /**
     * 获取路由菜单列表
     * @param typeId
     * @return
     */
    @GetMapping("/router/list/{typeId}")
    public ResponseResult<List<MenuVO>> routerList(@PathVariable("typeId") Integer typeId) {
        return menuService.getMenuList(typeId,null,null);
    }


    @Autowired
    private RoleService roleService;


    /**
     * 获取修改菜单角色列表
     * @return
     */
    @GetMapping("/role/list")
    public ResponseResult<List<RoleVO>> selectAll() {
        return roleService.selectAll();
    }


    /**
     * 获取管理菜单列表
     * @param typeId
     * @return
     */
    @GetMapping("/list/{typeId}")
    public ResponseResult<List<MenuVO>> list(@PathVariable("typeId") Integer typeId) {
        return menuService.getMenuList(typeId,null,null);
    }



}
