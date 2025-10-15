package xyz.kuailemao.controller;


import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import xyz.kuailemao.annotation.AccessLimit;
import xyz.kuailemao.domain.dto.LeaveWordIsCheckDTO;
import xyz.kuailemao.domain.dto.SearchLeaveWordDTO;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.LeaveWordListVO;
import xyz.kuailemao.domain.vo.LeaveWordVO;
import xyz.kuailemao.service.LeaveWordService;
import xyz.kuailemao.utils.ControllerUtils;

import java.util.List;

@RestController
@RequestMapping("leaveWord")
public class LeaveWordController {

    @Resource
    private LeaveWordService leaveWordService;


    /**
     * 用户留言
     * @param content
     * @return
     */
    @PostMapping("/auth/userLeaveWord")
    public ResponseResult<Void> userLeaveWord(@RequestBody @NotNull String content) {
        return leaveWordService.userLeaveWord(content);
    }


    /**
     * 搜索后台留言列表
     * @param searchDTO
     * @return
     */
    @PostMapping("/back/search")
    public ResponseResult<List<LeaveWordListVO>> backList(@RequestBody SearchLeaveWordDTO searchDTO) {
        return  leaveWordService.getBackLeaveWordList(searchDTO);
    }

    /**
     * 前端获取留言板列表
     * @param id
     * @return
     */
    @GetMapping("/list")
    public ResponseResult<List<LeaveWordVO>> list(@RequestParam(value = "id",required = false) String id) {
        return ControllerUtils.messageHandler(() -> leaveWordService.getLeaveWordList(id));
    }

    /**
     * 后台留言列表
     * @return
     */
    @GetMapping("/back/list")
    public ResponseResult<List<LeaveWordListVO>> backList() {
        return leaveWordService.getBackLeaveWordList(null);
    }

    /**
     * 删除留言
     * @param ids
     * @return
     */
    @DeleteMapping("/back/delete")
    public ResponseResult<Void> delete(@RequestBody List<Long> ids) {
        return leaveWordService.deleteLeaveWord(ids);
    }

    /**
     * 修改留言是否通过
     * @param leaveWordIsCheckDTO
     * @return
     */
    @PostMapping("/back/isCheck")
    public ResponseResult<Void> isCheck(@RequestBody @Valid LeaveWordIsCheckDTO leaveWordIsCheckDTO) {
        return leaveWordService.isCheckLeaveWord(leaveWordIsCheckDTO);
    }
}


}
