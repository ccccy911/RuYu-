package xyz.kuailemao.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kuailemao.domain.dto.SearchCommentDTO;
import xyz.kuailemao.domain.dto.UserCommentDTO;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.ArticleCommentVO;
import xyz.kuailemao.domain.vo.CommentListVO;
import xyz.kuailemao.domain.vo.PageVO;
import xyz.kuailemao.mapper.CommentMapper;
import xyz.kuailemao.service.CommentService;
import xyz.kuailemao.utils.ControllerUtils;

import java.util.List;

@RequestMapping("/comment")
@RestController
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 搜索后台评论列表
     * @param searchDTO
     * @return
     */
    @PostMapping("/back/search")
    public ResponseResult<List<CommentListVO>> backList(@RequestBody SearchCommentDTO searchDTO) {
        return ControllerUtils.messageHandler(() -> commentService.getBackCommentList(searchDTO));
    }


    /**
     * 用户添加评论
     * @param commentDTO
     * @return
     */
    @PostMapping("/auth/add/comment")
    public ResponseResult<String> userComment(@Valid @RequestBody UserCommentDTO commentDTO) {
        return commentService.userComment(commentDTO);
    }




    /**
     * 获取评论
     * @param type
     * @param typeId
     * @param pageNum
     * @param pageSize
     * @return
     */
    public ResponseResult<PageVO<List<ArticleCommentVO>>> comment(
            @Valid @NotNull Integer type,
            @Valid @NotNull Integer typeId,
            @Valid @NotNull Integer pageNum,
            @Valid @NotNull Integer pageSize
    ) {
        return ControllerUtils.messageHandler((() -> commentService.getComment(type, typeId, pageNum, pageSize)));
    }


}
