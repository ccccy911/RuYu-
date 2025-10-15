package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kuailemao.domain.dto.SearchTreeHoleDTO;
import xyz.kuailemao.domain.dto.TreeHoleIsCheckDTO;
import xyz.kuailemao.domain.entity.TreeHole;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.TreeHoleListVO;
import xyz.kuailemao.domain.vo.TreeHoleVO;

import java.util.List;

public interface TreeHoleService extends IService<TreeHole> {

    ResponseResult<List<TreeHoleListVO>> getBackTreeHoleList(SearchTreeHoleDTO searchDTO);

    ResponseResult<Void> deleteTreeHole(List<Long> ids);

    ResponseResult<List<TreeHoleVO>> getTreeHole();

    ResponseResult<Void> addTreeHole(String content);

    ResponseResult<Void> isCheckTreeHole(TreeHoleIsCheckDTO treeHoleIsCheckDTO);
}
