package xyz.kuailemao.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.domain.dto.FavoriteIsCheckDTO;
import xyz.kuailemao.domain.dto.SearchFavoriteDTO;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.FavoriteListVO;
import xyz.kuailemao.mapper.FavoriteMapper;
import xyz.kuailemao.service.FavoriteService;
import xyz.kuailemao.utils.ControllerUtils;

import java.util.List;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 搜索后台收藏列表
     * @param searchDTO
     * @return
     */
    @PostMapping("/back/search")
    public ResponseResult<List<FavoriteListVO>> backList(@RequestBody SearchFavoriteDTO searchDTO) {
        return favoriteService.getBackFavoriteList(searchDTO);
    }


    /**
     * 删除收藏
     * @param ids
     * @return
     */
    @DeleteMapping("/back/delete")
    public ResponseResult<Void> delete(@RequestBody List<Long> ids) {
        return favoriteService.deleteFavorite(ids);
    }

    /**
     * 取消收藏
     * @param type
     * @param typeId
     * @return
     */
    @DeleteMapping("/auth/favorite")
    public ResponseResult<Void> cancelFavorite(
            @RequestParam("type") Integer type,
            @RequestParam(value = "typeId", required = false) Integer typeId
    ) {
        return favoriteService.cancelFavorite(type, typeId);
    }


    /**
     * 修改收藏是否通过
     * @param favoriteIsCheckDTO
     * @return
     */
    @PostMapping("/back/isCheck")
    public ResponseResult<Void> isCheck(@RequestBody @Valid FavoriteIsCheckDTO favoriteIsCheckDTO) {
        return favoriteService.isCheckFavorite(favoriteIsCheckDTO);
    }

    /**
     * 收藏
     * @param type
     * @param typeId
     * @return
     */
    @PostMapping("/auth/favorite")
    public ResponseResult<Void> favorite(
            @Valid @NotNull @RequestParam("type") Integer type,
            @RequestParam(value = "typeId", required = false) Long typeId
    ) {
        return favoriteService.userFavorite(type, typeId);
    }

    /**
     * 后台收藏列表
     * @return
     */
    @GetMapping("/back/list")
    public ResponseResult<List<FavoriteListVO>> backList() {
        return favoriteService.getBackFavoriteList(null);
    }



}
