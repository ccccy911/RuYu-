package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import xyz.kuailemao.domain.dto.FavoriteIsCheckDTO;
import xyz.kuailemao.domain.dto.SearchFavoriteDTO;
import xyz.kuailemao.domain.entity.Favorite;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.FavoriteListVO;

import java.util.List;

public interface FavoriteService extends IService<Favorite> {

    ResponseResult<List<FavoriteListVO>> getBackFavoriteList(SearchFavoriteDTO searchDTO);

    ResponseResult<Void> deleteFavorite(List<Long> ids);

    ResponseResult<Void> cancelFavorite(Integer type, Integer typeId);

    ResponseResult<Void> isCheckFavorite(@Valid FavoriteIsCheckDTO favoriteIsCheckDTO);

    ResponseResult<Void> userFavorite(@Valid @NotNull Integer type, Long typeId);
}
