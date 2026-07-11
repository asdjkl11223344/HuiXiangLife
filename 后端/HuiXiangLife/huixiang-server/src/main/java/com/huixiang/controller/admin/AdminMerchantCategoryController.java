package com.huixiang.controller.admin;

import java.util.List;

import com.huixiang.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.huixiang.dto.MerchantCategoryCreateDTO;
import com.huixiang.dto.MerchantCategoryUpdateDTO;
import com.huixiang.service.MerchantCategoryService;
import com.huixiang.vo.MerchantCategoryVO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/merchant-category")
@RequiredArgsConstructor
public class AdminMerchantCategoryController {

    private final MerchantCategoryService merchantCategoryService;

    @GetMapping("/list")
    public Result<List<MerchantCategoryVO>> list() {
        List<MerchantCategoryVO> list = merchantCategoryService.list();
        return Result.success(list);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody MerchantCategoryCreateDTO merchantCategoryCreateDTO) {
        Long id = merchantCategoryService.create(merchantCategoryCreateDTO);
        return Result.success(id);
    }

    @PutMapping
    public Result<Boolean> update(@Valid @RequestBody MerchantCategoryUpdateDTO merchantCategoryUpdateDTO) {
        Boolean result = merchantCategoryService.update(merchantCategoryUpdateDTO);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        Boolean result = merchantCategoryService.delete(id);
        return Result.success(result);
    }
}