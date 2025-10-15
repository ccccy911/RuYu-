package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.constraints.NotNull;
import xyz.kuailemao.domain.entity.Like;
import xyz.kuailemao.domain.response.ResponseResult;

import java.util.List;

public interface LikeService extends IService<Like> {


    ResponseResult<Void> userLike(@NotNull Integer type, @NotNull Integer typeId);

    ResponseResult<Void> cancelLike(@NotNull Integer type, @NotNull Integer typeId);

    ResponseResult<List<Like>> isLike(@NotNull Integer type, Integer typeId);
}
