package xyz.kuailemao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.kuailemao.domain.dto.RoleDTO;
import xyz.kuailemao.domain.dto.RoleSearchDTO;
import xyz.kuailemao.domain.entity.Role;
import xyz.kuailemao.domain.entity.RoleMenu;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.RoleAllVO;
import xyz.kuailemao.domain.vo.RoleByIdVO;
import xyz.kuailemao.domain.vo.RoleVO;
import xyz.kuailemao.mapper.RoleMapper;
import xyz.kuailemao.mapper.RoleMenuMapper;
import xyz.kuailemao.service.RoleMenuService;
import xyz.kuailemao.service.RoleService;
import xyz.kuailemao.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RoleServiceimpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private RoleMenuService roleMenuService;
    /**
     * 添加角色信息
     * @param roleDTO
     * @return
     */
    @Override
    public ResponseResult<Void> addRole(RoleDTO roleDTO) {
        Role role = BeanUtil.copyProperties(roleDTO, Role.class);
        //角色的字符不能重复
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        Role isRole = this.baseMapper.selectOne(queryWrapper.eq(Role::getRoleKey,roleDTO.getRoleKey()));
        if(isRole!=null && !isRole.getId().equals(roleDTO.getId())){
            return ResponseResult.failure();
        }
        //如果保存成功，保存到权限菜单栏
        if(this.save(role)){
            //把角色id对应的原有菜单id先删掉
            roleMenuMapper.deleteById(roleDTO.getId());
            //再更新
            List<RoleMenu> roleMenuList = roleDTO.getMenuIds().stream().map(menuId -> new RoleMenu(menuId,roleDTO.getId())).collect(Collectors.toList());
            roleMenuService.saveBatch(roleMenuList);
        }
        return  ResponseResult.success();











    }

    /**
     * 修改角色信息
     * @param roleDTO
     * @return
     */
    @Override
    public ResponseResult<Void> updateRole(RoleDTO roleDTO) {
        Role role = BeanUtil.copyProperties(roleDTO, Role.class);
        //如果名字和字符没有同时重复
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Role::getRoleKey,roleDTO.getRoleKey());
        Role isRole = this.baseMapper.selectOne(queryWrapper);
        if(isRole!=null && !isRole.getId().equals(roleDTO.getId())){
            return ResponseResult.failure("该用户已存在");
        }
        this.updateById(role);
        return ResponseResult.success();
    }

    /**
     * 更新角色状态
     * @param roleDTO
     * @return
     */
    @Override
    public ResponseResult<Void> updateRoleStatus(RoleDTO roleDTO) {
        Role role = BeanUtil.copyProperties(roleDTO, Role.class);
        this.updateById(role);
        return ResponseResult.success();
    }

    /**
     * 根据条件搜索角色
     * @param roleSearchDTO
     * @return
     */
    @Override
    public ResponseResult<List<RoleAllVO>> searchRole(RoleSearchDTO roleSearchDTO) {
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Objects.nonNull(roleSearchDTO.getRoleKey()),Role::getRoleKey,roleSearchDTO.getRoleKey())
                .eq(Objects.nonNull(roleSearchDTO.getStatus()),Role::getStatus,roleSearchDTO.getStatus())
                .like(Objects.nonNull(roleSearchDTO.getRoleName()),Role::getRoleName,roleSearchDTO.getRoleName());
        if(roleSearchDTO.getCreateTimeStart() != null){
            queryWrapper.le(Role::getCreateTime,roleSearchDTO.getCreateTimeStart());
        }
        if(roleSearchDTO.getCreateTimeEnd() != null){
            queryWrapper.le(Role::getCreateTime,roleSearchDTO.getCreateTimeEnd());
        }
        //按ordernum字段升序排列
        queryWrapper.orderByAsc(Role::getOrderNum);

        List<Role> list = this.list(queryWrapper);
        List<RoleAllVO> roleAllVOList = list.stream().map(role -> BeanUtil.copyProperties(role,RoleAllVO.class)).collect(Collectors.toList());

        return ResponseResult.success(roleAllVOList);
    }

    /**
     * 获取角色列表
     * @param
     * @return
     */
    @Override
    public ResponseResult<List<RoleAllVO>> getRoleList() {
        List<Role>  roleList = this.list();
        List<RoleAllVO> roleAllVOList = roleList.stream().map(role -> BeanUtil.copyProperties(role,RoleAllVO.class)).collect(Collectors.toList());
        return ResponseResult.success(roleAllVOList);
    }

    /**
     * 根据id获取角色信息
     * @param id
     * @return
     */
    @Override
    public ResponseResult<RoleByIdVO> getRole(Integer id) {
       Role role = this.getById(id);
        RoleByIdVO roleByIdVO = BeanUtil.copyProperties(role,RoleByIdVO.class);
        return ResponseResult.success(roleByIdVO);
    }


    /**
     * 获取修改菜单角色列表
     * @return
     */
    @Override
    public ResponseResult<List<RoleVO>> selectAll() {
        List<Role>  roleList = this.list();
        List<RoleVO> roleVOList = roleList.stream().map(role -> BeanUtil.copyProperties(role,RoleVO.class)).collect(Collectors.toList());
        if(roleVOList.size()>0){
            return ResponseResult.success(roleVOList);
        }
        return ResponseResult.failure();
    }
}
