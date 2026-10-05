package com.mulinocoreano.backend.supplier;
import com.mulinocoreano.backend.security.ErpActor;
import java.util.List;
public interface SupplierService {
    SupplierResponse create(CreateSupplierRequest request,ErpActor actor,String key);
    SupplierResponse get(long id,ErpActor actor);
    List<SupplierResponse> list(int page,int size,ErpActor actor);
    SupplierResponse update(long id,UpdateSupplierRequest request,ErpActor actor,String key);
    SupplierResponse deactivate(long id,long expectedVersion,ErpActor actor,String key);
}
