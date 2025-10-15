package xyz.kuailemao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import xyz.kuailemao.domain.entity.UserRole;


/**
 * (UserRole)表数据库访问层
 *
 * @author kuailemao
 * @since 2023-11-17 16:33:52
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

}
