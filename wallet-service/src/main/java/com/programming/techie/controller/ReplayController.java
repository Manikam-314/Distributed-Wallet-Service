package com.programming.techie.controller;

import com.programming.techie.service.ReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/replay")
@RequiredArgsConstructor
public class ReplayController {

    private final ReplayService replayService;

    @PostMapping("/{id}")
    public String replay(@PathVariable Long id) {
        replayService.replayFailedEvent(id);
        return "Replay triggered for event " + id;
    }
}
