package xyz.kuailemao.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.domain.entity.Like;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.service.LikeService;

import java.util.List;

@RestController
@RequestMapping("/like")
public class LikeController {

    @Autowired
    private LikeService likeService;


    /**
     * 点赞
     * @param type
     * @param typeId
     * @return
     */
    public ResponseResult<Void> like(@RequestParam("type")  @NotNull Integer type,
                                     @RequestParam("typeId")  @NotNull Integer typeId) {
        return likeService.userLike(type, typeId);
    }


    /**
     * 取消点赞
     * @param type
     * @param typeId
     * @return
     */
    @DeleteMapping("/auth/like")
    public ResponseResult<Void> cancelLike(
            @RequestParam("type")  @NotNull Integer type,
            @RequestParam("typeId")  @NotNull Integer typeId
    ) {
        return likeService.cancelLike(type, typeId);
    }


    /**
     * 是否已经点赞
     * @param type
     * @param typeId
     * @return
     */
    @GetMapping("whether/like")
    public ResponseResult<List<Like>> isLike(
             @NotNull @RequestParam("type") Integer type,
            @RequestParam(value = "typeId", required = false) Integer typeId
    ) {
        return likeService.isLike(type, typeId);
    }


}
