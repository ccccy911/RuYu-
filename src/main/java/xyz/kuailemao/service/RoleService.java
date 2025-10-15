package xyz.kuailemao.service;


import com.baomidou.mybatisplus.extension.service.IService;
import xyz.kuailemao.domain.dto.RoleDTO;
import xyz.kuailemao.domain.dto.RoleSearchDTO;
import xyz.kuailemao.domain.entity.Role;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.RoleAllVO;
import xyz.kuailemao.domain.vo.RoleByIdVO;
import xyz.kuailemao.domain.vo.RoleVO;

import java.util.List;

public interface RoleService  extends IService<Role> {

    ResponseResult<Void> addRole(RoleDTO roleDTO);

    ResponseResult<Void> updateRole(RoleDTO roleDTO);

    ResponseResult<Void> updateRoleStatus(RoleDTO roleDTO);

    ResponseResult<List<RoleAllVO>> searchRole(RoleSearchDTO roleSearchDTO);

    ResponseResult getRoleList();

    ResponseResult<RoleByIdVO> getRole(Integer id);

    ResponseResult<List<RoleVO>> selectAll();
}
