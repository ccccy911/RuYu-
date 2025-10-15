package xyz.kuailemao.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.kuailemao.constants.RedisConst;
import xyz.kuailemao.domain.dto.FavoriteIsCheckDTO;
import xyz.kuailemao.domain.dto.SearchFavoriteDTO;
import xyz.kuailemao.domain.entity.Favorite;
import xyz.kuailemao.domain.entity.User;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.FavoriteListVO;
import xyz.kuailemao.mapper.*;
import xyz.kuailemao.service.FavoriteService;
import xyz.kuailemao.utils.RedisCache;
import xyz.kuailemao.utils.SecurityUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class FavoriteServiceimpl  extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {


    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private LeaveWordMapper leaveWordMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;


    @Autowired
    private RedisCache redisCache;

    /**
     * 搜索后台收藏列表
     * @param searchDTO
     * @return
     */
    @Override
    public ResponseResult<List<FavoriteListVO>> getBackFavoriteList(SearchFavoriteDTO searchDTO) {
        //传递：名称，类型，是否通过，开始时间，结束时间
        //返回：收藏id,名称，类型，内容，是否通过，开始时间
        LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
        //查询名称对应的全部id
        List<Long> idList = userMapper.selectList(new LambdaQueryWrapper<User>().like(User::getUsername, searchDTO.getUserName())).stream().map(user -> user.getId()).toList();
        //匹配条件
        queryWrapper.in(Favorite::getUserId, idList).eq(Favorite::getIsCheck,searchDTO.getIsCheck())
                .eq(Favorite::getType,searchDTO.getType());
        if(searchDTO.getStartTime() != null && searchDTO.getEndTime() != null) {
            queryWrapper.between(Favorite::getCreateTime, searchDTO.getStartTime(), searchDTO.getEndTime());
        }
        //查询结果
        List<Favorite> favorites = this.baseMapper.selectList(queryWrapper);
        List<FavoriteListVO> favoriteListVOList = favorites.stream().map(favorite -> {
            FavoriteListVO favoriteListVO = BeanUtil.copyProperties(favorite, FavoriteListVO.class);
            String  userName = userMapper.selectById(favorite.getUserId()).getUsername();
            favoriteListVO.setUserName(userName);
            switch (favorite.getType()) {
                case 1 -> favoriteListVO.setContent(articleMapper.selectById(favorite.getTypeId()).getArticleContent());
                case 2 -> favoriteListVO.setContent(leaveWordMapper.selectById(favorite.getTypeId()).getContent());
            }
            return favoriteListVO;
        }).toList();
        return ResponseResult.success(favoriteListVOList);
    }


    /**
     * 删除收藏
     * @param ids
     * @return
     */
    @Override
    public ResponseResult<Void> deleteFavorite(List<Long> ids) {
        if (favoriteMapper.deleteBatchIds(ids) > 0) return ResponseResult.success();
        return ResponseResult.failure();
    }


    /**
     * 取消收藏
     * @param type
     * @param typeId
     * @return
     */
    @Override
    public ResponseResult<Void> cancelFavorite(Integer type, Integer typeId) {
        // 查询是否已经收藏
        Favorite isFavorite = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, SecurityUtils.getUserId())
                .eq(Favorite::getType, type)
                .eq(Favorite::getTypeId, typeId));
        if (Objects.isNull(isFavorite)) return ResponseResult.failure();
        boolean cancelFavorite = this.remove(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, SecurityUtils.getUserId())
                .eq(Favorite::getType, type)
                .eq(Favorite::getTypeId, typeId));
        redisCache.incrementCacheMapValue(RedisConst.ARTICLE_FAVORITE_COUNT, typeId.toString(), -1);
        if (cancelFavorite) return ResponseResult.success();
        return ResponseResult.failure();
    }

    /**
     * 修改收藏是否通过
     * @param favoriteIsCheckDTO
     * @return
     */
    @Override
    public ResponseResult<Void> isCheckFavorite(FavoriteIsCheckDTO favoriteIsCheckDTO) {
        Favorite favorite = BeanUtil.copyProperties(favoriteIsCheckDTO, Favorite.class);
        if(this.updateById(favorite)) return ResponseResult.success();
        return ResponseResult.failure();
    }


    /**
     * 收藏
     * @param type
     * @param typeId
     * @return
     */
    @Override
    public ResponseResult<Void> userFavorite(Integer type, Long typeId) {
        // 查询是否已经收藏
        Favorite isFavorite = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, SecurityUtils.getUserId())
                .eq(Favorite::getType, type)
                .eq(Favorite::getTypeId, typeId));
        if(Objects.nonNull(isFavorite)) return ResponseResult.failure();
        Long userId = SecurityUtils.getUserId();
        Favorite favorite = Favorite.builder().type(type)
                .id(null)
                .userId(userId)
                .createTime(new Date())
                .isCheck(1)
                .typeId(typeId)
                .build();
        if(this.updateById(favorite)){
            if(type == 1){
            redisCache.incrementCacheMapValue(RedisConst.ARTICLE_FAVORITE_COUNT, typeId.toString(), 1);
            }
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }
}
