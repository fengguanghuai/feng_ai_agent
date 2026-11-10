package com.qcdfz.fengaiagent;

import com.qcdfz.fengaiagent.utils.SseEmitterUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class SseEmitterUtilsTest {
    @RestController
    static class StreamController {
        @GetMapping("/test-stream")
        SseEmitter stream() throws Exception {
            var emitter = new SseEmitter();
            SseEmitterUtils.sendText(emitter, " hello\n  world\n\n- item");
            SseEmitterUtils.completeWithDone(emitter);
            return emitter;
        }
    }

    @Test
    void preservesSpacesAndBlankLinesOnTheWire() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new StreamController()).build();
        var started = mvc.perform(get("/test-stream")).andReturn();
        var response = mvc.perform(asyncDispatch(started)).andReturn().getResponse();
        assertEquals("data:  hello\ndata:   world\ndata: \ndata: - item\n\ndata:[DONE]\n\n",
                response.getContentAsString());
    }
}
