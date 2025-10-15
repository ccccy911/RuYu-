package xyz.kuailemao.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.kuailemao.constants.RedisConst;
import xyz.kuailemao.domain.entity.Like;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.enums.LikeEnum;
import xyz.kuailemao.mapper.LikeMapper;
import xyz.kuailemao.service.LikeService;
import xyz.kuailemao.utils.RedisCache;
import xyz.kuailemao.utils.SecurityUtils;

import java.security.Security;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class LIkeServiceimpl  extends ServiceImpl<LikeMapper, Like> implements LikeService {

    @Autowired
    private RedisCache redisCache;


    @Autowired
    private LikeMapper likeMapper;

    /**
     * 点赞
     * @param type
     * @param typeId
     * @return
     */
    @Override
    public ResponseResult<Void> userLike(Integer type, Integer typeId) {
        //查询是否已经点赞
        Like isLike =this.baseMapper.selectOne(new LambdaQueryWrapper<Like>()
                .eq(Like::getType, type)
                .eq(Like::getUserId,SecurityUtils.getUserId())
                .eq(Like::getTypeId,typeId));
        if(isLike != null){
            return ResponseResult.failure();
        }
        Like like = Like.builder().
                userId(SecurityUtils.getUserId()).
                type(type).
                typeId(typeId).
                build();
        //如果是文章，redis里点赞数加1（key，field，value）
        if (Objects.equals(type, LikeEnum.LIKE_TYPE_ARTICLE.getType())){
            redisCache.incrementCacheMapValue(RedisConst.ARTICLE_LIKE_COUNT, typeId.toString(), 1);
        }


        if(this.save(like)){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


    /**
     * 取消点赞
     * @param type
     * @param typeId
     * @return
     */
    @Override
    public ResponseResult<Void> cancelLike(Integer type, Integer typeId) {
        //查询是否已经点赞
        Like isLike =this.baseMapper.selectOne(new LambdaQueryWrapper<Like>()
                .eq(Like::getType, type)
                .eq(Like::getUserId,SecurityUtils.getUserId())
                .eq(Like::getTypeId,typeId));
        if(isLike != null){
            return ResponseResult.failure();
        }
        //如果是文章，redis里点赞数减1
        if (Objects.equals(type, LikeEnum.LIKE_TYPE_ARTICLE.getType())){
            redisCache.incrementCacheMapValue(RedisConst.ARTICLE_LIKE_COUNT, typeId.toString(), 1);
        }
        //删除点赞记录
        if(this.remove(new LambdaQueryWrapper<Like>().eq(Like::getType, type).eq(Like::getUserId,SecurityUtils.getUserId()).eq(Like::getTypeId,typeId))){
            return ResponseResult.success();
        }
        return ResponseResult.failure();


    }


    /**
     * 是否已经点赞
     * @param type
     * @param typeId
     * @return
     */
    @Override
    public ResponseResult<List<Like>> isLike(Integer type, Integer typeId) {
        if (SecurityUtils.isLogin()) {
            LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
            if (Objects.equals(type, LikeEnum.LIKE_TYPE_ARTICLE.getType())) {
                List<Like> like = likeMapper.selectList(wrapper
                        .eq(Like::getUserId, SecurityUtils.getUserId())
                        .eq(Like::getType, type)
                        .eq(Like::getTypeId, typeId));
                if (!like.isEmpty()) return ResponseResult.success(like);
                else ResponseResult.failure(null);
            }
            if (Objects.equals(type, LikeEnum.LIKE_TYPE_COMMENT.getType()) || Objects.equals(type, LikeEnum.LIKE_TYPE_LEAVE_WORD.getType())) {
                if (Objects.equals(type, LikeEnum.LIKE_TYPE_LEAVE_WORD.getType())) wrapper.eq(Like::getTypeId, typeId);
                List<Like> like = likeMapper.selectList(wrapper
                        .eq(Like::getUserId, SecurityUtils.getUserId())
                        .eq(Like::getType, type));
                if (!like.isEmpty()) return ResponseResult.success(like);
                else ResponseResult.failure(null);
            }
        }
        return ResponseResult.failure(null);
    }


    @Override
    public Long getLikeCount(Integer likeTypeComment, Long id) {
        return likeMapper.selectCount(new LambdaQueryWrapper<Like>()
                .eq(Like::getType, likeTypeComment)
                .eq(Like::getTypeId, id));
    }
}
