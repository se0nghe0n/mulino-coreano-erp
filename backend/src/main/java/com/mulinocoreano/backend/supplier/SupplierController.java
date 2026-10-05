package com.mulinocoreano.backend.supplier;

import com.mulinocoreano.backend.common.error.ApiError;
import com.mulinocoreano.backend.common.error.CommonErrorCode;
import com.mulinocoreano.backend.common.response.ApiResult;
import com.mulinocoreano.backend.docs.ApiCommonErrorResponses;
import com.mulinocoreano.backend.security.ErpActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Supplier", description = "인간이 관리하는 공급업체 master")
public class SupplierController {
    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @ExceptionHandler({ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> invalidArgument(Exception error) {
        return ResponseEntity.badRequest().body(ApiError.of(CommonErrorCode.INVALID_INPUT_VALUE));
    }

    @PostMapping
    @ApiCommonErrorResponses
    @Operation(summary = "공급업체 생성")
    public ResponseEntity<ApiResult<SupplierResponse>> create(
            @Valid @RequestBody CreateSupplierRequest input,
            @AuthenticationPrincipal ErpActor actor, @RequestHeader("Idempotency-Key") String key) {
        return ApiResult.created(service.create(input, actor, key));
    }

    @GetMapping("/{id}")
    @ApiCommonErrorResponses
    @Operation(summary = "비활성 이력을 포함한 공급업체 조회")
    public ResponseEntity<ApiResult<SupplierResponse>> get(
            @PathVariable long id, @AuthenticationPrincipal ErpActor actor) {
        return ApiResult.ok(service.get(id, actor));
    }

    @GetMapping
    @ApiCommonErrorResponses
    @Operation(summary = "공급업체 목록 조회")
    public ResponseEntity<ApiResult<List<SupplierResponse>>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal ErpActor actor) {
        return ApiResult.ok(service.list(page, size, actor));
    }

    @PutMapping("/{id}")
    @ApiCommonErrorResponses
    @Operation(summary = "공급업체 수정")
    public ResponseEntity<ApiResult<SupplierResponse>> update(
            @PathVariable long id, @Valid @RequestBody UpdateSupplierRequest input,
            @AuthenticationPrincipal ErpActor actor, @RequestHeader("Idempotency-Key") String key) {
        return ApiResult.ok(service.update(id, input, actor, key));
    }

    @DeleteMapping("/{id}")
    @ApiCommonErrorResponses
    @Operation(summary = "공급업체 비활성화")
    public ResponseEntity<ApiResult<SupplierResponse>> deactivate(
            @PathVariable long id, @RequestParam long expectedVersion,
            @AuthenticationPrincipal ErpActor actor, @RequestHeader("Idempotency-Key") String key) {
        return ApiResult.ok(service.deactivate(id, expectedVersion, actor, key));
    }
}
