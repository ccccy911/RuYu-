package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;
import xyz.kuailemao.domain.dto.SearchTagDTO;
import xyz.kuailemao.domain.dto.TagDTO;
import xyz.kuailemao.domain.entity.Tag;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.TagVO;

import java.util.List;

public interface TagService extends IService<Tag> {

    List<TagVO> listAllTag();

    ResponseResult<Void> addTag(@Valid TagDTO tagDTO);

    ResponseResult<Void> addOrUpdateTag(@Valid TagDTO tagDTO);

    ResponseResult<Void> deleteTagByIds(List<Long> ids);

    List<TagVO> searchTag(SearchTagDTO searchTagDTO);

    TagVO getTagById(Long id);
}
