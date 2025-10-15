package xyz.kuailemao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import xyz.kuailemao.domain.dto.RoleUserDTO;
import xyz.kuailemao.domain.dto.UserRoleDTO;
import xyz.kuailemao.domain.entity.Role;
import xyz.kuailemao.domain.entity.User;
import xyz.kuailemao.domain.entity.UserRole;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.RoleAllVO;
import xyz.kuailemao.domain.vo.RoleUserVO;
import xyz.kuailemao.mapper.RoleMapper;
import xyz.kuailemao.mapper.UserMapper;
import xyz.kuailemao.mapper.UserRoleMapper;
import xyz.kuailemao.service.UserRoleService;
import xyz.kuailemao.utils.SecurityUtils;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserRoleServiceimpl extends ServiceImpl<UserRoleMapper, UserRole>  implements UserRoleService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 添加用户——角色关系（给用户分配多个角色）
     * @param roleUserDTO
     * @return
     */
    @Override
    public ResponseResult<Void> addRoleUser(RoleUserDTO roleUserDTO) {
        List<Long> roleIds = roleUserDTO.getRoleId();
        List<UserRole> userRoleList = roleIds.stream().map(roleId->{
            UserRole userRole = new UserRole();
            userRole.setRoleId(roleId);
            userRole.setUserId(SecurityUtils.getUserId());
            return userRole;
        }).toList();
        if(userRoleService.saveBatch(userRoleList)){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


    /**
     * 添加用户角色关系(给角色分配多个用户）
     * @param userRoleDTO
     * @return
     */
    @Override
    public ResponseResult<Void> addUserRole(UserRoleDTO userRoleDTO) {
        List<Long> userIds = userRoleDTO.getUserId();
        List<UserRole> userRoleList = userIds.stream().map(userId->{
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(userRoleDTO.getRoleId());
            return userRole;
        }).toList();
        if(userRoleService.saveBatch(userRoleList)){
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


    /**
     * 删除角色用户关系（删除用户对应的多个角色）
     * @param roleUserDTO
     * @return
     */
    @Override
    public ResponseResult<Void> deleteRoleUser(RoleUserDTO roleUserDTO) {
        int isDelete = userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, roleUserDTO.getUserId()).in(UserRole::getRoleId, roleUserDTO.getRoleId()));
        if (isDelete > 0) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }


    /**
     * 删除用户角色关系
     * @param userRoleDTO
     * @return
     */
    @Override
    public ResponseResult<Void> deleteUserRole(UserRoleDTO userRoleDTO) {
        int isDelete = userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, userRoleDTO.getRoleId()).in(UserRole::getUserId, userRoleDTO.getUserId()));
        if (isDelete > 0) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Override
    public List<RoleAllVO> selectRoleByUserId(Long userId, String roleName, String roleKey, Integer type) {
        //type为0，查已拥有角色
        List<Long> roleIds = new ArrayList<>();
        if(type == 0){
            roleIds  = this.baseMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)).stream().map(UserRole::getRoleId).toList();
        }
        else if(type == 1){
            //先查所有的角色id
            List<Long> roleIdList = roleMapper.selectList(null).stream().map(Role::getId).collect(Collectors.toList());
            //查询已拥有的角色id
            List<Long> ownedRoleId = this.baseMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)).stream().map(UserRole::getRoleId).toList();
            //过滤，获取还没有的角色id
            roleIds = roleIdList.stream().filter(roleId->!ownedRoleId.contains(roleId)).collect(Collectors.toList());
        }
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<Role>();
        queryWrapper.like(Objects.nonNull(roleName),Role::getRoleName, roleName)
                .like(Objects.nonNull(roleKey),Role::getRoleKey, roleKey)
                        .in(Role::getId,roleIds);
        List<Role> roleList = roleMapper.selectList(queryWrapper);
        List<RoleAllVO> roleAllVOList =   roleList.stream().map(role->{
            RoleAllVO roleAllVO = BeanUtil.copyProperties(role,RoleAllVO.class);
            return roleAllVO;
        }).toList();
        return roleAllVOList;
    }


    /**
     * 角色用户详情页（查询）
     * @param roleId
     * @param username
     * @param email
     * @param type
     * @return
     */
    @Override
    public List<RoleUserVO> selectRoleUser(Long roleId, String username, String email, Integer type) {
        //查询已拥有的用户
        List<Long> userIdList = new ArrayList<>();
        if(type == 0){
            userIdList = this.baseMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)).stream().map(UserRole::getUserId).toList();
        }
        else if(type == 1){
            //查询所有的用户
            List<Long> idList = userMapper.selectList(null).stream().map(User::getId).collect(Collectors.toList());
            //查询已拥有的用户
            List<Long> ownedIdList = this.baseMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId)).stream().map(UserRole::getUserId).toList();
            //过滤，获取未拥有的用户id
            userIdList = idList.stream().filter(id -> !ownedIdList.contains(id)).collect(Collectors.toList());
        }
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        if(userIdList.size()>0){
            queryWrapper.in((User::getId),userIdList);
        }
        queryWrapper.like(Objects.nonNull(username),User::getUsername,username)
                .like(Objects.nonNull(email),User::getEmail,email);
        List<User> userList = userMapper.selectList(queryWrapper);
        List<RoleUserVO> returnList = userList.stream().map(user -> BeanUtil.copyProperties(user, RoleUserVO.class)).toList();
        return returnList;
    }


}
