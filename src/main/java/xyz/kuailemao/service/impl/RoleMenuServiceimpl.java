package xyz.kuailemao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.kuailemao.domain.entity.RoleMenu;
import xyz.kuailemao.domain.entity.UserRole;
import xyz.kuailemao.mapper.RoleMenuMapper;
import xyz.kuailemao.mapper.UserRoleMapper;
import xyz.kuailemao.service.RoleMenuService;
import xyz.kuailemao.service.UserRoleService;

@Service
public class RoleMenuServiceimpl  extends ServiceImpl<RoleMenuMapper, RoleMenu> implements RoleMenuService {

}
