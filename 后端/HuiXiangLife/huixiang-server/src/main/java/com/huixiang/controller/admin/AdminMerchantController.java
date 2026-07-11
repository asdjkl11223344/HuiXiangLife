package com.huixiang.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.MerchantCreateDTO;
import com.huixiang.dto.MerchantUpdateDTO;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.query.MerchantQuery;
import com.huixiang.result.Result;
import com.huixiang.service.MerchantService;
import com.huixiang.vo.MerchantDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/merchant")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantService merchantService;

    @GetMapping("/page")
    public Result<Page<MerchantDetailVO>> page(MerchantQuery merchantQuery) {
        Page<MerchantDetailVO> pageResult = merchantService.adminPage(merchantQuery);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<MerchantDetailVO> detail(@PathVariable Long id) {
        MerchantDetailVO merchantDetailVO = merchantService.adminDetail(id);
        return Result.success(merchantDetailVO);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody MerchantCreateDTO merchantCreateDTO) {
        Long id = merchantService.create(merchantCreateDTO);
        return Result.success(id);
    }

    @PutMapping
    public Result<Boolean> update(@Valid @RequestBody MerchantUpdateDTO merchantUpdateDTO) {
        Boolean result = merchantService.update(merchantUpdateDTO);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody StatusUpdateDTO statusUpdateDTO) {
        Boolean result = merchantService.updateStatus(id, statusUpdateDTO);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(merchantService.delete(id));
    }

    @PostMapping("/{id}/search/sync")
    public Result<Boolean> syncSearchIndex(@PathVariable Long id) {
        return Result.success(merchantService.syncSearchIndex(id));
    }

    @PostMapping("/search/rebuild")
    public Result<Integer> rebuildSearchIndex() {
        return Result.success(merchantService.rebuildSearchIndex());
    }
}
