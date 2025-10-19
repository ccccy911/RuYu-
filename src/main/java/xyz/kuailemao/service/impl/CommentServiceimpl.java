package xyz.kuailemao.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.kuailemao.constants.RedisConst;
import xyz.kuailemao.constants.SQLConst;
import xyz.kuailemao.domain.dto.SearchCommentDTO;
import xyz.kuailemao.domain.dto.UserCommentDTO;
import xyz.kuailemao.domain.entity.Comment;
import xyz.kuailemao.domain.entity.LeaveWord;
import xyz.kuailemao.domain.entity.User;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.ArticleCommentVO;
import xyz.kuailemao.domain.vo.CommentListVO;
import xyz.kuailemao.domain.vo.PageVO;
import xyz.kuailemao.enums.MailboxAlertsEnum;
import xyz.kuailemao.mapper.CommentMapper;
import xyz.kuailemao.mapper.LeaveWordMapper;
import xyz.kuailemao.mapper.LikeMapper;
import xyz.kuailemao.mapper.UserMapper;
import xyz.kuailemao.service.CommentService;
import xyz.kuailemao.service.LikeService;
import xyz.kuailemao.service.PublicService;
import xyz.kuailemao.utils.ControllerUtils;
import xyz.kuailemao.utils.RedisCache;
import xyz.kuailemao.utils.SecurityUtils;
import xyz.kuailemao.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceimpl  extends ServiceImpl<CommentMapper, Comment> implements CommentService {


    @Resource
    private CommentMapper commentMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private LikeService likeService;

    @Resource
    private RedisCache redisCache;

    @Resource
    private LikeMapper likeMapper;

    @Resource
    private PublicService publicService;

    @Resource
    private LeaveWordMapper leaveWordMapper;

    @Value("${spring.mail.username}")
    private String fromUser;

    @Value("${mail.article-email-notice}")
    private Boolean articleEmailNotice;

    @Value("${mail.article-reply-notice}")
    private Boolean articleReplyNotice;

    @Value("${mail.message-email-notice}")
    private Boolean messageEmailNotice;

    @Value("${mail.message-reply-notice}")
    private Boolean messageReplyNotice;

    /**
     * 搜索后台评论列表
     * @param searchDTO
     * @return
     */
    @Override
    public List<CommentListVO> getBackCommentList(SearchCommentDTO searchDTO) {
        //搜索条件：名称，类型，内容，是否通过
        //返回：评论id，类型id，父评论id，回复评论id，内容，名称，类型，是否通过，评论时间，更新时间
        //根据名称获取对应的所有用户id
        List<User> userList =  userMapper.selectList(new LambdaQueryWrapper<User>().like(User::getUsername, searchDTO.getCommentUserName()));
        List<Long> userIdList = userList.stream().map(User::getId).collect(Collectors.toList());
        //匹配条件
        LambdaQueryWrapper<Comment> commentQueryWrapper = new LambdaQueryWrapper<>();
        if(userIdList.size()>0){
            commentQueryWrapper.in(Comment::getCommentUserId, userIdList);
        }
        commentQueryWrapper.eq(Comment::getIsCheck,searchDTO.getIsCheck())
                .eq(Comment::getType, searchDTO.getType())
                .like(Comment::getCommentContent, searchDTO.getCommentContent());

        //查询对应的评论
        List<Comment> commentList = this.baseMapper.selectList(commentQueryWrapper);
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId,commentList.stream().map(Comment::getId).toList()));
        Map<Long,String> map = users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
        List<CommentListVO> commentListVOS = commentList.stream().map(comment->{
          CommentListVO  commentListVO = BeanUtil.copyProperties(comment, CommentListVO.class);
          commentListVO.setCommentContent(map.get(comment.getCommentUserId()));
          return commentListVO;}).collect(Collectors.toList());
        return commentListVOS;
    }


    /**
     * 用户添加评论
     * @param commentDTO
     * @return
     */
    @Override
    public ResponseResult<String> userComment(UserCommentDTO commentDTO) {
        Comment comment = BeanUtil.copyProperties(commentDTO,Comment.class);
        comment.builder().commentUserId(SecurityUtils.getUserId()).createTime(new Date()).updateTime(new Date()).build();
        if (this.save(comment)) {
            // 判断用是否为第三方登录没有邮箱
            User user = userMapper.selectById(SecurityUtils.getUserId());
            if (StringUtils.isEmpty(user.getEmail())) {
                // 提示绑定邮箱
                return ResponseResult.success("检测到您尚未绑定邮箱,无法开启邮箱提醒，请先绑定邮箱");
            }
            return this.commentEmailReminder(commentDTO, user, comment);
        }
        return ResponseResult.failure();
    }

    /**
     * 用户获取评论
     * @param type
     * @param typeId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageVO<List<ArticleCommentVO>> getComment(Integer type, Integer typeId, Integer pageNum, Integer pageSize) {
        //先获取全部的父评论
        LambdaQueryWrapper<Comment> commentQueryWrapper = new LambdaQueryWrapper<>();
        //根据创建时间从新到旧排序
        commentQueryWrapper.orderByDesc(Comment::getCreateTime);
        commentQueryWrapper.eq(Comment::getType, type)
                .eq(Comment::getIsCheck, SQLConst.COMMENT_IS_CHECK)
                .eq(Comment::getTypeId, typeId)
                .isNull(Comment::getParentId);
        Page<Comment> page = new Page<>(pageNum, pageSize);
        IPage<Comment> commentIPage = commentMapper.selectPage(page, commentQueryWrapper);
        //每一页的数据对象，因为每次点击分页都会请求后端，同时传过来要看第几页的数据
        List<Comment> comments = commentIPage.getRecords();
        //查询父评论对应的全部子评论
        LambdaQueryWrapper<Comment> childQueryWrapper = new LambdaQueryWrapper<>();
        childQueryWrapper
                .orderByDesc(Comment::getCreateTime)
                .eq(Comment::getType, type)
                .eq(Comment::getTypeId, typeId)
                .eq(Comment::getIsCheck, SQLConst.COMMENT_IS_CHECK)
                .in(Comment::getParentId,comments.stream().map(Comment::getId).collect(Collectors.toList()));
        List<Comment> childComment = commentMapper.selectList(childQueryWrapper);
        //把子评论组装到父评论中
        List<ArticleCommentVO> parentList = comments.stream().map(comment -> BeanUtil.copyProperties(comment, ArticleCommentVO.class)).toList();
        List<ArticleCommentVO> childList = childComment.stream().map(comment -> BeanUtil.copyProperties(comment, ArticleCommentVO.class)).collect(Collectors.toList());
        //避免n+1
        Map<Long, List<ArticleCommentVO>> parentChildMap = childList.stream()
                .collect(Collectors.groupingBy(
                        ArticleCommentVO::getParentId,  // 分组依据：子评论的父ID（即所属的父评论ID）
                        Collectors.toList()             // 收集方式：将同一父ID的子评论放入List（默认行为，可省略）
                ));
        //先夺舍父评论，然后组装对应子评论
        List<ArticleCommentVO> articleCommentVOList =parentList.stream().peek(comment->{comment.setChildComment(parentChildMap.get(comment.getId()));})
                .toList();
        //获取全部的

        return null;
    }


    /**
     * 评论邮箱提醒
     *
     * @param commentDTO 前端DTO
     * @param user       用户
     * @param comment    新增评论消息
     * @return ResponseResult
     */
    public ResponseResult<String> commentEmailReminder(UserCommentDTO commentDTO, User user, Comment comment) {
        // 缓存评论数量+1
        redisCache.incrementCacheMapValue(RedisConst.ARTICLE_COMMENT_COUNT, commentDTO.getTypeId().toString(), 1);
        // 评论
        if (StringUtils.isNull(commentDTO.getReplyId())) {

            if ((commentDTO.getType() == 1 && !articleEmailNotice) || commentDTO.getType() == 2 && !messageEmailNotice)
                return ResponseResult.success();

            Map<String, Object> selectWhereMap = new HashMap<>();
            selectWhereMap.put("commentType", commentDTO.getType());
            selectWhereMap.put("commentId", comment.getId());

            // 留言提示对应发布留言的用户
            if (commentDTO.getType() == 1) {
                if (Objects.equals(fromUser, user.getEmail())) return ResponseResult.success();
                // 发邮箱给站长
                publicService.sendEmail(MailboxAlertsEnum.COMMENT_NOTIFICATION_EMAIL.getCodeStr(), fromUser, selectWhereMap);
            }

            if (commentDTO.getType() == 2) {
                // 查出回复的该留言用户的邮箱
                LeaveWord leaveWord = leaveWordMapper.selectOne(new LambdaQueryWrapper<LeaveWord>().eq(LeaveWord::getId, commentDTO.getTypeId()));
                User replyUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, leaveWord.getUserId()));
                // 用户没绑定邮箱，或者回复的留言是自己
                if (Objects.equals(replyUser.getEmail(), null) || Objects.equals(replyUser.getEmail(), user.getEmail()))
                    return ResponseResult.success();
                // 发送邮箱给该留言的用户
                publicService.sendEmail(MailboxAlertsEnum.COMMENT_NOTIFICATION_EMAIL.getCodeStr(), replyUser.getEmail(), selectWhereMap);
            }
        }
        // 回复评论
        if (Objects.nonNull(commentDTO.getReplyId())) {
            User replyUser = userMapper.selectById(commentDTO.getReplyUserId());
            if ((commentDTO.getType() == 1 && !articleReplyNotice) || (commentDTO.getType() == 2 && !messageReplyNotice))
                return ResponseResult.success();

            // 如果用户回复自己并且回复人是站长就无需提醒
            if (Objects.equals(replyUser.getEmail(), user.getEmail()) && Objects.equals(fromUser, user.getEmail())) {
                return ResponseResult.success();
            }

            Map<String, Object> selectWhereMap = new HashMap<>();
            selectWhereMap.put("commentType", commentDTO.getType());
            selectWhereMap.put("commentId", comment.getId());
            selectWhereMap.put("replyCommentId", commentDTO.getReplyId());

            // 回复人与被回复人不是站长本人的话就发送新增评论邮箱给站长
            if (!Objects.equals(user.getEmail(), fromUser) && !Objects.equals(replyUser.getEmail(), fromUser)) {
                publicService.sendEmail(MailboxAlertsEnum.COMMENT_NOTIFICATION_EMAIL.getCodeStr(), fromUser, selectWhereMap);
            }

            // 回复人不是站长本人并且不是自己回复自己，就发送回复通知
            if (!Objects.equals(user.getEmail(), replyUser.getEmail())) {
                publicService.sendEmail(MailboxAlertsEnum.REPLY_COMMENT_NOTIFICATION_EMAIL.getCodeStr(), replyUser.getEmail(), selectWhereMap);
            }
        }
        return ResponseResult.success();
    }



}
