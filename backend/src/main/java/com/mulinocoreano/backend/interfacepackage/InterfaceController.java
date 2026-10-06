package com.mulinocoreano.backend.interfacepackage;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 인터페이스 메커니즘 REST API — CLI와 대화 MCP 커넥터의 단일 진입점.
 * local 채널 인증과 scope를 적용하며 ERP 쓰기는 각 도메인의 인간 승인 게이트를 따른다.
 */
@RestController
@RequestMapping("/api/v1")
public class InterfaceController {

    private final InterfaceService service;
    private final DispatcherService dispatcher;
    private final CaseIntakeService intake;

    public InterfaceController(InterfaceService service, DispatcherService dispatcher, CaseIntakeService intake) {
        this.service = service;
        this.dispatcher = dispatcher;
        this.intake = intake;
    }

    // ------------------------------------------------------------ ASK
    @GetMapping("/ask")
    public AskResponse ask(@RequestParam(required = false) String q) {
        return service.ask(q);
    }

    // ------------------------------------------------------------ ACT
    @PostMapping("/cases")
    public CaseDto createCase(@Valid @RequestBody CreateCaseRequest req,
                              @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return intake.createCase(req, key);
    }

    @GetMapping("/cases")
    public List<CaseDto> listCases(@RequestParam(required = false) String status) {
        return service.listCases(status);
    }

    @GetMapping("/cases/{caseRef}")
    public CaseDto getCase(@PathVariable String caseRef) {
        return service.getCase(caseRef);
    }

    @GetMapping("/cases/{caseRef}/work-items")
    public List<WorkItemDto> listWorkItems(@PathVariable String caseRef) {
        return service.listWorkItems(caseRef);
    }

    // ------------------------------------------------------------ Execution (Run)
    @PostMapping("/runs")
    public RunDto createRun(@Valid @RequestBody CreateRunRequest req) {
        return service.createRun(req);
    }

    // ------------------------------------------------------------ Events / Dispatcher
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventDispatchResponse ingestEvent(@Valid @RequestBody CreateEventRequest req) {
        return dispatcher.ingest(req);
    }

    @PostMapping("/dispatch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventDispatchResponse dispatch() {
        return dispatcher.dispatchScheduled();
    }

    @GetMapping("/events")
    public List<EventDto> listEvents(@RequestParam(required = false) String caseRef) {
        return dispatcher.listEvents(caseRef);
    }

    // ------------------------------------------------------------ Attention
    @GetMapping("/attention")
    public List<AttentionDto> attention() {
        return service.listAttention();
    }

    // ------------------------------------------------------------ Monitor
    @GetMapping("/monitor")
    public MonitorDto monitor() {
        return service.monitor();
    }

    // ------------------------------------------------------------ health
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "layer", "interface");
    }
}
