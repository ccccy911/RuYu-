package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import xyz.kuailemao.domain.dto.SearchCommentDTO;
import xyz.kuailemao.domain.dto.UserCommentDTO;
import xyz.kuailemao.domain.entity.Comment;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.ArticleCommentVO;
import xyz.kuailemao.domain.vo.CommentListVO;
import xyz.kuailemao.domain.vo.PageVO;
import xyz.kuailemao.mapper.CommentMapper;

import java.util.List;

public interface CommentService  extends IService<Comment> {

    List<CommentListVO> getBackCommentList(SearchCommentDTO searchDTO);

    ResponseResult<String> userComment(@Valid UserCommentDTO commentDTO);

    PageVO<List<ArticleCommentVO>> getComment(@Valid @NotNull Integer type, @Valid @NotNull Integer typeId, @Valid @NotNull Integer pageNum, @Valid @NotNull Integer pageSize);
}
