package edu.nju.jap.integration;

import edu.nju.jap.service.OnlineStatusScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnnotationFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OnlineStatusScheduler onlineStatusScheduler;

    @Test
    void annotatorCanCompleteHappyPath() throws Exception {
        String token = login("annotator1", "123456");

        mockMvc.perform(get("/api/tasks/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.total", is(1)));

        mockMvc.perform(get("/api/tasks/1001/items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dataId", is(101)));

        mockMvc.perform(get("/api/tasks/1001/items/101").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.document.title", notNullValue()));

        String payload = """
                {
                  "taskId": 1001,
                  "dataId": 101,
                  "isDraft": false,
                  "propositions": [
                    {"propId":"P1","sequenceNo":1,"startPos":0,"endPos":8,"text":"依法成立的合同","tag":"GM-L"},
                    {"propId":"P2","sequenceNo":2,"startPos":16,"endPos":24,"text":"被告未按期付款","tag":"SF"}
                  ],
                  "relations": [
                    {"relId":"R1","type":"S","source":"P1","target":"P2","members":["P1","P2"]}
                  ]
                }
                """;
        mockMvc.perform(post("/api/annotations/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        mockMvc.perform(get("/api/tasks/1001/items/101").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.annotation.propositions[0].propId", is("P1")))
                .andExpect(jsonPath("$.data.annotation.relations[0].relId", is("R1")))
                .andExpect(jsonPath("$.data.annotation.draft", is(false)));
    }

    @Test
    void anonymousUserCannotAccessMyTasks() throws Exception {
        mockMvc.perform(get("/api/tasks/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(401)));
    }

    @Test
    void missingGuideVersionReturnsNotFoundToastMessage() throws Exception {
        String token = login("admin", "123456");

        mockMvc.perform(get("/api/configs/versions/999").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", is("指南版本不存在")));
    }

    private String login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
        return mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll("(?s).*?\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
