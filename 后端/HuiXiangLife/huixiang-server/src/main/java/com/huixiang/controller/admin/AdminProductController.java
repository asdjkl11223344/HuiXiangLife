package com.huixiang.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.ProductCreateDTO;
import com.huixiang.dto.ProductUpdateDTO;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.query.ProductQuery;
import com.huixiang.result.Result;
import com.huixiang.service.ProductService;
import com.huixiang.vo.ProductDetailVO;
import com.huixiang.vo.SeckillAdminStatusVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/page")
    public Result<Page<ProductDetailVO>> page(ProductQuery productQuery) {
        Page<ProductDetailVO> pageResult = productService.adminPage(productQuery);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        ProductDetailVO productDetailVO = productService.adminDetail(id);
        return Result.success(productDetailVO);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductCreateDTO productCreateDTO) {
        Long id = productService.create(productCreateDTO);
        return Result.success(id);
    }

    @PutMapping
    public Result<Boolean> update(@Valid @RequestBody ProductUpdateDTO productUpdateDTO) {
        Boolean result = productService.update(productUpdateDTO);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody StatusUpdateDTO statusUpdateDTO) {
        Boolean result = productService.updateStatus(id, statusUpdateDTO);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        Boolean result = productService.delete(id);
        return Result.success(result);
    }

    @PostMapping("/{id}/seckill/preheat")
    public Result<Boolean> preheatSeckillStock(@PathVariable Long id) {
        return Result.success(productService.preheatSeckillStock(id));
    }

    @PostMapping("/seckill/preheat/batch")
    public Result<Integer> batchPreheatSeckillStock(@RequestParam List<Long> ids) {
        return Result.success(productService.batchPreheatSeckillStock(ids));
    }

    @PostMapping("/{id}/seckill/reset")
    public Result<Boolean> resetSeckillStock(@PathVariable Long id) {
        return Result.success(productService.resetSeckillStock(id));
    }

    @PostMapping("/seckill/reset/batch")
    public Result<Integer> batchResetSeckillStock(@RequestParam List<Long> ids) {
        return Result.success(productService.batchResetSeckillStock(ids));
    }

    @GetMapping("/{id}/seckill/status")
    public Result<SeckillAdminStatusVO> getSeckillAdminStatus(@PathVariable Long id,
                                                              @RequestParam(required = false) Long userId) {
        return Result.success(productService.getSeckillAdminStatus(id, userId));
    }

    @PostMapping("/seckill/preheat/trigger")
    public Result<Integer> triggerUpcomingSeckillPreheat(@RequestParam(required = false) Integer advanceMinutes) {
        return Result.success(productService.triggerUpcomingSeckillPreheat(advanceMinutes));
    }

    @PostMapping("/{id}/search/sync")
    public Result<Boolean> syncSearchIndex(@PathVariable Long id) {
        return Result.success(productService.syncSearchIndex(id));
    }

    @PostMapping("/search/rebuild")
    public Result<Integer> rebuildSearchIndex() {
        return Result.success(productService.rebuildSearchIndex());
    }
}
