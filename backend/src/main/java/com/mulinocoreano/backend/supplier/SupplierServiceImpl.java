package com.mulinocoreano.backend.supplier;

import com.mulinocoreano.backend.idempotency.RequestIdempotency;
import com.mulinocoreano.backend.persistence.PlanningDataGuard;
import com.mulinocoreano.backend.planning.CanonicalJson;
import com.mulinocoreano.backend.security.ErpActor;
import com.mulinocoreano.backend.security.HumanActor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@Service
public class SupplierServiceImpl implements SupplierService {
    private final SupplierRepository repository;
    private final EntityManager em;
    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;
    private final PlanningDataGuard guard;
    private final RequestIdempotency keys;
    private final ObjectMapper mapper;
    private final CanonicalJson json;

    public SupplierServiceImpl(SupplierRepository repository, EntityManager em, JdbcClient jdbc,
            PlatformTransactionManager manager, PlanningDataGuard guard, RequestIdempotency keys,
            ObjectMapper mapper, CanonicalJson json) {
        this.repository = repository;
        this.em = em;
        this.jdbc = jdbc;
        this.guard = guard;
        this.keys = keys;
        this.mapper = mapper;
        this.json = json;
        transactions = new TransactionTemplate(manager);
        // The guard serializes writes; READ_COMMITTED observes a receipt committed while waiting.
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactions.setTimeout(30);
    }

    private HumanActor require(ErpActor actor, boolean write) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(actor instanceof HumanActor human) || authentication == null
                || !authentication.isAuthenticated() || !human.equals(authentication.getPrincipal())
                || !human.capabilities().contains(write ? "work:write" : "erp:read")) {
            throw new AccessDeniedException("Authenticated Human required");
        }
        boolean allowed = jdbc.sql("""
                SELECT is_active AND role::text=:role
                    AND (:read OR role IN ('OPERATOR','MANAGER'))
                FROM users WHERE user_id=:id FOR SHARE
                """)
                .param("role", human.role()).param("read", !write).param("id", human.userId())
                .query(Boolean.class).optional().orElse(false);
        if (!allowed) throw new AccessDeniedException("Active matching Human role required");
        return human;
    }

    @Override
    public SupplierResponse get(long id, ErpActor actor) {
        return transactions.execute(status -> {
            require(actor, false);
            return find(id, false).response();
        });
    }

    @Override
    public List<SupplierResponse> list(int page, int size, ErpActor actor) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        return transactions.execute(status -> {
            require(actor, false);
            return repository.findAll(PageRequest.of(page, size, Sort.by("id")))
                    .stream().map(Supplier::response).toList();
        });
    }

    private Supplier find(long id, boolean lock) {
        var supplier = em.find(Supplier.class, id,
                lock ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE);
        if (supplier == null) throw new SupplierException(SupplierErrorCode.NOT_FOUND);
        return supplier;
    }

    private SupplierResponse mutate(String operation, Object request, ErpActor actor, String key,
            Function<HumanActor, SupplierResponse> action) {
        return transactions.execute(status -> {
            guard.lock();
            var human = require(actor, true);
            // Authority is checked even for historical exact replay. Version checks belong in action.
            return mapper.treeToValue(keys.executeCanonicalJson(
                    "suppliers." + operation + ":" + human.userId(), key, request,
                    () -> action.apply(human)), SupplierResponse.class);
        });
    }

    @Override
    public SupplierResponse create(CreateSupplierRequest input, ErpActor actor, String key) {
        return mutate("create", input, actor, key, human -> {
            var supplier = repository.saveAndFlush(new Supplier(input));
            var after = supplier.response();
            audit("CREATED", human, null, after);
            return after;
        });
    }

    @Override
    public SupplierResponse update(long id, UpdateSupplierRequest input, ErpActor actor, String key) {
        return mutate("update", Map.of("id", id, "input", input), actor, key, human -> {
            var supplier = find(id, true);
            checkVersion(supplier, input.expectedVersion());
            var before = supplier.response();
            supplier.replace(input.supplier());
            repository.flush();
            var after = supplier.response();
            audit("UPDATED", human, before, after);
            return after;
        });
    }

    @Override
    public SupplierResponse deactivate(long id, long version, ErpActor actor, String key) {
        if (version < 0) throw new IllegalArgumentException("Invalid version");
        return mutate("deactivate", Map.of("id", id, "expectedVersion", version), actor, key, human -> {
            var supplier = find(id, true);
            checkVersion(supplier, version);
            var before = supplier.response();
            supplier.deactivate();
            repository.flush();
            var after = supplier.response();
            audit("DEACTIVATED", human, before, after);
            return after;
        });
    }

    private void checkVersion(Supplier supplier, long version) {
        if (supplier.version() != version) throw new SupplierException(SupplierErrorCode.VERSION_CONFLICT);
    }

    private void audit(String event, HumanActor actor, SupplierResponse before, SupplierResponse after) {
        jdbc.sql("""
                INSERT INTO governance_audit_logs
                    (actor_id,event_type,resource_type,resource_id,before_state,after_state)
                VALUES (:actor,:event,'SUPPLIER',:id,CAST(:before AS jsonb),CAST(:after AS jsonb))
                """)
                .param("actor", actor.userId()).param("event", "SUPPLIER_" + event)
                .param("id", after.supplierId())
                .param("before", before == null ? null : json.write(Map.of("supplier", before, "actor", actor)))
                .param("after", json.write(Map.of("supplier", after, "actor", actor))).update();
    }
}
