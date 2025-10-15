package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.constraints.NotNull;
import xyz.kuailemao.domain.dto.MenuDTO;
import xyz.kuailemao.domain.entity.Menu;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.MenuByIdVO;
import xyz.kuailemao.domain.vo.MenuVO;

import java.util.List;

public interface MenuService extends IService<Menu> {

    ResponseResult<Void> addMenu(MenuDTO menuDTO);

    ResponseResult<MenuByIdVO> selectUpdateMenu(@NotNull Long id);

    ResponseResult<Void> updateMenu(MenuDTO menuDTO);

    ResponseResult<String> deleteMenu(@NotNull Long id);

    ResponseResult<List<MenuVO>> getMenuList(Integer typeId, String username, Integer status);
}
