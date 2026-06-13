package com.sherry.supervision.controller;

import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.dto.ChatRequest;
import com.sherry.supervision.dto.ChatResponse;
import com.sherry.supervision.service.ChatService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/run")
    public ApiResponse<ChatResponse> run(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(chatService.run(request));
    }

    @PostMapping("/resume")
    public ApiResponse<ChatResponse> resume(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(chatService.run(request));
    }

    @GetMapping("/state/{threadId}")
    public ApiResponse<Map<String, Object>> state(@PathVariable String threadId) {
        return ApiResponse.ok(Map.of("thread_id", threadId, "state", Map.of("thread_id", threadId)));
    }

    @GetMapping("/history/{threadId}")
    public ApiResponse<Map<String, Object>> history(@PathVariable String threadId) {
        return ApiResponse.ok(Map.of("thread_id", threadId, "messages", java.util.List.of(), "trace", java.util.List.of()));
    }
}
