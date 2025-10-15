package xyz.kuailemao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.jna.platform.win32.Wincon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.kuailemao.domain.dto.SearchTreeHoleDTO;
import xyz.kuailemao.domain.dto.TreeHoleIsCheckDTO;
import xyz.kuailemao.domain.entity.TreeHole;
import xyz.kuailemao.domain.entity.User;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.TreeHoleListVO;
import xyz.kuailemao.domain.vo.TreeHoleVO;
import xyz.kuailemao.mapper.TreeHoleMapper;
import xyz.kuailemao.mapper.UserMapper;
import xyz.kuailemao.service.TreeHoleService;
import xyz.kuailemao.service.UserService;
import xyz.kuailemao.utils.SecurityUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TreeHoleServiceimpl  extends ServiceImpl<TreeHoleMapper, TreeHole> implements TreeHoleService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ResponseResult<List<TreeHoleListVO>> getBackTreeHoleList(SearchTreeHoleDTO searchDTO) {
        LambdaQueryWrapper<TreeHole> queryWrapper = new LambdaQueryWrapper<TreeHole>();
        List<Long> userIds = new ArrayList<>();
        if (searchDTO.getUserName() != null) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().like(User::getUsername, searchDTO.getUserName()));
            userIds = users.stream().map(User::getId).collect(Collectors.toList());
        }
        queryWrapper.in(TreeHole::getUserId, userIds)
                .eq(Objects.nonNull(searchDTO.getIsCheck()), TreeHole::getIsCheck, searchDTO.getIsCheck());
        if (searchDTO.getStartTime() != null && searchDTO.getEndTime() != null) {
            queryWrapper.ge(TreeHole::getCreateTime, searchDTO.getStartTime())
                    .le(TreeHole::getUpdateTime, searchDTO.getEndTime());
        }
        List<TreeHoleListVO> list = this.baseMapper.selectList(queryWrapper).stream().map(treeHole -> {
                    TreeHoleListVO treeHoleListVO = BeanUtil.copyProperties(treeHole, TreeHoleListVO.class);
                    treeHoleListVO.setUserName(userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, treeHole.getUserId())).getUsername());
                    return treeHoleListVO;
                }
        ).toList();
        return ResponseResult.success(list);
    }


    /**
     * 删除树洞
     *
     * @param ids
     * @return
     */
    @Override
    public ResponseResult<Void> deleteTreeHole(List<Long> ids) {
        if (this.removeByIds(ids)) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();

    }

    /**
     * 前端查看树洞
     *
     * @return
     */
    @Override
    public ResponseResult<List<TreeHoleVO>> getTreeHole() {
        //获取树洞里的所有用户
        List<TreeHole> users = this.baseMapper.selectList(null);
        if (users == null || users.size() == 0) {
            return ResponseResult.success(new ArrayList<>());
        }
        //获取用户Id列表
        List<Long> userIds = users.stream().map(TreeHole::getId).collect(Collectors.toList());
        //获取所有用户
        Map<Long, User> userMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)).stream().collect(Collectors.toMap(User::getId, user -> user));
        List<TreeHoleVO> treeHoleVOList = users.stream().map(item -> {
            TreeHoleVO treeHoleVO = BeanUtil.copyProperties(item, TreeHoleVO.class);
            treeHoleVO.setNickname(userMap.get(item.getId()).getNickname());
            treeHoleVO.setAvatar(userMap.get(item.getUserId()).getAvatar());
            return treeHoleVO;
        }).toList();
        return  ResponseResult.success(treeHoleVOList);

    }


    /**
     * 添加树洞
     * @param content
     * @return
     */
    @Override
    public ResponseResult<Void> addTreeHole(String content) {
        TreeHole treeHole = new TreeHole();
        //该用户的id
        treeHole.setId(SecurityUtils.getUserId());
        //内容
        treeHole.setContent(content);
        //创建时间和修改时间
        treeHole.setCreateTime(new Date());
        treeHole.setUpdateTime(new Date());
        //是否通过
        treeHole.setIsCheck(1);
        //是否已删除
        treeHole.setIsDeleted(0);
        if(this.save(treeHole)){
            return ResponseResult.success();
        }
        return ResponseResult.failure();

    }


    /**
     * 修改树洞是否通过
     * @param treeHoleIsCheckDTO
     * @return
     */
    @Override
    public ResponseResult<Void> isCheckTreeHole(TreeHoleIsCheckDTO treeHoleIsCheckDTO) {
        TreeHole treeHole = BeanUtil.copyProperties(treeHoleIsCheckDTO,TreeHole.class);
        if(this.updateById(treeHole)){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }
}
