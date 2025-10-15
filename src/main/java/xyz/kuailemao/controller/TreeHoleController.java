package xyz.kuailemao.controller;


import com.alibaba.fastjson.JSON;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.domain.dto.SearchTreeHoleDTO;
import xyz.kuailemao.domain.dto.TreeHoleIsCheckDTO;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.TreeHoleListVO;
import xyz.kuailemao.domain.vo.TreeHoleVO;
import xyz.kuailemao.service.TreeHoleService;
import xyz.kuailemao.utils.ControllerUtils;

import java.util.List;

@RestController
@RequestMapping("/treeHole")
public class TreeHoleController {

    @Autowired
    private TreeHoleService treeHoleService;


    /**
     * 后台树洞列表
     * @return
     */
    @GetMapping("/back/list")
    public ResponseResult<List<TreeHoleListVO>> backList() {
        return treeHoleService.getBackTreeHoleList(null);
    }


    /**
     * 搜索后台树洞列表
     * @param searchDTO
     * @return
     */
    @PostMapping("/back/search")
    public ResponseResult<List<TreeHoleListVO>> backList(@RequestBody SearchTreeHoleDTO searchDTO) {
        return  treeHoleService.getBackTreeHoleList(searchDTO);
    }

    /**
     * 删除树洞
     * @param ids
     * @return
     */
    @DeleteMapping("/back/delete")
    public ResponseResult<Void> delete(@RequestBody List<Long> ids) {
        return treeHoleService.deleteTreeHole(ids);
    }


    /**
     * 查看树洞
     * @return
     */
    @GetMapping("/getTreeHoleList")
    public ResponseResult<List<TreeHoleVO>> getTreeHoleList() {
        return  treeHoleService.getTreeHole();
    }


    /**
     * 添加树洞
     * @param content
     * @return
     */
    @PostMapping("/auth/addTreeHole")
    public ResponseResult<Void> addTreeHole( @NotNull @RequestBody String content) {
        return treeHoleService.addTreeHole(JSON.parseObject(content).getString("content"));
    }

    /**
     * 修改树洞是否通过
     * @param treeHoleIsCheckDTO
     * @return
     */
    @PostMapping("/back/isCheck")
    public ResponseResult<Void> isCheck(@RequestBody  TreeHoleIsCheckDTO treeHoleIsCheckDTO) {
        return treeHoleService.isCheckTreeHole(treeHoleIsCheckDTO);
    }

}
