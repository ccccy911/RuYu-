package xyz.kuailemao.controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.annotation.AccessLimit;
import xyz.kuailemao.domain.dto.SearchTagDTO;
import xyz.kuailemao.domain.dto.TagDTO;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.TagVO;
import xyz.kuailemao.mapper.TagMapper;
import xyz.kuailemao.service.TagService;
import xyz.kuailemao.utils.ControllerUtils;

import java.util.List;

@RestController
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private TagService tagService;


    /**
     * 获取标签列表（客户端）
     * @return
     */
    @GetMapping("/list")
    public ResponseResult<List<TagVO>> list() {
        return ControllerUtils.messageHandler(() -> tagService.listAllTag());
    }


    /**
     *新增标签-文章列表
     * @param tagDTO
     * @return
     */
    @PutMapping()
    public ResponseResult<Void> addTag(@RequestBody @Valid TagDTO tagDTO) {
        return tagService.addTag(tagDTO);
    }

    /**
     * 修改标签
     * @param tagDTO
     * @return
     */
    @PostMapping("/back/update")
    public ResponseResult<Void> updateTag(@RequestBody @Valid TagDTO tagDTO) {
        return tagService.addOrUpdateTag(tagDTO);
    }

    /**
     * 删除标签
     * @param ids
     * @return
     */
    @DeleteMapping("/back/delete")
    public ResponseResult<Void> deleteTag(@RequestBody List<Long> ids) {
        return tagService.deleteTagByIds(ids);
    }

    /**
     * 搜索标签列表
     * @param searchTagDTO
     * @return
     */
    @PostMapping("/back/search")
    public ResponseResult<List<TagVO>> searchTag(@RequestBody SearchTagDTO searchTagDTO) {
        return ControllerUtils.messageHandler(() -> tagService.searchTag(searchTagDTO));
    }


    /**
     * 后台获取标签列表
     * @return
     */
    @GetMapping("/back/list")
    public ResponseResult<List<TagVO>> listArticleTag() {
        return ControllerUtils.messageHandler(() -> tagService.listAllTag());
    }

    /**
     * 根据id查询标签
     * @param id
     * @return
     */
    @GetMapping("/back/get/{id}")
    public ResponseResult<TagVO> getTagById(@PathVariable(value = "id") Long id) {
        return ControllerUtils.messageHandler(() -> tagService.getTagById(id));
    }




}
