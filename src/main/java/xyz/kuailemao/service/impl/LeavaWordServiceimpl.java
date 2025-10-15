package xyz.kuailemao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.kuailemao.constants.FunctionConst;
import xyz.kuailemao.constants.SQLConst;
import xyz.kuailemao.domain.dto.LeaveWordIsCheckDTO;
import xyz.kuailemao.domain.dto.SearchLeaveWordDTO;
import xyz.kuailemao.domain.entity.*;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.LeaveWordListVO;
import xyz.kuailemao.domain.vo.LeaveWordVO;
import xyz.kuailemao.enums.CommentEnum;
import xyz.kuailemao.enums.FavoriteEnum;
import xyz.kuailemao.enums.LikeEnum;
import xyz.kuailemao.enums.MailboxAlertsEnum;
import xyz.kuailemao.mapper.*;
import xyz.kuailemao.service.LeaveWordService;
import xyz.kuailemao.service.PublicService;
import xyz.kuailemao.utils.SecurityUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeavaWordServiceimpl  extends ServiceImpl<LeaveWordMapper, LeaveWord> implements LeaveWordService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private LikeMapper likeMapper;

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private LeaveWordMapper leaveWordMapper;




    @Resource
    private PublicService publicService;

    @Value("${spring.mail.username}")
    private String email;

    @Value("${mail.message-new-notice}")
    private Boolean messageNewNotice;

    /**
     * 用户留言
     * @param content
     * @return
     */
    @Override
    public ResponseResult<Void> userLeaveWord(String content) {
        //留言不能过长
        if (content.length() > FunctionConst.LEAVE_WORD_CONTENT_LENGTH) {
            return  ResponseResult.failure("留言过长");
        }
        LeaveWord leaveWord = LeaveWord.builder().content(content)
                .userId(SecurityUtils.getUserId())
                .isCheck(1)
                .createTime(new Date())
                .updateTime(new Date())
                .isDeleted(0)
                .build();
        if(this.save(leaveWord)){
            //如果是站长本人或者邮件提醒没开，就不需要邮件提醒
            User user = userMapper.selectById(leaveWord.getUserId());
            if(user.getEmail().equals(email)||messageNewNotice==false){
                return  ResponseResult.success();
            }
            // 留言成功，发送邮箱提醒给站长
            Map<String, Object> map = new HashMap<>();
            map.put("messageId",leaveWord.getId());
            publicService.sendEmail(MailboxAlertsEnum.MESSAGE_NOTIFICATION_EMAIL.getCodeStr(), email, map);

            return ResponseResult.success();
        }
        return  ResponseResult.failure();
    }

    /**
     * 搜索后台留言列表
     * @param searchDTO
     * @return
     */
    @Override
    public ResponseResult<List<LeaveWordListVO>> getBackLeaveWordList(SearchLeaveWordDTO searchDTO) {
        LambdaQueryWrapper<LeaveWord> queryWrapper = new  LambdaQueryWrapper<>();
        List<Long> userIdList = userMapper.selectList(new LambdaQueryWrapper<User>().like(User::getUsername,searchDTO.getUserName())).stream().map(User::getId).collect(Collectors.toList());
        if(userIdList.size()>0){
            queryWrapper.in(LeaveWord::getUserId, userIdList);
        }
        queryWrapper.eq(Objects.nonNull(searchDTO.getIsCheck()),LeaveWord::getIsCheck, searchDTO.getIsCheck());
        if(searchDTO.getIsCheck()!=null && searchDTO.getEndTime()!=null){
            queryWrapper.between(LeaveWord::getCreateTime, searchDTO.getStartTime(), searchDTO.getEndTime());
        }
        List<LeaveWord> leaveWordList = leaveWordMapper.selectList(queryWrapper);
        //避免每次都循环查询用户名称
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, leaveWordList.stream().map(LeaveWord::getUserId).collect(Collectors.toList())));
        Map<Long,String> map = users.stream().collect(Collectors.toMap(User::getId, User::getUsername));

        List<LeaveWordListVO> resultList = leaveWordList.stream().map(leaveWord -> {
            LeaveWordListVO leaveWordListVO = BeanUtil.copyProperties(leaveWord, LeaveWordListVO.class);
            leaveWordListVO.setUserName(map.get(leaveWord.getUserId()));
            return leaveWordListVO;
        }).collect(Collectors.toList());
        return ResponseResult.success(resultList);
    }

    /**
     * 前端留言列表
     * @param id
     * @return
     */
    @Override
    public List<LeaveWordVO> getLeaveWordList(String id) {
        //传入id是为了看某条评论的详情，如果传null就是查看全部评论区
        return this.query()
                .eq(SQLConst.IS_CHECK, SQLConst.IS_CHECK_YES)
                .eq(id != null, SQLConst.ID, id)
                .orderByDesc(SQLConst.CREATE_TIME)
                .list().stream().map(leaveWord -> {
                    User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, leaveWord.getUserId()));
                    return leaveWord.asViewObject(LeaveWordVO.class, leaveWordVO -> leaveWordVO.setNickname(user.getNickname())
                            .setAvatar(user.getAvatar())
                            //每条留言对应的评论总数
                            .setCommentCount(commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getType, CommentEnum.COMMENT_TYPE_LEAVE_WORD.getType()).eq(Comment::getIsCheck, SQLConst.IS_CHECK_YES).eq(Comment::getTypeId, leaveWord.getId())))
                            //每条评论对应的点赞数
                            .setLikeCount(likeMapper.selectCount(new LambdaQueryWrapper<Like>().eq(Like::getType, LikeEnum.LIKE_TYPE_LEAVE_WORD.getType()).eq(Like::getTypeId, leaveWord.getId())))
                            //每条评论对应的收藏数
                            .setFavoriteCount(favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>().eq(Favorite::getType, CommentEnum.COMMENT_TYPE_LEAVE_WORD.getType()).eq(Favorite::getTypeId, leaveWord.getId()))));
                }).toList();
    }


    /**
     * 删除留言
     * @param ids
     * @return
     */
    @Override
    public ResponseResult<Void> deleteLeaveWord(List<Long> ids) {
        //点赞，收藏，留言，评论都要删除
        //删除收藏
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().in(Favorite::getTypeId, ids).eq(Favorite::getType, FavoriteEnum.FAVORITE_TYPE_LEAVE_WORD.getType()));
        //删除点赞
        likeMapper.delete(new LambdaQueryWrapper<Like>().in(Like::getTypeId,ids).eq(Like::getType,LikeEnum.LIKE_TYPE_LEAVE_WORD.getType()));
        //删除留言
        leaveWordMapper.deleteBatchIds(ids);
        //删除评论
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getType, CommentEnum.COMMENT_TYPE_LEAVE_WORD.getType()).and(a -> a.in(Comment::getTypeId, ids)));
        return ResponseResult.success();
    }


    /**
     * 修改留言是否通过
     * @param leaveWordIsCheckDTO
     * @return
     */
    @Override
    public ResponseResult<Void> isCheckLeaveWord(LeaveWordIsCheckDTO leaveWordIsCheckDTO) {
        if(this.updateById(BeanUtil.copyProperties(leaveWordIsCheckDTO, LeaveWord.class))){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

}
