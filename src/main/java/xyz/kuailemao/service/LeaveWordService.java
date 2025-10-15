package xyz.kuailemao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import xyz.kuailemao.domain.dto.LeaveWordIsCheckDTO;
import xyz.kuailemao.domain.dto.SearchLeaveWordDTO;
import xyz.kuailemao.domain.entity.LeaveWord;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.LeaveWordListVO;
import xyz.kuailemao.domain.vo.LeaveWordVO;

import java.util.List;

public interface LeaveWordService  extends IService<LeaveWord> {

    ResponseResult<Void> userLeaveWord(@NotNull String content);

    ResponseResult<List<LeaveWordListVO>> getBackLeaveWordList(SearchLeaveWordDTO searchDTO);

    List<LeaveWordVO> getLeaveWordList(String id);

    ResponseResult<Void> deleteLeaveWord(List<Long> ids);

    ResponseResult<Void> isCheckLeaveWord(@Valid LeaveWordIsCheckDTO leaveWordIsCheckDTO);
}
